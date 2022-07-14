package model.comms.manager;

import com.google.gson.JsonObject;
import model.comms.drivers.GEComms;
import model.comms.drivers.GEResponse;
import model.comms.exceptions.GECommsException;
import model.comms.payloads.ESendPayload;
import model.comms.util.JSONParser;
import model.env.Environment;
import model.items.GContent;
import model.items.GTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests email functionalities of a comms manager.
 */
public class GECommsManagerEmailTest {
    private GECommsManager fixture;

    // Mocks + Dummies
    private GEComms mockOnlineComms;
    private GEComms mockOfflineComms;
    private GEResponse mockResponse;
    private JSONParser mockParser;
    private Environment mockEnv;

    private JsonObject dummyJson;
    private ESendPayload dummyPayload;

    // Setup
    @BeforeEach
    public void setup() throws GECommsException {
        // Mock setup
        mockOnlineComms = mock(GEComms.class);
        mockOfflineComms = mock(GEComms.class);
        mockResponse = mock(GEResponse.class);
        when(mockResponse.body()).thenReturn("");
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockOnlineComms.sendEmail(ArgumentMatchers.any(ESendPayload.class)))
                .thenReturn(mockResponse);
        when(mockOfflineComms.sendEmail(ArgumentMatchers.any(ESendPayload.class)))
                .thenReturn(mockResponse);

        mockParser = mock(JSONParser.class);
        when(mockParser.parseResponse(ArgumentMatchers.anyString())).thenReturn(dummyJson);

        mockEnv = mock(Environment.class);
        when(mockEnv.getenv("SENDGRID_API_KEY")).thenReturn("post office keys");
        when(mockEnv.getenv("SENDGRID_API_EMAIL")).thenReturn("post office address");

        // Dummies setup
        dummyJson = new JsonObject();

        GTag dummyTag = new GTag("fishing_spots", "advice", "Cool Fishing Spots", "url", "url");
        GContent dummyContent = new GContent("beach",
                                             "place",
                                             "places",
                                             "2022",
                                             "The Beach",
                                             "url",
                                             "url",
                                             1,
                                             10);
        dummyPayload = new ESendPayload("walrus", dummyTag, List.of(dummyContent));

        // Fixture setup
        fixture = new GECommsManagerImpl(false, false, false);
        fixture.injectNewDrivers(mockOnlineComms, mockOfflineComms);
        fixture.injectNewParser(mockParser);
        fixture.injectNewEnvironment(mockEnv);
    }

    // Tests
    /**
     * Tests usual email sending.
     * @throws GECommsException Ignore this.
     */
    @Test
    public void sendEmailTest() throws GECommsException {
        // Setup "online"
        fixture.setOnline(false, true, false);

        assertTrue(fixture.sendEmail(dummyPayload));

        // Ensure correct comms used
        verify(mockOnlineComms).sendEmail(ArgumentMatchers.any(ESendPayload.class));

        // Check "offline"
        fixture.setOnline(false, false, false);
        assertTrue(fixture.sendEmail(dummyPayload));

        // Ensure correct comms used
        verify(mockOfflineComms).sendEmail(ArgumentMatchers.any(ESendPayload.class));
    }

    /**
     * Tests sending response when there is an HTTP error status.
     */
    @Test
    public void sendEmailResponseErrorTest() {
        // Setup
        when(mockResponse.body()).thenReturn("{\"colossal huge disastrous\":\"error\"}");

        // Check all error codes are caught
        for (int i = 400; i < 600; i++) {
            when(mockResponse.statusCode()).thenReturn(i);
            assertThrows(GECommsException.class, () -> fixture.sendEmail(dummyPayload));
        }
    }

    /**
     * Tests trying to send without an API key environment variable.
     */
    @Test
    public void sendEmailNoKeyEnvVarTest() {
        // Setup
        fixture.setOnline(false, true, false);
        when(mockEnv.getenv("SENDGRID_API_KEY")).thenReturn(null);

        // Ensure when online that throws
        assertThrows(GECommsException.class, () -> fixture.sendEmail(dummyPayload));

        // Ensure that when offline doesn't throw
        fixture.setOnline(false, false, false);
        assertDoesNotThrow(() -> fixture.sendEmail(dummyPayload));
    }

    /**
     * Tests sending an email with no sender email environment variable.
     */
    @Test
    public void sendEmailNoEmailEnvVarTest() {
        // Setup
        fixture.setOnline(false, true, false);
        when(mockEnv.getenv("SENDGRID_API_EMAIL")).thenReturn(null);

        // Ensure when online that throws
        assertThrows(GECommsException.class, () -> fixture.sendEmail(dummyPayload));

        // Ensure that when offline doesn't throw
        fixture.setOnline(false, false, false);
        assertDoesNotThrow(() -> fixture.sendEmail(dummyPayload));
    }
}
