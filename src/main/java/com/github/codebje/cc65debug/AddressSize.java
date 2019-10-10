package com.github.codebje.cc65debug;

/**
 * An address size.
 */
public enum AddressSize {
    INVALID ("unknown"),
    DEFAULT ("default"),
    ZEROPAGE ("zeropage"),
    ABSOLUTE ("absolute"),
    FAR ("far"),
    LONG ("long");

    private final String name;

    private AddressSize(String name) {
        this.name = name;
    }

    /**
     * Get the name of the address size.
     *
     * @return the name of the address size
     */
    public String getName() {
        return name;
    }

    public static AddressSize lookup(String name) {
        for (AddressSize size : AddressSize.values()) {
            if (size.getName().equals(name))
                return size;
        }
        return null;
    }
}
