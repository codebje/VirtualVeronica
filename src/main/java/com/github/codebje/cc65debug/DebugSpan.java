package com.github.codebje.cc65debug;

/**
 * A CC65 debug info span.
 *
 * A span is a small part of a {@link DebugSegment}. It has a start address and a size. Spans are
 * used to record sizes of other objects. {@link DebugLine}s and {@link DebugScope}s may have spans
 * attached, so it is possible to lookup which data was generated for these items.
 *
 */
class DebugSpan {
    private final int startAddress;
    private final int size;
    private final int segmentId;
    private final int typeId;

    DebugSpan(int startAddress, int size, int segmentId, int typeId) {
        this.startAddress = startAddress;
        this.size = size;
        this.segmentId = segmentId;
        this.typeId = typeId;
    }

    /**
     * Get the start address of the span.
     *
     * @return the start address of the span
     */
    public int getStartAddress() {
        return startAddress;
    }

    /**
     * Get the size of the span.
     *
     * @return the size of the span
     */
    public int getSize() {
        return size;
    }

    /**
     * Get the id of the segment where the span is located.
     *
     * @return the id of the {@link DebugSegment} where the span is located
     */
    public int getSegmentId() {
        return segmentId;
    }

    /**
     * Get the id of the type of the data in the span.
     *
     * {@value DebugInfo#CC65_INV_ID} - Not specified
     *
     * @return the id of the {@link DebugType} of the data in the span
     */
    public int getTypeId() {
        return typeId;
    }
}
