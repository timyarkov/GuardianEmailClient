package model.system;

import model.comms.exceptions.GECommsException;
import model.comms.manager.GECommsManager;
import model.comms.payloads.GContentPayload;
import model.comms.payloads.GTagPayload;
import model.db.GEDatabase;
import model.env.Environment;
import model.items.GContent;
import model.items.GTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests non API components of a GESystem; i.e., observer
 * operations, environment, and dependency injection.
 */
public class GESystemMiscTest {
    private GESystem fixture;

    // Mocks + Dummy objects
    private GECommsManager mockComms;
    private GEDatabase mockDB;
    private Environment mockEnv;
    private GESystemObserver mockObserver;

    // Setup
    @BeforeEach
    public void setup() {
        // Mock Setup
        mockEnv = mock(Environment.class);
        when(mockEnv.getenv("INPUT_API_KEY")).thenReturn("pingu's secret key");
        when(mockEnv.getenv("SENDGRID_API_KEY")).thenReturn("key to igloo post office");
        when(mockEnv.getenv("SENDGRID_API_EMAIL")).thenReturn("igloo post office address");
        when(mockEnv.getenv("REDDIT_API_CLIENT")).thenReturn("walrus' key");
        when(mockEnv.getenv("REDDIT_API_SECRET")).thenReturn("walrus' post history");

        mockObserver = mock(GESystemObserver.class);

        mockComms = mock(GECommsManager.class);
        mockDB = mock(GEDatabase.class);

        // Fixture setup
        fixture = new GESystemImpl(false, false, false, mockDB);
        fixture.injectNewEnvironment(mockEnv);
        fixture.injectNewCommsManager(mockComms);
    }

    // Tests
    /**
     * Tests the environment checking process.
     */
    @Test
    public void testEnvironmentCheck() {
        // Set up observers
        fixture.addObserver(mockObserver);

        // Valid environment
        assertTrue(fixture.checkEnvironmentVars(true, true));

        // Check case of each missing
        String[] vars = {"INPUT_API_KEY", "SENDGRID_API_KEY", "SENDGRID_API_EMAIL"};

        for (int i = 0; i < vars.length; i++) {
            // Reset vars
            for (String v : vars) {
                when(mockEnv.getenv(v)).thenReturn("valid!");
            }

            // Set current variable to be "missing"
            when(mockEnv.getenv(vars[i])).thenReturn(null);

            // Ensure false return and error event
            String preMsg = fixture.getErrorMessage();
            assertFalse(fixture.checkEnvironmentVars(true, true));
            assertNotEquals(preMsg, fixture.getErrorMessage());
            verify(mockObserver, times(i + 1)).update();
        }

        // Reset vars
        for (String v : vars) {
            when(mockEnv.getenv(v)).thenReturn("valid!");
        }

        // Check it doesn't care if reddit keys are missing (that should be checked
        // individually by postReddit method)
        when(mockEnv.getenv("REDDIT_API_CLIENT")).thenReturn(null);
        assertTrue(fixture.checkEnvironmentVars(true, true));

        when(mockEnv.getenv("REDDIT_API_CLIENT")).thenReturn("valid!"); // Reset

        when(mockEnv.getenv("REDDIT_API_SECRET")).thenReturn(null);
        assertTrue(fixture.checkEnvironmentVars(true, true));
    }

    /**
     * Tests that comms injection is respected.
     * @throws GECommsException Ignore this.
     */
    @Test
    public void testCommsInjection() throws GECommsException {
        // Invalid injection (null)
        assertFalse(fixture.injectNewCommsManager(null));

        // Valid injection
        GECommsManager newComms = mock(GECommsManager.class);
        assertTrue(fixture.injectNewCommsManager(newComms));
        // Verify it is used
        fixture.getTags("fresh communicator");
        verify(newComms).getTags(ArgumentMatchers.any(GTagPayload.class));
    }

    /**
     * Tests that environment injection is respected.
     */
    @Test
    public void testEnvInjection() {
        // Invalid injection (null)
        assertFalse(fixture.injectNewEnvironment(null));

        // Valid injection
        Environment newEnv = mock(Environment.class);
        assertTrue(fixture.injectNewEnvironment(newEnv));
        // Verify it is used
        fixture.checkEnvironmentVars(true, true);
        verify(newEnv).getenv("INPUT_API_KEY");
        verify(newEnv).getenv("SENDGRID_API_KEY");
        verify(newEnv).getenv("SENDGRID_API_EMAIL");
    }

    /**
     * Tests that operations on the observers list works.
     */
    @Test
    public void testObserverListOperations() {
        // Addition
        assertFalse(fixture.addObserver(null)); // Invalid add (null)
        assertTrue(fixture.addObserver(mockObserver)); // Valid add

        // Removal
        assertFalse(fixture.removeObserver(null)); // Invalid removal (null)
        assertTrue(fixture.removeObserver(mockObserver)); // Valid remove
        assertFalse(fixture.removeObserver(mockObserver)); // Invalid removal (not in list)
    }

    /**
     * Tests that cache clearing is passed to the comms manager.
     */
    @Test
    public void cacheClearTest() {
        // Ensure cache clear call is passed to comms manager
        fixture.clearCache();
        verify(mockComms).clearContentCache();
    }

    /**
     * Tests that cache checking is passed to the comms manager.
     */
    @Test
    public void isCachedContentTest() {
        // Setup
        GTag dummyTag = new GTag("pass", "the", "method", "over", "please");

        // Ensure cached content check is passed to comms manager
        fixture.isCachedContent(dummyTag, "thanks lots!", 1);
        verify(mockComms).isContentCached(any(GContentPayload.class));
    }

    /**
     * Tests operations of the reading list.
     */
    @Test
    public void readingListOperationsTest() {
        // Setup
        GContent dummyContent1 = new GContent("noot",
                                              "noot",
                                              "pingu",
                                              "softly",
                                              "screamed",
                                              "as",
                                              "the",
                                              1,
                                              2);
        GContent dummyContent2 = new GContent("fish",
                                              "swam",
                                              "away",
                                              "never",
                                              "to be",
                                              "seen",
                                              "again",
                                              1,
                                              2);

        // Normal add
        assertTrue(fixture.addToReadingList(dummyContent1));
        assertThat(fixture.getReadingList(), containsInAnyOrder(dummyContent1));

        // Duplicate add; should be rejected
        assertFalse(fixture.addToReadingList(dummyContent1));
        assertThat(fixture.getReadingList(), containsInAnyOrder(dummyContent1));

        // Null add; should be rejected
        assertFalse(fixture.addToReadingList(null));
        assertThat(fixture.getReadingList(), containsInAnyOrder(dummyContent1));

        // Setup for removal
        assertTrue(fixture.addToReadingList(dummyContent2));
        assertThat(fixture.getReadingList(), containsInAnyOrder(dummyContent1, dummyContent2));

        // Valid removal
        assertTrue(fixture.removeFromReadingList(dummyContent2));
        assertThat(fixture.getReadingList(), containsInAnyOrder(dummyContent1));

        // Removing item not in list; should be rejected
        assertFalse(fixture.removeFromReadingList(dummyContent2));
        assertThat(fixture.getReadingList(), containsInAnyOrder(dummyContent1));
    }
}