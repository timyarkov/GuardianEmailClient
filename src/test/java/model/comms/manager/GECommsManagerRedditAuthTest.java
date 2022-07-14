package model.comms.manager;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import model.comms.drivers.GEComms;
import model.comms.drivers.GEResponse;
import model.comms.exceptions.GECommsException;
import model.comms.payloads.RTokenPayload;
import model.comms.util.JSONParser;
import model.env.Environment;
import model.items.RedditToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests token getting from Reddit via the comms manager.
 */
public class GECommsManagerRedditAuthTest {
    private GECommsManager fixture;

    // Mocks + Dummies
    private GEComms mockOnlineComms;
    private GEComms mockOfflineComms;
    private GEResponse mockTokenResponse;
    private JSONParser mockParser;
    private Environment mockEnv;

    private JsonObject dummyJson;
    private RTokenPayload dummyTokenPayload;

    // Setup
    @BeforeEach
    public void setup() throws GECommsException {
        // Dummies setup
        dummyJson = new JsonObject();
        dummyJson.add("access_token", new JsonPrimitive("pingucoin"));
        dummyJson.add("expires_in", new JsonPrimitive(123));

        dummyTokenPayload = new RTokenPayload("secret", "waffle recipes");

        // Mock setup
        mockOnlineComms = mock(GEComms.class);
        mockOfflineComms = mock(GEComms.class);

        mockTokenResponse = mock(GEResponse.class);
        when(mockTokenResponse.body()).thenReturn("success!");
        when(mockTokenResponse.statusCode()).thenReturn(200);

        when(mockOnlineComms.getRedditToken(any(RTokenPayload.class)))
                .thenReturn(mockTokenResponse);
        when(mockOfflineComms.getRedditToken(any(RTokenPayload.class)))
                .thenReturn(mockTokenResponse);

        mockParser = mock(JSONParser.class);
        when(mockParser.parseResponse(anyString())).thenReturn(dummyJson);

        mockEnv = mock(Environment.class);
        when(mockEnv.getenv("REDDIT_API_CLIENT")).thenReturn("seal's computer");
        when(mockEnv.getenv("REDDIT_API_SECRET")).thenReturn("seal's cookie recipes");

        // Fixture setup
        fixture = new GECommsManagerImpl(false, false, false);
        fixture.injectNewDrivers(mockOnlineComms, mockOfflineComms);
        fixture.injectNewParser(mockParser);
        fixture.injectNewEnvironment(mockEnv);
    }

    // Tests
    /**
     * Tests usual reddit authentication.
     * @throws GECommsException Ignore this.
     */
    @Test
    public void authRedditTest() throws GECommsException {
        // Setup
        RedditToken expected = new RedditToken("pingucoin", 123);

        // Setup "online", make request
        fixture.setOnline(false, false, true);
        assertThat(fixture.getRedditToken(dummyTokenPayload), equalTo(expected));

        // Ensure correct comms used
        verify(mockOnlineComms).getRedditToken(dummyTokenPayload);

        // Setup "offline", make request
        fixture.setOnline(false, false, false);
        assertThat(fixture.getRedditToken(dummyTokenPayload), equalTo(expected));

        // Ensure correct comms used
        verify(mockOfflineComms).getRedditToken(dummyTokenPayload);
    }

    /**
     * Tests reddit authentication with bad credentials.
     */
    @Test
    public void authRedditBadCredsTest() {
        // Setup
        JsonObject err = new JsonObject();
        err.add("error", new JsonPrimitive("invalid_grant"));
        when(mockParser.parseResponse(mockTokenResponse.body())).thenReturn(err);

        // Ensure fail
        assertThrows(GECommsException.class, () -> fixture.getRedditToken(dummyTokenPayload));
    }

    /**
     * Tests the authentication when there is an HTTP error during the request.
     */
    @Test
    public void authRedditErrorResponseTest() {
        // Test auth response fail
        for (int i = 400; i < 600; i++) {
            when(mockTokenResponse.statusCode()).thenReturn(i);
            assertThrows(GECommsException.class, () -> fixture.getRedditToken(dummyTokenPayload));
        }
    }

    /**
     * Tests trying to authenticate with no API client key environment variable.
     */
    @Test
    public void authRedditNoClientEnvVarTest() {
        // Setup
        fixture.setOnline(false, false, true);
        when(mockEnv.getenv("REDDIT_API_CLIENT")).thenReturn(null);

        // Ensure when online that throws
        assertThrows(GECommsException.class, () -> fixture.getRedditToken(dummyTokenPayload));

        // Ensure that when offline doesn't throw
        fixture.setOnline(false, false, false);
        assertDoesNotThrow(() -> fixture.getRedditToken(dummyTokenPayload));
    }

    /**
     * Tests trying to authenticate with no API secret key environment variable.
     */
    @Test
    public void authRedditNoSecretEnvVarTest() {
        // Setup
        fixture.setOnline(false, false, true);
        when(mockEnv.getenv("REDDIT_API_SECRET")).thenReturn(null);

        // Ensure when online that throws
        assertThrows(GECommsException.class, () -> fixture.getRedditToken(dummyTokenPayload));

        // Ensure that when offline doesn't throw
        fixture.setOnline(false, false, false);
        assertDoesNotThrow(() -> fixture.getRedditToken(dummyTokenPayload));
    }
}
