package com.github.codebje.devices;

import com.github.codebje.exceptions.MemoryAccessException;
import com.github.codebje.exceptions.MemoryRangeException;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * The Veronica custom GPU.
 */
public class VeronicaGPU extends VideoDevice {

    private static final int DEVICE_ADDRESS = 0xefff;
    private static final int IMAGE_WIDTH = 256;
    private static final int IMAGE_HEIGHT = 240;

    private static final byte TEXT_MAX_X = 64;
    private static final byte TEXT_MAX_Y = 30;

    private static final byte CLEARSCR = 0x01;
    private static final byte PLOTCHAR = 0x02;
    private static final byte PLOTSTR = 0x03;
    private static final byte FONTFGCLR = 0x04;
    private static final byte FONTBGCLR = 0x05;
    private static final byte CURSORXPOS = 0x06;
    private static final byte CURSORYPOS = 0x07;

    // VGA signal is 25.175MHz clock, 800 clocks per line, 524 lines per frame
    // for a total rate of a hair over 60Hz per frame. This constant computes
    // the number of nanoseconds in one frame of video. For 640x480 mode, there
    // are 800 pixel clocks per horizontal lines, and 524 lines per frame.
    private static final long NANOS_PER_FRAME
            = 800 * 524 * (1000000000 / 25175000);

    // The vertical blanking time is 11 lines of front porch, 2 lines of sync pulse,
    // and 31 lines of back porch, for 44 lines total.
    private static final long NANOS_PER_VBLANK
            = 800 * 44 * (1000000000 / 25175000);

    private byte commandByte = 0;
    private byte fontFgClr = 0x16;
    private byte fontBgClr = 0x16;
    private byte cursorXpos = 0;
    private byte cursorYpos = 0;

    private final BufferedImage image;

    public VeronicaGPU() throws MemoryRangeException, MemoryAccessException {
        super(DEVICE_ADDRESS, DEVICE_ADDRESS, "GPU");

        // Construct the color model
        byte[] r = new byte[64];
        byte[] g = new byte[64];
        byte[] b = new byte[64];
        for (int i = 0; i < 64; i++) {
            r[i] = (byte)(((i) & 0x03) * 255 / 3);
            g[i] = (byte)(((i >> 2) & 0x03) * 255 / 3);
            b[i] = (byte)(((i >> 4) & 0x03) * 255 / 3);
        }
        IndexColorModel model = new IndexColorModel(6, 64, r, g, b);

        image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_BYTE_INDEXED,
                model);

