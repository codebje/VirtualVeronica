package com.github.codebje.cc65debug;

/**
 * The type of a symbol.
 */
enum SymbolType {
    EQUATE("equ"),
    LABEL("lab"),
    IMPORT("imp");

    private final String code;

    SymbolType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static SymbolType typeForCode(String code) {
        for (SymbolType sym : SymbolType.values()) {
            if (sym.getCode().equals(code)) {
                return sym;
            }
        }
        return null;
    }
}
