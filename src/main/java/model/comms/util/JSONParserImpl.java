package model.comms.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * Implementation of a JSON parser.
 */
public class JSONParserImpl implements JSONParser {
    /**
     * Parses the response into JSON.
     * @param response Response to parse. Cannot be null.
     * @return Parsed response if successful, null if unsuccessful
     *         or bad parameters.
     */
    @Override
    public JsonObject parseResponse(String response) {
        if (response == null) {
            return null;
        }

        try {
            return JsonParser.parseString(response).getAsJsonObject();
        } catch (JsonParseException e) {
            return null;
        }
    }
}
