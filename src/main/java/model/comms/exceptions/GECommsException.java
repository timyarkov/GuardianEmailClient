package model.comms.exceptions;

/**
 * Exception for if something goes wrong during communications.
 * A -1 code means an error unrelated to the HTTP operation, otherwise
 * the appropriate HTTP response code will be present.
 */
public class GECommsException extends Exception {
    private int code;
    private String msg;

    /**
     * Creates a communications exception.
     * @param code Status code; set to HTTP response code, or -1 if a local
     *             coding problem.
     * @param msg Message to use.
     */
    public GECommsException(int code, String msg) {
        this.msg = msg;
        this.code = code;
    }

    /**
     * Returns the response code.
     * @return Response code.
     */
    public int getCode() {
        return this.code;
    }

    /**
     * Returns the message for this exception.
     * @return Message for this exception.
     */
    @Override
    public String getMessage() {
        return String.format("Communications Error %d | %s", this.code, this.msg);
    }
}