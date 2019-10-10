package com.github.codebje.cc65debug;

/**
 * A CC65 debug information scope.
 *
 * Each {@link DebugModule} has a main scope where all {@link DebugSymbol}s live, that are
 * specified outside other scopes. Additional nested scopes may be specified in the
 * sources. So scopes have a one to many relation: Each scope (with the exception of the main
 * scope) has exactly one parent and may have several child scopes. Scopes may not cross modules.
 *
 */
class DebugScope {
    public final static int CC65_SCOPE_GLOBAL = 0;        // Global scope
    public final static int CC65_SCOPE_MODULE = 1;        // Module scope
    public final static int CC65_SCOPE_SCOPE  = 2;        // .PROC/.SCOPE
    public final static int CC65_SCOPE_STRUCT = 3;        // .STRUCT
    public final static int CC65_SCOPE_ENUM   = 4;        // .ENUM

    private final String name;
    private final int type;
    private final int size;
    private final int parentId;
    private final int symbolId;
    private final int moduleId;

    DebugScope(String name, int type, int size, int parentId, int symbolId, int moduleId) {
        this.name = name;
        this.type = type;
        this.size = size;
        this.parentId = parentId;
        this.symbolId = symbolId;
        this.moduleId = moduleId;
    }

    /**
     * Get the name of the scope.
     *
     * The name may be empty.
     *
     * @return the name of the scope
     */
    public String getName() {
        return name;
    }

    /**
     * Get the type of the scope.
     *
     * {@value #CC65_SCOPE_GLOBAL} - Global scope
     * {@value #CC65_SCOPE_MODULE} - Module scope
     * {@value #CC65_SCOPE_SCOPE}  - .PROC/.SCOPE
     * {@value #CC65_SCOPE_STRUCT} - .STRUCT
     * {@value #CC65_SCOPE_ENUM}   - .ENUM
     *
     * @return the type of the scope
     */
    public int getType() {
        return type;
    }

    /**
     * Get the size of the scope.
     *
     * This is the size of the {@link DebugSpan} for the active segment.
     *
     * @return the size of the scope
     */
    public int getSize() {
        return size;
    }

    /**
     * Get the ID of the parent scope.
     *
     * This is {@value DebugInfo#CC65_INV_ID} for the main scope.
     *
     * @return the ID of the parent scope.
     */
    public int getParentId() {
        return parentId;
    }

    /**
     * Get the ID of the attached symbol for .PROC scopes.
     *
     * This is {@value DebugInfo#CC65_INV_ID} for other scope types.
     *
     * @return the ID of the attached symbol
     */
    public int getSymbolId() {
        return symbolId;
    }

    /**
     * Get the ID of the module where the scope comes from.
     *
     * @return the ID of the module where the scope comes from
     */
    public int getModuleId() {
        return moduleId;
    }
}
