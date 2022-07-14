package model.comms.drivers;

import model.items.GContent;
import model.items.GTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests miscellaneous functionalities of drivers.
 */
public class GECommsMiscTest {
    private GEComms fixture;

    // Mocks + Dummy objects
    private GTag dummyTag;
    private GContent dummyContent;

    // Setup
    @BeforeEach
    public void setup() {
        // Dummy setup
        dummyTag = new GTag("snowy", "places", "brr", "weburl", "apiurl");
        dummyContent = new GContent("antarctica",
                                    "places",
                                    "Places",
                                    "1980-03-03T21:00:00Z",
                                    "Snow & the cold;",
                                    "weburl",
                                    "apiurl",
                                    1,
                                    10);

        // Fixture setup
        fixture = new GEDummyComms(0);
    }

    // NOTE: Empty list is not tested as front-end blocks empty
    //       sending/posting to begin with.

    // Tests
    /**
     * Tests the utility function for generating content body,
     * with and without encoding on the Web title (i.e. changing potentially bad
     * chars like &)
     */
    @Test
    public void makeOutputContentBodyTest() {
        // Test non-encoded
        String expectedNoEncode =
                """
                Here are some articles for the tag snowy:
                - Snow & the cold; | Published 1980-03-03T21:00:00Z
                """;
        assertThat(fixture.makeOutputContentBody(dummyTag, List.of(dummyContent), false),
                    equalTo(expectedNoEncode));

        // Test non-encoded
        String expectedEncode =
                """
                Here are some articles for the tag snowy:
                - Snow %26 the cold%3b | Published 1980-03-03T21:00:00Z
                """;
        assertThat(fixture.makeOutputContentBody(dummyTag, List.of(dummyContent), true),
                    equalTo(expectedEncode));
    }
}
