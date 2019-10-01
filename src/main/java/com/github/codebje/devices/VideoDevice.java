package com.github.codebje.devices;

import com.github.codebje.exceptions.MemoryRangeException;

import java.awt.*;

/**
 * Devices rendering to the VideoWindow must extend this ABC.
 */
public abstract class VideoDevice extends Device {

    VideoDevice(int startAddress, int endAddress, String name) throws MemoryRangeException {
        super(startAddress, endAddress, name);
    }

    // The Image backs the video display
    public abstract Image getImage();
}
