package model.system;

import model.comms.exceptions.GECommsException;
import model.comms.manager.GECommsManager;
import model.comms.payloads.RTokenPayload;
import model.db.GEDatabase;
import model.env.Environment;
import model.items.GContent;
import model.items.GTag;
import model.items.RedditToken;
import model.util.SleepModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests Reddit authorisation abilities of GESystem.
 */
public class GESystemRedditAuthTest {
    private GESystem fixture;

    // Mocks + Dummy objects
    private Environment mockEnv;
    private GECommsManager mockComms;
    private GEDatabase mockDB;
    private GESystemObserver mockObserver;
    private SleepModule mockSleeper;

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
        dummyToken = new RedditToken("cookies", 86400);

        // Mock Setup
        mockEnv = mock(Environment.class);
        when(mockEnv.getenv("REDDIT_API_CLIENT"))
                .thenReturn("seal's reddit account");
        when(mockEnv.getenv("REDDIT_API_SECRET"))
                .thenReturn("seal's karma stash");

        mockObserver = mock(GESystemObserver.class);

        mockComms = mock(GECommsManager.class);
        when(mockComms.getRedditToken(any(RTokenPayload.class))).thenReturn(dummyToken);

        mockDB = mock(GEDatabase.class);

        mockSleeper = mock(SleepModule.class);

        // Fixture setup
        fixture = new GESystemImpl(false, false, false, mockDB);
        fixture.injectNewCommsManager(mockComms);
        fixture.injectNewEnvironment(mockEnv);
    }

    // Tests
    /**
     * Tests usual reddit authentication.
     */
    @Test
    public void authRedditTest() {
        // Check Preconditions
        assertNull(fixture.getRuntimeData().get(GERuntimeData.REDDIT_USERNAME.key));
        assertNull(fixture.getRuntimeData().get(GERuntimeData.REDDIT_TOKEN.key));

        // Authenticate
        assertTrue(fixture.authenticateReddit("where are my", "it will say cookies"));

        // Check Postconditions
        assertThat(fixture.getRuntimeData()
                          .get(GERuntimeData.REDDIT_USERNAME.key), equalTo("where are my"));
        assertThat(fixture.getRuntimeData()
                          .get(GERuntimeData.REDDIT_TOKEN.key), equalTo("cookies"));
    }

    /**
     * Tests that token invalidation works properly; broadcasting to observers
     * and setting the token to null.
     * @throws GECommsException Ignore this.
     */
    @Test
    public void authRedditInvalidationTest() throws GECommsException {
        // Setup
        fixture.injectNewSleepModule(mockSleeper);
        RedditToken shortTok = new RedditToken("elusive cookie", 123);
        when(mockComms.getRedditToken(any(RTokenPayload.class))).thenReturn(shortTok);
        fixture.addObserver(mockObserver);

        // Authenticate
        assertTrue(fixture.authenticateReddit("seal", "treat baker"));

        // Check preconditions
        assertThat(fixture.getRuntimeData()
                          .get(GERuntimeData.REDDIT_USERNAME.key), equalTo("seal"));
        assertThat(fixture.getRuntimeData()
                          .get(GERuntimeData.REDDIT_TOKEN.key), equalTo("elusive cookie"));

        // Check wait done
        // Verify with timeout to allow possible delays in execution of thread
        verify(mockSleeper, timeout(100)).sleep(123 * 1000);

        // Check observer notified and post-conditions
        verify(mockObserver).update();
        assertNull(fixture.getRuntimeData().get(GERuntimeData.REDDIT_TOKEN.key));
    }

    /**
     * Tests system response when reddit authentication fails.
     * @throws GECommsException Ignore this.
     */
    @Test
    public void authRedditFailTest() throws GECommsException {
        // Set up mock to fail, set up observer
        when(mockComms.getRedditToken(any(RTokenPayload.class)))
                .thenThrow(GECommsException.class);
        fixture.addObserver(mockObserver);

        // Expected behaviour of false return and error event
        String preMsg = fixture.getErrorMessage();
        assertFalse(fixture.authenticateReddit("sneaky", "fish"));
        assertNotEquals(preMsg, fixture.getErrorMessage());
        verify(mockObserver).update();
    }
}
