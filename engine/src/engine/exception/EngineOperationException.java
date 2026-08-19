package engine.exception;

/**
 * Thrown when an engine operation cannot be performed in the current state
 * (no file loaded, unknown event id, event already closed, illegal amount, etc.).
 */
public class EngineOperationException extends RuntimeException {

    public EngineOperationException(String message) {
        super(message);
    }
}
