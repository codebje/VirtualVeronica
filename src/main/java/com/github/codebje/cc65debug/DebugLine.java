package com.github.codebje.cc65debug;

/**
 * A source file line reference.
 *
 * A line is a location in a source file. It is module dependent, which means that if two modules
 * use the same source file, each one has its own line information for this file. While the
 * assembler has also column information, it is dropped early because it would generate much more
 * data. A line may have one or more spans attached if code or data is generated.
 *
 */
class DebugLine {
    /* Line types */
    public static final int TYPE_ASSEMBLER  = 0;    // Assembler source
    public static final int TYPE_EXTERNAL   = 1;    // Externally supplied (i.e. C)
    public static final int TYPE_MACRO      = 2;    // Macro expansion

    private final int fileId;
    private final int lineNumber;
    private final int spans[];
    private final int type;
    private final int count;

    DebugLine(int fileId, int lineNumber, int[] spans, int type, int count) {
        this.fileId = fileId;
        this.lineNumber = lineNumber;
        this.spans = spans;
        this.type = type;
        this.count = count;
    }

    /**
     * Get the source file ID
     *
     * @return the source file ID
     */
    public int getFileId() {
        return fileId;
    }

    /**
     * Get the source file line number
     *
     * @return the source file line number
     */
    public int getLineNumber() {
        return lineNumber;
    }

    public int[] getSpans() {
        return spans;
    }

    /**
     * Get the line type constant
     *
     *  - {@value DebugInfo#CC65_INV_ID} - not specified
     *  - {@value #TYPE_ASSEMBLER}       - Assembler source
     *  - {@value #TYPE_EXTERNAL}        - Externally supplied (i.e. C)
     *  - {@value #TYPE_MACRO}           - Macro expansion
     *
     * @return the line type constant
     */
    public int getType() {
        return type;
    }

    /**
     * Get the macro expansion count
     *
     * When {@link #getType} is {@value #TYPE_MACRO}, this returns
     * the macro's nesting expansion count.
     *
     * @return the macro expansion count
     */
    public int getCount() {
        return count;
    }
}
