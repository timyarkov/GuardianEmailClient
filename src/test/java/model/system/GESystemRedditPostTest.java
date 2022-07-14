package model.system;

import model.comms.exceptions.GECommsException;
import model.comms.manager.GECommsManager;
import model.comms.payloads.RPostPayload;
import model.comms.payloads.RTokenPayload;
import model.db.GEDatabase;
import model.env.Environment;
import model.items.GContent;
import model.items.GTag;
import model.items.RedditToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests Reddit posting abilities of GESystem.
 */
public class GESystemRedditPostTest {
    private GESystem fixture;

    // Mocks + Dummy objects
    private Environment mockEnv;
    private GECommsManager mockComms;
    private GEDatabase mockDB;
    private GESystemObserver mockObserver;

    private GTag dummyTag;
    private GContent dummyContent;
    private RedditToken dummyToken;

    // NOTE: For testing system error state, expected behaviour is that error state
    //       is set, broadcast is sent to observers, and then the error state is
    //       unset. Thus, checking the error message has changed and that observers
    //       were notified is used as a proxy for checking an error state was raised.

    // Setup
    @BeforeEach
    public void setup() throws GECommsException {
        // Dummy objects setup
        dummyTag = new GTag("places/snow", "keyword", "Snowy Places",
                "https://en.wikipedia.org/wiki/Penguin",
                "https://en.wikipedia.org/wiki/Penguin");
        dummyContent = new GContent("pingu",
                "people/pingu",
                "Pingu",
                "sometime this year",
                "The Great Pingu",
                "https://en.wikipedia.org/wiki/Penguin",
                "https://en.wikipedia.org/wiki/Penguin",
                1,
                10);

        // As long as tests don't take 24h to run this won't break the tests
        dummyToken = new RedditToken("cake recipes", 86400);

        // Mock Setup
        mockEnv = mock(Environment.class);
        when(mockEnv.getenv("REDDIT_API_CLIENT"))
                .thenReturn("seal's reddit account");
        when(mockEnv.getenv("REDDIT_API_SECRET"))
                .thenReturn("seal's karma stash");

        mockObserver = mock(GESystemObserver.class);

        mockComms = mock(GECommsManager.class);
        when(mockComms.getRedditToken(any(RTokenPayload.class))).thenReturn(dummyToken);
        when(mockComms.postReddit(any(RPostPayload.class))).thenReturn(true);

        mockDB = mock(GEDatabase.class);

        // Fixture setup
        fixture = new GESystemImpl(false, false, false, mockDB);
        fixture.injectNewCommsManager(mockComms);
        fixture.injectNewEnvironment(mockEnv);
        fixture.authenticateReddit("walrus the", "karma farmer");
    }

    // Tests
    /**
     * Tests usual posting.
     * @throws GECommsException Ignore this.
     */
    @Test
    public void postRedditTest() throws GECommsException {
        assertTrue(fixture.postReddit(dummyTag, List.of(dummyContent)));

        // Assert comms used
        verify(mockComms).postReddit(any(RPostPayload.class));
    }

    /**
     * Tests posting when there is an error of some sort.
     * @throws GECommsException Ignore this.
     */
    @Test
    public void postRedditFailTest() throws GECommsException {
        // Set up mock to fail, set up observer
        when(mockComms.postReddit(any(RPostPayload.class)))
                .thenThrow(GECommsException.class);
        fixture.addObserver(mockObserver);

        // Expected behaviour of false return and error event
        String preMsg = fixture.getErrorMessage();
        assertFalse(fixture.postReddit(dummyTag, List.of(dummyContent)));
        assertNotEquals(preMsg, fixture.getErrorMessage());
        verify(mockObserver).update();
    }
}
