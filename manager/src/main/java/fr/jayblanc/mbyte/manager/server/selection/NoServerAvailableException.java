package fr.jayblanc.mbyte.manager.server.selection;

/**
 * Exception thrown when no server is available for store deployment.
 */
public class NoServerAvailableException extends Exception {

    public NoServerAvailableException(String message) {
        super(message);
    }

    public NoServerAvailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
