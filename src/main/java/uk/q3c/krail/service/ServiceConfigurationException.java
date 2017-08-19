package uk.q3c.krail.service;

/**
 * Created by David Sowerby on 19 Aug 2017
 */
public class ServiceConfigurationException extends RuntimeException {
    public ServiceConfigurationException() {
    }

    public ServiceConfigurationException(String message) {
        super(message);
    }

    public ServiceConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServiceConfigurationException(Throwable cause) {
        super(cause);
    }

    public ServiceConfigurationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
