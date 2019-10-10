package com.github.codebje.cc65debug;

/**
 * A CC65 debug info source file.
 *
 * {@link DebugModule}s are generated from source files. Since some source files are used several
 * times when generating a list of modules (header files for example), the linker will merge
 * duplicates to reduce redundant information. Source files are considered identical if the full
 * name including the path is identical, and the size and time of last modification matches. Please
 * note that there may be still duplicates if files are accessed using different paths.
 *
 * It is suggested that a debugger might use the path of the file to locate it on disk, and the
 * size and time of modification to check if the file has been modified. Showing a warning to the
 * user in the later case might prevent dumb errors like debugging an executable using wrong
 * versions of the sources.
 *
 */
public class SourceFile {
    private final String fileName;
    private final long fileSize;
    private final long modifiedTime;

    SourceFile(String name, long size, long mtime) {
        this.fileName = name;
        this.fileSize = size;
        this.modifiedTime = mtime;
    }

    /**
     * Get the name of the source file including the path.
     *
     * @return the name of the source file including the path
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Get the size of the file at the time when it was read.
     *
     * @return the size of the file at the time when it was read
     */
    public long getFileSize() {
        return fileSize;
    }

    /**
     * Get the time of last modification at the time when the file was read.
     *
     * @return the time of last modification at the time when the file was read
     */
    public long getModifiedTime() {
        return modifiedTime;
    }

}
