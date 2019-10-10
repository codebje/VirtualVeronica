package com.github.codebje.cc65debug;

/**
 * A CC65 debug info type.
 */
class DebugType {
    private final String value;
    // a sequence of hex-encoded bytes:
    //  "00": void
    //  "20": int
    //  "40": ptr
    //  "60": float
    //  "80": array
    //  "A0": func
    //  "C0": struct
    //  "E0": union
    // byte-order mark is a bit-flag at 0x10; signed flag is at 0x08
    // size is in lower 3 bits
    // array types either have the size in the lower 3 bits, or encoded separately in next byte
    // followed by a count byte, then a recursively parsed type

    // none of this is very interesting to unpack in detail right now.

    DebugType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
