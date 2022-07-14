package model.comms.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests JSON parser modules.
 */
public class JSONParserTest {
    private JSONParser fixture;

    // Setup
    @BeforeEach
    public void setup() {
        fixture = new JSONParserImpl();
    }

    // Tests
    /**
     * Tests a usual JSON parse.
     */
    @Test
    public void parseJsonTest() {
        // Setup
        String json =
                """
                {
                    "igloo_material": "snow",
                    "igloo_contract": {
                        "signed_by": "pingu"
                    },
                    "igloo_items": [
                        "desk",
                        "keys"
                    ]
                }
                """;
        JsonObject expected = new JsonObject();
        expected.add("igloo_material", new JsonPrimitive("snow"));

        JsonObject e_contract = new JsonObject();
        e_contract.add("signed_by", new JsonPrimitive("pingu"));
        expected.add("igloo_contract", e_contract);

        JsonArray e_items = new JsonArray();
        e_items.add("desk");
        e_items.add("keys");
        expected.add("igloo_items", e_items);

        // Check
        assertThat(fixture.parseResponse(json), equalTo(expected));
    }

    /**
     * Tests response to unparsable input.
     */
    @Test
    public void unparseableTest() {
        String bad = "!ha}cke r :}{}}guy!!";
        assertNull(fixture.parseResponse(bad));
    }
}
