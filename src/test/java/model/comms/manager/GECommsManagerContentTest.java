package model.comms.manager;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import model.comms.drivers.GEComms;
import model.comms.drivers.GEResponse;
import model.comms.exceptions.GECommsException;
import model.comms.payloads.GContentPayload;
import model.comms.util.JSONParser;
import model.db.GEDatabase;
import model.env.Environment;
import model.items.GTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests content functionalities of comms manager.
 * Includes testing usage of cache during comms calls.
 */
public class GECommsManagerContentTest {
    private GECommsManager fixture;

    // Mocks + Dummies
    private GEComms mockOnlineComms;
    private GEComms mockOfflineComms;
    private GEResponse mockResponse;
    private JSONParser mockParser;
    private GEDatabase mockDB;
    private Environment mockEnv;

    private JsonObject dummyJson;
    private GContentPayload dummyPayload;

    // Setup
    @BeforeEach
    public void setup() throws GECommsException {
        // Dummies setup
        dummyJson = new JsonObject();

        GTag dummyTag = new GTag("fishing_spots", "advice", "Cool Fishing Spots", "url", "url");
        dummyPayload = new GContentPayload(dummyTag, "hello!", 1, 10);

        // Mock setup
        mockOnlineComms = mock(GEComms.class);
        mockOfflineComms = mock(GEComms.class);
        mockResponse = mock(GEResponse.class);
        when(mockResponse.body()).thenReturn("{\"mail\": \"cold snow\"}");
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockOnlineComms.getContent(any(GContentPayload.class))).thenReturn(mockResponse);
        when(mockOfflineComms.getContent(any(GContentPayload.class))).thenReturn(mockResponse);

        mockParser = mock(JSONParser.class);
        when(mockParser.parseResponse(anyString())).thenReturn(dummyJson);

        mockDB = mock(GEDatabase.class);
        when(mockDB.getCachedContent(eq(dummyPayload.tag()), anyString(), anyInt()))
                .thenReturn("{\"cached\":\"content\"}");

        mockEnv = mock(Environment.class);
        when(mockEnv.getenv("INPUT_API_KEY")).thenReturn("antarctica press pass");

        // Fixture setup
        fixture = new GECommsManagerImpl(false, false, false);
        fixture.injectNewDrivers(mockOnlineComms, mockOfflineComms);
        fixture.injectNewParser(mockParser);
        fixture.injectDatabase(mockDB);
        fixture.injectNewEnvironment(mockEnv);
    }

    // Tests

    /**
     * Tests getting content without use of the cache.
     * @throws GECommsException Ignore this.
     */
    @Test
    public void getContentNoCacheTest() throws GECommsException {
        // Set "online" first
        fixture.setOnline(true, false, false);

        // Assert correct object gained (result of parse)
        assertThat(fixture.getContent(dummyPayload, false), equalTo(dummyJson));

        // Verify parser used
        verify(mockParser).parseResponse(anyString());
        // Verify correct comms used
        verify(mockOnlineComms).getContent(any(GContentPayload.class));

        // Setup "offline"
        fixture.setOnline(false, false, false);

        assertThat(fixture.getContent(dummyPayload, false), equalTo(dummyJson));

        // Verify correct comms used
        verify(mockOfflineComms).getContent(any(GContentPayload.class));
    }

    /**
     * Tests getting content with the use of the cache.
     * @throws GECommsException Ignore this.
     */
    @Test
    public void getContentCacheUseTest() throws GECommsException {
        // Setup "online" mode
        fixture.setOnline(true, false, false);

        // Call for content with cache in use
        fixture.getContent(dummyPayload, true);

        // Ensure cache was queried and used, and no API call was made
        verify(mockDB).getCachedContent(dummyPayload.tag(),
                                        dummyPayload.query(),
                                        dummyPayload.page());
        verify(mockOnlineComms, never()).getContent(any(GContentPayload.class));
    }

    /**
     * Tests that getting content offline doesn't hit the cache.
     * @throws GECommsException Ignore this.
     */
    @Test
    public void getContentOfflineCacheUseTest() throws GECommsException {
        // Setup "offline" mode
        fixture.setOnline(false, false, false);

        // Try use cache with offline mode
        fixture.getContent(dummyPayload, true);

        // Ensure that offline mode skips DB checks and goes for API call
        verify(mockDB, never()).getCachedContent(dummyPayload.tag(),
                                                 dummyPayload.query(),
                                                 dummyPayload.page());
        verify(mockOfflineComms).getContent(any(GContentPayload.class));
    }

    /**
     * Tests getting content when there is a cache miss.
     * @throws GECommsException Ignore this.
     */
    @Test
    public void getContentFreshTest() throws GECommsException {
        // Setup
        fixture.setOnline(true, false, false);
        when(mockDB.getCachedContent(any(GTag.class), anyString(), anyInt())).thenReturn("");

        // Ensure content can be gained still
        assertThat(fixture.getContent(dummyPayload, true), equalTo(dummyJson));

        // Ensure cache was checked, and that fresh content was gained
        verify(mockDB).getCachedContent(dummyPayload.tag(),
                                        dummyPayload.query(),
                                        dummyPayload.page());
        verify(mockOnlineComms).getContent(any(GContentPayload.class));
    }

    /**
     * Tests content getting response when there is a database error.
     */
    @Test
    public void getContentDBErrorTest() {
        // Setup
        fixture.setOnline(true, false, false);
        when(mockDB.getCachedContent(any(GTag.class), anyString(), anyInt())).thenReturn(null);

        // Ensure it fails
        assertThrows(GECommsException.class, () -> fixture.getContent(dummyPayload, true));
    }

    /**
     * Tests content getting response when there are problems parsing the data.
     */
    @Test
    public void getContentParseFailureTest() {
        // Setup
        when(mockParser.parseResponse(anyString())).thenReturn(null);

        // Check throws correct
        assertThrows(GECommsException.class, () -> fixture.getContent(dummyPayload, false));
    }

    /**
     * Tests content getting response when the HTTP response code is an error.
     */
    @Test
    public void getContentErrorResponseTest() {
        // Setup
        JsonObject errObj = new JsonObject();
        errObj.add("message", new JsonPrimitive("a whoopsie happened!"));
        dummyJson.add("response", errObj);

        // Check for each error code
        for (int i = 400; i < 600; i++) {
            when(mockResponse.statusCode()).thenReturn(i);
            assertThrows(GECommsException.class, () -> fixture.getContent(dummyPayload, false));
        }
    }

    /**
     * Tests trying to get content without the appropriate environment keys.
     */
    @Test
    public void getContentNoEnvVarTest() {
        // Setup
        fixture.setOnline(true, false, false);
        when(mockEnv.getenv("INPUT_API_KEY")).thenReturn(null);

        // Ensure when online that throws
        assertThrows(GECommsException.class, () -> fixture.getContent(dummyPayload, false));

        // Ensure that when offline doesn't throw
        fixture.setOnline(false, false, false);
        assertDoesNotThrow(() -> fixture.getContent(dummyPayload, false));
    }
}
