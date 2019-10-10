package com.github.codebje.cc65debug;

import java.util.stream.Stream;

/**
 * A CC65 debug info file value
 */
class Value {
    private final String value;

    Value(String value) {
        this.value = value;
    }

    int getInteger() {
        if (value.startsWith("0x"))
            return Integer.parseInt(value.substring(2), 16);
        else
            return Integer.parseInt(value);
    }

    String getString() {
        return value;
    }

    int[] getArray() {
        if (value.isEmpty()) {
            return new int[] {};
        } else {
            String[] parts = value.split("\\+");
            return Stream.of(parts).mapToInt(Integer::parseInt).toArray();
        }
    }
}
