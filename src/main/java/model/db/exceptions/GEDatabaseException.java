package model.db.exceptions;

/**
 * Exception to throw for something going wrong during
 * database operations.
 */
public class GEDatabaseException extends RuntimeException {
    private String msg;
    private boolean isCritical;

    /**
     * Creates a database error.
     * @param isCritical Whether the error is critical (i.e. if program should stop)
     * @param msg Error Message.
     */
    public GEDatabaseException(boolean isCritical, String msg) {
        this.isCritical = isCritical;
        this.msg = msg;
    }

    /**
     * Returns whether this error was critical or not (i.e.
     * is the DB usable or not).
     * @return Whether error is critical or not.
     */
    public boolean isCritical() {
        return this.isCritical;
    }

    /**
     * Returns the message for this exception.
     * @return Message for this exception.
     */
    @Override
    public String getMessage() {
        return this.isCritical ? String.format("CRITICAL Database Error | %s", this.msg) :
                                 String.format("Database Error | %s", this.msg);
    }
}
