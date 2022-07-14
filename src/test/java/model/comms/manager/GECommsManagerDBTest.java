package model.comms.manager;

import model.comms.drivers.GEComms;
import model.comms.payloads.GContentPayload;
import model.db.GEDatabase;
import model.items.GTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests that the comms manager actually uses the database.
 */
public class GECommsManagerDBTest {
    private GECommsManager fixture;

    // Mocks + Dummies
    private GEComms mockComms;
    private GEDatabase mockDB;

    private GContentPayload dummyPayload;

    // Setup
    @BeforeEach
    public void setup() {
        // Mock setup
        mockComms = mock(GEComms.class);
        mockDB = mock(GEDatabase.class);

        // Dummy Setup
        GTag dummyTag = new GTag("fish", "animal", "FISH", "somewhere", "api somewhere");
        dummyPayload = new GContentPayload(dummyTag, "hi!", 1, 10);

        // Fixture setup
        fixture = new GECommsManagerImpl(true, true, true);
        fixture.injectNewDrivers(mockComms, mockComms);
        fixture.injectDatabase(mockDB);
    }

    // Tests
    /**
     * Ensures a content cache check is passed to the DB manager.
     */
    @Test
    public void isContentCachedTest() {
        // Ensure it asks the database for data
        fixture.isContentCached(dummyPayload);
        verify(mockDB).getCachedContent(dummyPayload.tag(),
                                        dummyPayload.query(),
                                        dummyPayload.page());
    }

    /**
     * Ensures a content cache clear request is passed to the DB manager.
     */
    @Test
    public void clearCacheTest() {
        // Ensure it calls the database to clear
        fixture.clearContentCache();
        verify(mockDB).clearCachedContent();
    }
}
