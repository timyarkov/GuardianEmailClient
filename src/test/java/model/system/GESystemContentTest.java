package model.system;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import model.comms.exceptions.GECommsException;
import model.comms.manager.GECommsManager;
import model.comms.payloads.GContentPayload;
import model.db.GEDatabase;
import model.env.Environment;
import model.items.GContent;
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
import static org.mockito.Mockito.mock;

/**
 * Tests content getting operations for a GESystem.
 */
public class GESystemContentTest {
    private GESystem fixture;

    // Mocks + Dummy objects
    private Environment mockEnv;
    private GECommsManager mockComms;
    private GEDatabase mockDB;
    private GESystemObserver mockObserver;
    private JsonObject contentRet;
    private GTag dummyTag;

    // NOTE: For testing system error state, expected behaviour is that error state
    //       is set, broadcast is sent to observers, and then the error state is
    //       unset. Thus, checking the error message has changed and that observers
    //       were notified is used as a proxy for checking an error state was raised.

    // Setup
    @BeforeEach
    public void setup() throws GECommsException {
        // Data setup
        String contentJson =
                """
                {
                    "response": {
                        "status": "ok",
                        "userTier": "free",
                        "total": 1,
                        "startIndex": 1,
                        "pageSize": 10,
                        "currentPage": 1,
                        "pages": 1,
                        "orderBy": "newest",
                        "results": [
                            {
                                "id": "politics/blog/2014/feb/17/alex-salmond-speech-first-minister-scottish-independence-eu-currency-live",
                                "sectionId": "politics",
                                "sectionName": "Politics",
                                "webPublicationDate": "2014-02-17T12:05:47Z",
                                "webTitle": "Alex Salmond speech – first minister hits back over Scottish independence – live",
                                "webUrl": "https://www.theguardian.com/politics/blog/2014/feb/17/alex-salmond-speech-first-minister-scottish-independence-eu-currency-live",
                                "apiUrl": "https://content.guardianapis.com/politics/blog/2014/feb/17/alex-salmond-speech-first-minister-scottish-independence-eu-currency-live"
                            },
                            {
                                "id": "politics/blog/2014/feb/17/alex-salmond-speech-first-minister-scottish-independence-eu-currency-live",
                                "sectionId": "politics",
                                "sectionName": "Politics",
                                "webPublicationDate": "2014-02-17T12:05:47Z",
                                "webTitle": "Pingu becomes President of the Antarctic; what happens next?",
                                "webUrl": "https://www.youtube.com/watch?v=aYNXqKaZWR4",
                                "apiUrl": "https://content.guardianapis.com/politics/blog/2014/feb/17/alex-salmond-speech-first-minister-scottish-independence-eu-currency-live"
                            }
                        ]
                    }
                }
                """;

        // Mock Setup
        mockEnv = mock(Environment.class);
        when(mockEnv.getenv("INPUT_API_KEY"))
                .thenReturn("pingu's secret key");

        mockObserver = mock(GESystemObserver.class);

        mockComms = mock(GECommsManager.class);
        contentRet = JsonParser.parseString(contentJson).getAsJsonObject();
        when(mockComms.getContent(ArgumentMatchers.any(GContentPayload.class),
                                  ArgumentMatchers.anyBoolean())).thenReturn(contentRet);

        mockDB = mock(GEDatabase.class);

        // Dummy objects setup
        dummyTag = new GTag("places/snow", "keyword", "Snowy Places",
                            "https://en.wikipedia.org/wiki/Penguin",
                            "https://en.wikipedia.org/wiki/Penguin");

        // Fixture setup
        fixture = new GESystemImpl(false, false, false, mockDB);
        fixture.injectNewCommsManager(mockComms);
        fixture.injectNewEnvironment(mockEnv);
    }

    // Tests
    /**
     * Tests content getting (with cache).
     * @throws GECommsException Ignore this.
     */
    @Test
    public void testGetContent() throws GECommsException {
        List<GContent> ret = fixture.getContent(dummyTag, "query", 1, true);

        // Assert that comms were used
        verify(mockComms).getContent(ArgumentMatchers.any(GContentPayload.class), eq(true));

        // Assert correct parsing
        assertThat(ret, containsInAnyOrder(
                new GContent("politics/blog/2014/feb/17/alex-salmond-speech-first-minister-scottish-independence-eu-currency-live",
                        "politics",
                        "Politics",
                        "2014-02-17T12:05:47Z",
                        "Alex Salmond speech – first minister hits back over Scottish independence – live",
                        "https://www.theguardian.com/politics/blog/2014/feb/17/alex-salmond-speech-first-minister-scottish-independence-eu-currency-live",
                        "https://content.guardianapis.com/politics/blog/2014/feb/17/alex-salmond-speech-first-minister-scottish-independence-eu-currency-live",
                        1,
                        1),
                new GContent("politics/blog/2014/feb/17/alex-salmond-speech-first-minister-scottish-independence-eu-currency-live",
                        "politics",
                        "Politics",
                        "2014-02-17T12:05:47Z",
                        "Pingu becomes President of the Antarctic; what happens next?",
                        "https://www.youtube.com/watch?v=aYNXqKaZWR4",
                        "https://content.guardianapis.com/politics/blog/2014/feb/17/alex-salmond-speech-first-minister-scottish-independence-eu-currency-live",
                        1,
                        1)
        ));
    }

    /**
     * Tests content getting (without cache use).
     * @throws GECommsException Ignore this.
     */
    @Test
    public void testGetContentNoCacheUse() throws GECommsException {
        // Ensure that the cache option specified is respected
        List<GContent> ret = fixture.getContent(dummyTag, "query", 1, false);
        verify(mockComms).getContent(ArgumentMatchers.any(GContentPayload.class), eq(false));
    }

    /**
     * Tests content getting when there is a failure of some sort.
     * @throws GECommsException Ignore this.
     */
    @Test
    public void testGetContentFail() throws GECommsException {
        // Set up mock to fail, set up observer
        when(mockComms.getContent(ArgumentMatchers.any(GContentPayload.class),
                                  ArgumentMatchers.anyBoolean())).thenThrow(GECommsException.class);
        fixture.addObserver(mockObserver);

        // Expected behaviour of empty list, and that error event occurs
        String preMsg = fixture.getErrorMessage();
        List<GContent> ret = fixture.getContent(dummyTag,
                "blizzard making connection fuzzy",
                1,
                true);
        assertTrue(ret.isEmpty());
        assertNotEquals(preMsg, fixture.getErrorMessage());
        verify(mockObserver).update();
    }
}
