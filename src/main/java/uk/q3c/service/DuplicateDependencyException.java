package uk.q3c.service;

/**
 * Created by David Sowerby on 17 Dec 2015
 */
public class DuplicateDependencyException extends RuntimeException {
    public DuplicateDependencyException(String msg) {
        super(msg);
    }
}
