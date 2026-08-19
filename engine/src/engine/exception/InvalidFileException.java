package engine.exception;

/**
 * Thrown when a system-details XML file fails validation.
 * Unchecked by design (see course forum discussion) - the UI catches it
 * and presents the message to the user without crashing.
 */
public class InvalidFileException extends RuntimeException {

    public InvalidFileException(String message) {
        super(message);
    }

    public InvalidFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
