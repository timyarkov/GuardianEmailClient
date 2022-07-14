package model.comms.manager;

import model.comms.drivers.GEComms;
import model.comms.drivers.GEResponse;
import model.comms.exceptions.GECommsException;
import model.comms.payloads.RPostPayload;
import model.env.Environment;
import model.items.GContent;
import model.items.GTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests reddit posting functionality of a comms manager.
 */
public class GECommsManagerRedditPostingTest {
    private GECommsManager fixture;

    // Mocks + Dummies
    private GEComms mockOnlineComms;
    private GEComms mockOfflineComms;
    private GEResponse mockPostResponse;
    private Environment mockEnv;

    private RPostPayload dummyPostPayload;
    private GTag dummyTag;
    private GContent dummyContent;

    // Setup
    @BeforeEach
    public void setup() throws GECommsException {
        // Dummies setup
        dummyTag = new GTag("fishing_spots", "advice", "Cool Fishing Spots", "url", "url");
        dummyContent = new GContent("beach",
                "place",
                "places",
                "2022",
                "The Beach",
                "url",
                "url",
                1,
                10);
        dummyPostPayload = new RPostPayload("pingu", "car keys", dummyTag, List.of(dummyContent));

        // Mock setup
        mockOnlineComms = mock(GEComms.class);
        mockOfflineComms = mock(GEComms.class);

        mockPostResponse = mock(GEResponse.class);
        when(mockPostResponse.body()).thenReturn("success!");
        when(mockPostResponse.statusCode()).thenReturn(200);

        when(mockOnlineComms.postReddit(any(RPostPayload.class))).thenReturn(mockPostResponse);
        when(mockOfflineComms.postReddit(any(RPostPayload.class))).thenReturn(mockPostResponse);

        mockEnv = mock(Environment.class);
        when(mockEnv.getenv("REDDIT_API_CLIENT")).thenReturn("seal's computer");
        when(mockEnv.getenv("REDDIT_API_SECRET")).thenReturn("seal's cookie recipes");

        // Fixture setup
        fixture = new GECommsManagerImpl(false, false, false);
        fixture.injectNewDrivers(mockOnlineComms, mockOfflineComms);
        fixture.injectNewEnvironment(mockEnv);
    }

    // Tests
    /**
     * Tests usual posting to reddit.
     * @throws GECommsException Ignore this.
     */
    @Test
    public void postRedditTest() throws GECommsException {
        // Setup "online", make request
        fixture.setOnline(false, false, true);
        assertTrue(fixture.postReddit(dummyPostPayload));

        // Ensure correct comms used
        verify(mockOnlineComms).postReddit(dummyPostPayload);

        // Check "offline", make request
        fixture.setOnline(false, false, false);
        assertTrue(fixture.postReddit(dummyPostPayload));

        // Ensure correct comms used
        verify(mockOfflineComms).postReddit(dummyPostPayload);
    }

    /**
     * Tests the posting response when there is an HTTP error during the request.
     */
    @Test
    public void postRedditErrorResponseTest() {
        // Test post response fail
        for (int i = 400; i < 600; i++) {
            when(mockPostResponse.statusCode()).thenReturn(i);
            assertThrows(GECommsException.class,
                    () -> fixture.postReddit(dummyPostPayload));
        }
    }

    /**
     * Tests posting response when there is no token provided (null)
     */
    @Test
    public void postRedditNoTokenTest() {
        // Setup
        RPostPayload noTokPayload = new RPostPayload("baby pingi",
                                                     null,
                                                     dummyTag,
                                                     List.of(dummyContent));

        // Ensure throws
        assertThrows(GECommsException.class, () -> fixture.postReddit(noTokPayload));
    }

    /**
     * Tests trying to post with no API client key environment variable.
     */
    @Test
    public void postRedditNoClientEnvVarTest() {
        // Setup
        fixture.setOnline(false, false, true);
        when(mockEnv.getenv("REDDIT_API_CLIENT")).thenReturn(null);

        // Ensure when online that throws
        assertThrows(GECommsException.class, () -> fixture.postReddit(dummyPostPayload));

        // Ensure that when offline doesn't throw
        fixture.setOnline(false, false, false);
        assertDoesNotThrow(() -> fixture.postReddit(dummyPostPayload));
    }

    /**
     * Tests trying to post with no API secret key environment variable.
     */
    @Test
    public void postRedditNoSecretEnvVarTest() {
        // Setup
        fixture.setOnline(false, false, true);
        when(mockEnv.getenv("REDDIT_API_SECRET")).thenReturn(null);

        // Ensure when online that throws
        assertThrows(GECommsException.class, () -> fixture.postReddit(dummyPostPayload));

        // Ensure that when offline doesn't throw
        fixture.setOnline(false, false, false);
        assertDoesNotThrow(() -> fixture.postReddit(dummyPostPayload));
    }
}