        reset();
    }

    @Override
    public void reset() throws MemoryAccessException {
        // load the boot logo
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.white);
        g2d.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        g2d.dispose();

        cursorXpos = cursorYpos = 0;
        fontBgClr = 0x3f;
        fontFgClr = 0x00;

        try {
            byte[] logo = loadBootLogo("/veronica-boot-logo.img");
            image.getRaster().setDataElements(28, 88, 200, 64, logo);
        } catch (IOException ex) {
            // no logo!
        }

        notifyListeners();
    }

    private void plotCharacter(int character) {
        // bounds check
        if (character > '~') return;

        int fg = image.getColorModel().getRGB(fontFgClr);
        int bg = image.getColorModel().getRGB(fontBgClr);

        int y = cursorYpos * 8;
        for (int row = 0; row < 4; row++) {
            int x = cursorXpos * 4;
            int pixels = FONT_DATA[character * 4 + row];
            // first scanline
            for (int bit = 7; bit > 3; bit--) {
                image.setRGB(x++, y, ((pixels >> bit) & 1) == 1 ? fg : bg);
            }
            x = cursorXpos * 4;
            y++;
            // second scanline
            for (int bit = 3; bit >= 0; bit--) {
                image.setRGB(x++, y, ((pixels >> bit) & 1) == 1 ? fg : bg);
            }
            y++;
        }
    }

    @Override
    public void write(int address, int data) throws MemoryAccessException {

        // GPU commands are a two-byte packet, command then argument
        switch (commandByte) {
            case CLEARSCR:
                Graphics2D g2d = image.createGraphics();
                g2d.setColor(new Color(image.getColorModel().getRGB(data)));
                g2d.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
                g2d.dispose();
                break;
            case PLOTCHAR:
                plotCharacter(data);
                break;
            case PLOTSTR:
                if (data == '\n') {
                    cursorXpos = 0;
                    cursorYpos = (byte)((cursorYpos + 1) % TEXT_MAX_Y);
                } else {
                    plotCharacter(data);
                    if (++cursorXpos >= TEXT_MAX_X) {
                        cursorXpos = 0;
                        if (++cursorYpos >= TEXT_MAX_Y) {
                            cursorYpos = 0;
                        }
                    }
                }
                break;
            case FONTFGCLR:
                fontFgClr = (byte)data;
                break;
            case FONTBGCLR:
                fontBgClr = (byte)data;
                break;
            case CURSORXPOS:
                cursorXpos = (byte)data;
                break;
            case CURSORYPOS:
                cursorYpos = (byte)data;
                break;

        }

        commandByte = (commandByte == 0) ? (byte)data : 0;

        notifyListeners();
    }

    @Override
    public int read(int address, boolean cpuAccess) throws MemoryAccessException {
        long frameTime = System.nanoTime() % NANOS_PER_FRAME;

        // 0xff in VBL period, 0x00 outside of it.
        return (frameTime > NANOS_PER_VBLANK) ? 0x00 : 0xff;
    }

    @Override
    public Image getImage() {
        return image;
    }

    @Override
    public String toString() {
        return "GPU";
    }

    private byte[] loadBootLogo(String resource) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(this.getClass().getResourceAsStream(resource))) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            while (bis.available() > 0) {
                bos.write(bis.read());
            }
            bos.flush();
            bos.close();

            return bos.toByteArray();
        }
    }


    private static final int[] FONT_DATA = {
            // Font data from https://robey.lag.net/2010/01/23/tiny-monospace-font.html
            0x00,0xea,0xaa,0xe0,        // character 0
            0x00,0xea,0xaa,0xe0,        // character 1
            0x00,0xea,0xaa,0xe0,        // character 2
            0x00,0xea,0xaa,0xe0,        // character 3
            0x00,0xea,0xaa,0xe0,        // character 4
            0x00,0xea,0xaa,0xe0,        // character 5
            0x00,0xea,0xaa,0xe0,        // character 6
            0x00,0xea,0xaa,0xe0,        // character 7
            0x00,0xea,0xaa,0xe0,        // character 8
            0x00,0xea,0xaa,0xe0,        // character 9
            0x00,0xea,0xaa,0xe0,        // character 10
            0x00,0xea,0xaa,0xe0,        // character 11
            0x00,0xea,0xaa,0xe0,        // character 12
            0x00,0xea,0xaa,0xe0,        // character 13
            0x00,0xea,0xaa,0xe0,        // character 14
            0x00,0xea,0xaa,0xe0,        // character 15
            0x00,0xea,0xaa,0xe0,        // character 16
            0x00,0xea,0xaa,0xe0,        // character 17
            0x00,0xea,0xaa,0xe0,        // character 18
            0x00,0xea,0xaa,0xe0,        // character 19
            0x00,0xea,0xaa,0xe0,        // character 20
            0x00,0xea,0xaa,0xe0,        // character 21
            0x00,0xea,0xaa,0xe0,        // character 22
            0x00,0xea,0xaa,0xe0,        // character 23
            0x00,0xea,0xaa,0xe0,        // character 24
            0x00,0xea,0xaa,0xe0,        // character 25
            0x00,0xea,0xaa,0xe0,        // character 26
            0x00,0xea,0xaa,0xe0,        // character 27
            0x00,0xea,0xaa,0xe0,        // character 28
            0x00,0xea,0xaa,0xe0,        // character 29
            0x00,0xea,0xaa,0xe0,        // character 30
            0x00,0xee,0xee,0xe0,        // character 31
            0x00,0x00,0x00,0x00,        // space
            0x00,0x44,0x40,0x40,        // !
            0x00,0xaa,0x00,0x00,        // "
            0x00,0xae,0xae,0xa0,        // #
            0x00,0x6c,0x6c,0x40,        // $
            0x00,0x82,0x48,0x20,        // %
            0x00,0xcc,0xea,0x60,        // &
            0x00,0x44,0x00,0x00,        // '
            0x00,0x24,0x44,0x20,        // (
            0x00,0x84,0x44,0x80,        // )
            0x00,0x0a,0x4a,0x00,        // *
            0x00,0x04,0xe4,0x00,        // +
            0x00,0x00,0x04,0x80,        // ,
            0x00,0x00,0xe0,0x00,        // -
            0x00,0x00,0x00,0x80,        // .
            0x00,0x22,0x48,0x80,        // /
            0x00,0x6a,0xaa,0xc0,        // 0
            0x00,0x4c,0x44,0x40,        // 1
            0x00,0xc2,0x48,0xe0,        // 2
            0x00,0xc2,0x42,0xc0,        // 3
            0x00,0xaa,0xe2,0x20,        // 4
            0x00,0xe8,0xc2,0xc0,        // 5
            0x00,0x68,0xea,0xe0,        // 6
            0x00,0xe2,0x48,0x80,        // 7
            0x00,0xea,0xea,0xe0,        // 8
            0x00,0xea,0xe2,0xc0,        // 9
            0x00,0x04,0x04,0x00,        // :
            0x00,0x04,0x04,0x80,        // ;
            0x00,0x24,0x84,0x20,        // <
            0x00,0x0e,0x0e,0x00,        // =
            0x00,0x84,0x24,0x80,        // >
            0x00,0xe2,0x40,0x40,        // ?
            0x00,0x4a,0xe8,0x60,        // @

            // Fonts for A-Z by Quinn Dunki
            /*
             * font.h
             *
             * This is the font for text rendering. Each character is
             * 4x8 pixels. The top-most two rows and right-most column are
             * generally blank for readability. Each byte is two rows,
             * with 1-bit per pixel. The color of the characters is
             * determined by a GPU state which can be set by rendering
             * command.
             *
             * (c)2013 Quinn Dunki. All rights reserved.
             *
             * http://www.quinndunki.com
             * http://www.quinndunki.com/blondihacks
             *
             * You're free to use and distribute this code for noncommercial purposes,
             * but please leave this accreditation intact.
             *
             * Transcribed to a Java constant by Byron Ellacott. int is used to avoid
             * sign issues.
             */
            0x00,0x4a,0xea,0xa0,        // A
            0x00,0xca,0xca,0xc0,        // B
            0x00,0x68,0x88,0x60,        // C
            0x00,0xca,0xaa,0xc0,        // D
            0x00,0xe8,0xc8,0xe0,        // E
            0x00,0xe8,0xc8,0x80,        // F
            0x00,0xe8,0xaa,0xe0,        // G
            0x00,0xaa,0xea,0xa0,        // H
            0x00,0xe4,0x44,0xe0,        // I
            0x00,0x22,0x2a,0x40,        // J error (fixed)
            0x00,0xaa,0xca,0xa0,        // K
            0x00,0x88,0x88,0xe0,        // L
            0x00,0xae,0xea,0xa0,        // M
            0x00,0xae,0xea,0xa0,        // N
            0x00,0x4a,0xaa,0x40,        // O
            0x00,0xca,0xc8,0x80,        // P
            0x00,0x4a,0xae,0xe0,        // Q
            0x00,0xca,0xca,0xa0,        // R
            0x00,0xe8,0xe2,0xe0,        // S
            0x00,0xe4,0x44,0x40,        // T
            0x00,0xaa,0xaa,0xe0,        // U
            0x00,0xaa,0xaa,0x40,        // V
            0x00,0xaa,0xee,0xa0,        // W
            0x00,0xaa,0x4a,0xa0,        // X
            0x00,0xaa,0x44,0x40,        // Y
            0x00,0xe2,0x48,0xe0,        // Z

            // Font data from https://robey.lag.net/2010/01/23/tiny-monospace-font.html
            0x00,0xe8,0x88,0xe0,        // [
            0x00,0x08,0x42,0x00,        // \
            0x00,0xe2,0x22,0xe0,        // ]
            0x00,0x4a,0x00,0x00,        // ^
            0x00,0x00,0x00,0xe0,        // _
            0x00,0x84,0x00,0x00,        // `
            0x00,0x0c,0x6a,0xe0,        // a
            0x00,0x8c,0xaa,0xc0,        // b
            0x00,0x06,0x88,0x60,        // c
            0x00,0x26,0xaa,0x60,        // d
            0x00,0x06,0xac,0x60,        // e
            0x00,0x24,0xe4,0x40,        // f
            0x00,0x06,0xae,0x24,        // g
            0x00,0x8c,0xaa,0xa0,        // h
            0x00,0x40,0x44,0x40,        // i
            0x00,0x20,0x22,0xa4,        // j
            0x00,0x8a,0xcc,0xa0,        // k
            0x00,0xc4,0x44,0xe0,        // l
            0x00,0x0e,0xee,0xa0,        // m
            0x00,0x0c,0xaa,0xa0,        // n
            0x00,0x04,0xaa,0x40,        // o
            0x00,0x0c,0xaa,0xc8,        // p
            0x00,0x06,0xaa,0x62,        // q
            0x00,0x06,0x88,0x80,        // r
            0x00,0x06,0xc6,0xc0,        // s
            0x00,0x4e,0x44,0x60,        // t
            0x00,0x0a,0xaa,0x60,        // u
            0x00,0x0a,0xae,0x40,        // v
            0x00,0x0a,0xee,0xe0,        // w
            0x00,0x0a,0x44,0xa0,        // x
            0x00,0x0a,0xa6,0x24,        // y
            0x00,0x0e,0x6c,0xe0,        // z
            0x00,0x64,0x84,0x60,        // {
            0x00,0x44,0x04,0x40,        // |
            0x00,0xc4,0x24,0xc0,        // }
            0x00,0x6c,0x00,0x00,        // ~
            0x00,0xee,0xee,0xe0,        // solid block
    };

}