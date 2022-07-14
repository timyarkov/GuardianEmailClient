package model.comms.drivers;

/**
 * Data gained from an HTTP response.
 * @param body Body of the response.
 * @param statusCode Status code of the response.
 */
public record GEResponse(
        String body,
        int statusCode
) { }
