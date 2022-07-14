package model.system;

import model.comms.exceptions.GECommsException;
import model.comms.manager.GECommsManager;
import model.comms.payloads.ESendPayload;
import model.db.GEDatabase;
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
 * Tests email sending operations for a GESystem.
 */
public class GESystemEmailTest {
    private GESystem fixture;

    // Mocks + Dummy objects
    private Environment mockEnv;
    private GECommsManager mockComms;
    private GEDatabase mockDB;
    private GESystemObserver mockObserver;
    private GTag dummyTag;
    private GContent dummyContent;

    // NOTE: For testing system error state, expected behaviour is that error state
    //       is set, broadcast is sent to observers, and then the error state is
    //       unset. Thus, checking the error message has changed and that observers
    //       were notified is used as a proxy for checking an error state was raised.

    // Setup
    @BeforeEach
    public void setup() throws GECommsException {
        // Mock Setup
        mockEnv = mock(Environment.class);
        when(mockEnv.getenv("SENDGRID_API_KEY"))
                .thenReturn("key to igloo post office");
        when(mockEnv.getenv("SENDGRID_API_EMAIL"))
                .thenReturn("igloo post office address");

        mockObserver = mock(GESystemObserver.class);

        mockComms = mock(GECommsManager.class);
        when(mockComms.sendEmail(ArgumentMatchers.any(ESendPayload.class)))
                .thenReturn(true);

        mockDB = mock(GEDatabase.class);

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

        // Fixture setup
        fixture = new GESystemImpl(false, false, false, mockDB);
        fixture.injectNewCommsManager(mockComms);
        fixture.injectNewEnvironment(mockEnv);
    }

    // Tests
    /**
     * Tests email sending.
     * @throws GECommsException Ignore this.
     */
    @Test
    public void testSendEmail() throws GECommsException {
        assertTrue(fixture.sendEmail(dummyTag, List.of(dummyContent), "walrus"));

        // Assert comms used
        verify(mockComms).sendEmail(ArgumentMatchers.any(ESendPayload.class));
    }

    /**
     * Tests email sending when there is some sort of error.
     * @throws GECommsException Ignore this.
     */
    @Test
    public void testSendEmailFail() throws GECommsException {
        // Set up mock to fail, set up observer
        when(mockComms.sendEmail(ArgumentMatchers.any(ESendPayload.class)))
                .thenThrow(GECommsException.class);
        fixture.addObserver(mockObserver);

        // Expected behaviour of false return and error event
        String preMsg = fixture.getErrorMessage();
        assertFalse(fixture.sendEmail(dummyTag, List.of(dummyContent), "cousin swan"));
        assertNotEquals(preMsg, fixture.getErrorMessage());
        verify(mockObserver).update();
    }
}