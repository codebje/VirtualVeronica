package com.github.codebje.cc65debug;

/**
 * A description of a source file location.
 *
 * A source file location consists of a file name, some attributes to help identify if the file
 * is the right contents, and a line number.
 */
public class SourceLocation {
    private final SourceFile file;
    private final DebugLine line;

    SourceLocation(SourceFile file, DebugLine line) {
        this.file = file;
        this.line = line;
    }

    /**
     * Get the name of the source file including the path.
     *
     * @return the name of the source file including the path
     */
    public String getFileName() {
        return file.getFileName();
    }

    /**
     * Get the size of the file at the time when it was read.
     *
     * @return the size of the file at the time when it was read
     */
    public long getFileSize() {
        return file.getFileSize();
    }

    /**
     * Get the time of last modification at the time when the file was read.
     *
     * @return the time of last modification at the time when the file was read
     */
    public long getModifiedTime() {
        return file.getModifiedTime();
    }

    /**
     * Get the line number within the line.
     *
     * @return the line number within the line
     */
    public int getLineNumber() {
        return line.getLineNumber();
    }

}
