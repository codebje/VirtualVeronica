package com.github.codebje.ui;

import com.github.codebje.devices.DeviceChangeListener;
import com.github.codebje.devices.VideoDevice;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyListener;

/**
 * A VideoDevice output panel
 */
public class VideoPanel extends JPanel implements DeviceChangeListener {

    private final int scaleX, scaleY;

    private Dimension dimensions;
    private VideoDevice device;

    private KeyListener keyListener;

    public VideoPanel(VideoDevice device, int scaleX, int scaleY)
    {
        device.registerListener(this);

        this.device = device;
        this.scaleX = scaleX;
        this.scaleY = scaleY;

        this.dimensions = new Dimension(
                this.device.getImage().getWidth(null) * scaleX,
                this.device.getImage().getHeight(null) * scaleY);
    }

    public void setKeyListener(KeyListener keyListener) {
        if (this.keyListener != null) {
            removeKeyListener(this.keyListener);
        }
        this.keyListener = keyListener;
        if (this.keyListener != null) {
            addKeyListener(this.keyListener);
            setFocusable(true);
            requestFocus();
        }
    }

    /**
     * Called by the video device on state change.
     */
    public void deviceStateChanged() {

        Dimension dim = new Dimension(
                this.device.getImage().getWidth(null) * scaleX,
                this.device.getImage().getHeight(null) * scaleY);

        if (!dim.equals(dimensions)) {
            dimensions = dim;
            invalidate();
        }
        repaint();

    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.scale(scaleX, scaleY);
        g2d.drawImage(device.getImage(), 0, 0, null);
    }

    @Override
    public Dimension getMinimumSize() {
        return dimensions;
    }

    @Override
    public Dimension getPreferredSize() {
        return dimensions;
    }

}
