package co.sena.academic.adapter.out.actors;

public class ServiceAccessTokenUnavailableException extends RuntimeException {
    public ServiceAccessTokenUnavailableException(String message) {
        super(message);
    }

    public ServiceAccessTokenUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
