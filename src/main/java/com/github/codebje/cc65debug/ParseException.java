package com.github.codebje.cc65debug;

/**
 * An exception thrown while parsing a CC65 debug info file
 */
public class ParseException extends Exception {
    public ParseException(String message) {
        super(message);
    }
}
