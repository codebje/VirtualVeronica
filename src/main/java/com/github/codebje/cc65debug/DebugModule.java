package com.github.codebje.cc65debug;

/**
 * CC65 debug module information
 *
 * A module is actually an object file. It is generated from one or more source files and may come
 * from a library. The assembler generates a main scope for symbols declared outside user generated
 * scopes. The main scope has an empty name.
 *
 */
class DebugModule {
    private final String moduleName;
    private final int mainSourceId;
    private final int libraryId;
    private final int mainScopeId;

    DebugModule(String moduleName, int mainSourceId, int libraryId, int mainScopeId) {
        this.moduleName = moduleName;
        this.mainSourceId = mainSourceId;
        this.libraryId = libraryId;
        this.mainScopeId = mainScopeId;
    }

    /**
     * Get the name of the module including the path.
     *
     * @return the name of the module including the path
     */
    public String getModuleName() {
        return moduleName;
    }

    /**
     * Get the id of the main source file.
     *
     * @return the id of the main source file
     */
    public int getMainSourceId() {
        return mainSourceId;
    }

    /**
     * Get the id of the library the module comes from.
     *
     * This is {@value DebugInfo#CC65_INV_ID} if there is no associated library for this module.
     *
     * @return the id of the library the module comes from
     */
    public int getLibraryId() {
        return libraryId;
    }

    /**
     * Get the id of the main scope for this module.
     *
     * @return the id of the main scope for this module
     */
    public int getMainScopeId() {
        return mainScopeId;
    }
}
