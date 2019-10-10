package com.github.codebje.cc65debug;

import java.util.HashMap;
import java.util.Map;

/**
 * A CC65 debug info file keyword
 */
enum Keyword {
    LINETYPE(""),       // pseudo-keyword for the line type
    ABSOLUTE("abs"),
    ADDRSIZE("addrsize"),
    AUTO("auto"),
    COUNT("count"),
    CSYM("csym"),
    DEF("def"),
    ENUM("enum"),
    EQUATE("equ"),
    EXPORT("exp"),
    EXTERN("ext"),
    FILE("file"),
    FUNC("func"),
    GLOBAL("global"),
    ID("id"),
    IMPORT("imp"),
    INFO("info"),
    LABEL("lab"),
    LIBRARY("lib"),
    LINE("line"),
    LONG("long"),
    MAJOR("major"),
    MINOR("minor"),
    MODULE("mod"),
    MTIME("mtime"),
    NAME("name"),
    OFFS("offs"),
    OUTPUTNAME("oname"),
    OUTPUTOFFS("ooffs"),
    PARENT("parent"),
    REF("ref"),
    REGISTER("reg"),
    RO("ro"),
    RW("rw"),
    SC("sc"),
    SCOPE("scope"),
    SEGMENT("seg"),
    SIZE("size"),
    SPAN("span"),
    START("start"),
    STATIC("static"),
    STRUCT("struct"),
    SYM("sym"),
    TYPE("type"),
    VALUE("val"),
    VAR("var"),
    VERSION("version"),
    ZEROPAGE("zp");

    private final String token;
    private static final Map<String,Keyword> lookup = new HashMap<>();

    static {
        for (Keyword kw : Keyword.values()) {
            lookup.put(kw.token, kw);
        }
    }

    Keyword(String token) {
        this.token = token;
    }

    public static Keyword findKeyword(String token) {
        return lookup.get(token);
    }
}
