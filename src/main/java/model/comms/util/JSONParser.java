package model.comms.util;

import com.google.gson.JsonObject;

/**
 * JSON Parser for HTTP responses.
 */
public interface JSONParser {
    /**
     * Parses the response into JSON.
     * @param response Response to parse. Cannot be null.
     * @return Parsed response if successful, null if unsuccessful
     *         or bad parameters.
     */
    public JsonObject parseResponse(String response);
}
