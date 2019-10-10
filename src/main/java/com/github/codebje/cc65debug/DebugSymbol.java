package com.github.codebje.cc65debug;

/**
 * A CC65 debug info symbol.
 *
 * A symbol is a label, equate, or import defined in a source file.
 *
 * Beware: Even for an import, the id of the corresponding export may be
 * {@value DebugInfo#CC65_INV_ID}. This happens if the {@link DebugModule} with the export has no
 * debug information. So make sure that your application can handle it.
 *
 */
class DebugSymbol {
    private final String name;
    private final SymbolType type;
    private final AddressSize addressSize;
    private final long value;
    private final int size;
    private final int exportId;
    private final int segmentId;
    private final int scopeId;
    private final int parentId;
    private final int[] definitionIds;
    private final int[] referenceIds;

    DebugSymbol(String name, SymbolType type, AddressSize addressSize, long value, int size, int exportId, int segmentId, int scopeId, int parentId, int[] definitionIds, int[] referenceIds) {
        this.name = name;
        this.type = type;
        this.addressSize = addressSize;
        this.value = value;
        this.size = size;
        this.exportId = exportId;
        this.segmentId = segmentId;
        this.scopeId = scopeId;
        this.parentId = parentId;
        this.definitionIds = definitionIds;
        this.referenceIds = referenceIds;
    }

    public String getName() {
        return name;
    }

    public SymbolType getType() {
        return type;
    }

    public AddressSize getAddressSize() {
        return addressSize;
    }

    public long getValue() {
        return value;
    }

    public int getSize() {
        return size;
    }

    public int getExportId() {
        return exportId;
    }

    public int getSegmentId() {
        return segmentId;
    }

    public int getScopeId() {
        return scopeId;
    }

    public int getParentId() {
        return parentId;
    }

    public int[] getDefinitionIds() {
        return definitionIds;
    }

    public int[] getReferenceIds() {
        return referenceIds;
    }
}
