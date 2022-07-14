package model.comms.manager;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import model.comms.drivers.GEComms;
import model.comms.drivers.GEResponse;
import model.comms.exceptions.GECommsException;
import model.comms.payloads.GTagPayload;
import model.comms.util.JSONParser;
import model.env.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests the tags functionalities of the comms manager.
 */
public class GECommsManagerTagTest {
    private GECommsManager fixture;

    // Mocks + Dummies
    private GEComms mockOnlineComms;
    private GEComms mockOfflineComms;
    private GEResponse mockResponse;
    private JSONParser mockParser;
    private Environment mockEnv;

    private JsonObject dummyJson;
    private GTagPayload dummyPayload;

    // Setup
    @BeforeEach
    public void setup() throws GECommsException {
        // Dummies setup
        dummyJson = new JsonObject();
        dummyPayload = new GTagPayload("igloos", 1, 10);

        // Mock setup
        mockOnlineComms = mock(GEComms.class);
        mockOfflineComms = mock(GEComms.class);
        mockResponse = mock(GEResponse.class);
        when(mockResponse.body()).thenReturn("{\"pingu's\":\"letter\"}");
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockOnlineComms.getTags(ArgumentMatchers.any(GTagPayload.class)))
                .thenReturn(mockResponse);
        when(mockOfflineComms.getTags(ArgumentMatchers.any(GTagPayload.class)))
                .thenReturn(mockResponse);

        mockParser = mock(JSONParser.class);
        when(mockParser.parseResponse(ArgumentMatchers.anyString())).thenReturn(dummyJson);

        mockEnv = mock(Environment.class);
        when(mockEnv.getenv("INPUT_API_KEY")).thenReturn("antarctica press pass");

        // Fixture setup
        fixture = new GECommsManagerImpl(false, false, false);
        fixture.injectNewDrivers(mockOnlineComms, mockOfflineComms);
        fixture.injectNewParser(mockParser);
        fixture.injectNewEnvironment(mockEnv);
    }

    // Tests
    /**
     * Tests usual tags getting.
     * @throws GECommsException Ignore this.
     */
    @Test
    public void getTagsTest() throws GECommsException {
        // Setup "online"
        fixture.setOnline(true, false, false);

        // Assert correct object gained (result of parse)
        assertThat(fixture.getTags(dummyPayload), equalTo(dummyJson));

        // Verify parser used
        verify(mockParser).parseResponse(ArgumentMatchers.anyString());
        // Verify correct comms used
        verify(mockOnlineComms).getTags(ArgumentMatchers.any(GTagPayload.class));

        // Setup "offline"
        fixture.setOnline(false, false, false);

        assertThat(fixture.getTags(dummyPayload), equalTo(dummyJson));

        // Verify correct comms used
        verify(mockOfflineComms).getTags(ArgumentMatchers.any(GTagPayload.class));
    }

    /**
     * Tests tags getting when there is a failure in parsing the response.
     * @throws GECommsException Ignore this.
     */
    @Test
    public void getTagsParseFailureTest() {
        // Setup
        when(mockParser.parseResponse(ArgumentMatchers.anyString())).thenReturn(null);

        // Check throws correct
        assertThrows(GECommsException.class, () -> fixture.getTags(dummyPayload));
    }

    /**
     * Tests tags getting when there is an HTTP error response.
     * @throws GECommsException Ignore this.
     */
    @Test
    public void getTagsErrorResponseTest() {
        // Setup
        JsonObject errObj = new JsonObject();
        errObj.add("message", new JsonPrimitive("a whoopsie happened!"));
        dummyJson.add("response", errObj);

        // Check for each error code
        for (int i = 400; i < 600; i++) {
            when(mockResponse.statusCode()).thenReturn(i);
            assertThrows(GECommsException.class, () -> fixture.getTags(dummyPayload));
        }
    }

    /**
     * Tests tag getting with no API key environment variable.
     */
    @Test
    public void getTagsNoEnvVarTest() {
        // Setup
        fixture.setOnline(true, false, false);
        when(mockEnv.getenv("INPUT_API_KEY")).thenReturn(null);

        // Ensure when online that throws
        assertThrows(GECommsException.class, () -> fixture.getTags(dummyPayload));

        // Ensure that when offline doesn't throw
        fixture.setOnline(false, false, false);
        assertDoesNotThrow(() -> fixture.getTags(dummyPayload));
    }
}
