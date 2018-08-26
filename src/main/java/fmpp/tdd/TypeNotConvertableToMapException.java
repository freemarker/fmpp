package fmpp.tdd;

import fmpp.util.ExceptionCC;

/**
 * Thrown by {@link TddUtil#convertToDataMap(Object)}; see there.
 *
 * @since 0.9.16
 */
public class TypeNotConvertableToMapException extends ExceptionCC {

    public TypeNotConvertableToMapException(String message, Throwable cause) {
        super(message, cause);
    }

    public TypeNotConvertableToMapException(String message) {
        this(message, null);
    }

    public TypeNotConvertableToMapException(Throwable cause) {
        this("Failed to convert value to Map", cause);
    }
    
}
