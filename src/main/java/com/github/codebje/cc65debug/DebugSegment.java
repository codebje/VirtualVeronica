package com.github.codebje.cc65debug;

/**
 * A CC65 debug info segment.
 *
 * This represents one segment in the linked program.
 *
 * It is also possible to retrieve the {@link DebugSpan}s for sections (a section is the part of a
 * segment that comes from one {@link DebugModule}). Since the main {@link DebugScope} covers a
 * whole module, and the main scope has spans assigned (if not empty), the spans for the main
 * scope of a module are also the spans for the sections in the segments.
 *
 */
class DebugSegment {
    private final String name;
    private final int startAddress;
    private final int size;
    private final String outputName;
    private final int outputOffset;

    DebugSegment(String name, int startAddress, int size, String outputName, int outputOffset) {
        this.name = name;
        this.startAddress = startAddress;
        this.size = size;
        this.outputName = outputName;
        this.outputOffset = outputOffset;
    }

    /**
     * Get the name of the segment.
     *
     * @return the name of the segment
     */
    public String getName() {
        return name;
    }

    /**
     * Get the start address of the segment.
     *
     * @return the start address of the segment
     */
    public int getStartAddress() {
        return startAddress;
    }

    /**
     * Get the size of the segment.
     *
     * @return the size of the segment
     */
    public int getSize() {
        return size;
    }

    /**
     * Get the name of the output file this segment was written to.
     *
     * This may be NULL if the data wasn't written to any output file, such as for the BSS segment.
     *
     * @return the name of the output file this segment was written to
     */
    public String getOutputName() {
        return outputName;
    }

    /**
     * Get the offset of this segment in the output file.
     *
     * This is invalid if {@link #getOutputName()} is {@literal NULL}.
     *
     * @return the offset of this segment in the output file
     */
    public int getOutputOffset() {
        return outputOffset;
    }
}
