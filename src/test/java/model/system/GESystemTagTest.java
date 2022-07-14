package model.system;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import model.comms.exceptions.GECommsException;
import model.comms.manager.GECommsManager;
import model.comms.payloads.GTagPayload;
import model.db.GEDatabase;
import model.env.Environment;
import model.items.GTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import java.util.List;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Tests tag getting operations for a GESystem.
 */
public class GESystemTagTest {
    private GESystem fixture;

    // Mocks + Dummy objects
    private Environment mockEnv;
    private GECommsManager mockComms;
    private GEDatabase mockDB;
    private GESystemObserver mockObserver;
    private JsonObject tagsRet;

    // NOTE: For testing system error state, expected behaviour is that error state
    //       is set, broadcast is sent to observers, and then the error state is
    //       unset. Thus, checking the error message has changed and that observers
    //       were notified is used as a proxy for checking an error state was raised.

    // Setup
    @BeforeEach
    public void setup() throws GECommsException {
        // Data setup
        String tagJson =
                """
                {
                    "response": {
                        "status": "ok",
                        "userTier": "free",
                        "total": 65,
                        "startIndex": 1,
                        "pageSize": 10,
                        "currentPage": 1,
                        "pages": 7,
                        "results": [
                            {
                                "id": "katine/football",
                                "type": "keyword",
                                "webTitle": "Football",
                                "webUrl": "http://www.theguardian.com/katine/football",
                                "apiUrl": "http://beta.content.guardianapis.com/katine/football",
                                "sectionId": "katine",
                                "sectionName": "Katine"
                            }
                        ]
                    }
                }
                """;

        // Mock Setup
        mockEnv = mock(Environment.class);
        when(mockEnv.getenv("INPUT_API_KEY")).thenReturn("pingu's secret key");

        mockObserver = mock(GESystemObserver.class);

        mockComms = mock(GECommsManager.class);
        tagsRet = JsonParser.parseString(tagJson).getAsJsonObject();
        when(mockComms.getTags(ArgumentMatchers.any(GTagPayload.class)))
                .thenReturn(tagsRet);

        mockDB = mock(GEDatabase.class);

        // Fixture setup
        fixture = new GESystemImpl(false, false, false, mockDB);
        fixture.injectNewCommsManager(mockComms);
        fixture.injectNewEnvironment(mockEnv);
    }

    // Tests
    /**
     * Tests tag getting.
     * @throws GECommsException Ignore this.
     */
    @Test
    public void testGetTags() throws GECommsException {
        // Assert that comms were used
        List<GTag> ret = fixture.getTags("igloo");

        verify(mockComms).getTags(ArgumentMatchers.any(GTagPayload.class));

        // Assert correct parsing
        assertThat(ret, containsInAnyOrder(
                new GTag("katine/football",
                        "keyword",
                        "Football",
                        "http://www.theguardian.com/katine/football",
                        "http://beta.content.guardianapis.com/katine/football")
        ));
    }

    /**
     * Tests tag getting when there is an error of some sort.
     * @throws GECommsException Ignore this.
     */
    @Test
    public void testGetTagsFail() throws GECommsException {
        // Set up mock to fail, set up observer
        when(mockComms.getTags(ArgumentMatchers.any(GTagPayload.class)))
                .thenThrow(GECommsException.class);
        fixture.addObserver(mockObserver);

        // Expected behaviour of empty list, and that error event occurs
        String preMsg = fixture.getErrorMessage();
        List<GTag> ret = fixture.getTags("no network in antarctica");
        assertTrue(ret.isEmpty());
        assertNotEquals(preMsg, fixture.getErrorMessage());
        verify(mockObserver).update();
    }
}
