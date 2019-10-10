package com.github.codebje.ui;

import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.awt.*;

/**
 * Highlight full-width lines.
 */
public class FullWidthHighlight implements Highlighter.HighlightPainter {
    @Override
    public void paint(Graphics g, int p0, int p1, Shape bounds, JTextComponent c) {

        Rectangle r0, r1, rb = bounds.getBounds();

        try {
            r0 = c.modelToView(p0);
            r1 = c.modelToView(p1);
        } catch (BadLocationException ex) {
            return;
        }

        g.setColor(Color.lightGray);

        int minY = (int)Math.round(Math.min(r0.getY(), r1.getY()));
        int r0maxY = (int)Math.round(r0.getY() + r0.getHeight());
        int r1maxY = (int)Math.round(r1.getY() + r1.getHeight());
        int height = (int)Math.round(Math.max(r0maxY, r1maxY)) - minY;

        g.setColor(Color.lightGray);
        g.fillRect(0, minY, rb.width, height);

    }
}
