package com.github.codebje.ui;

import com.github.codebje.cc65debug.DebugInfo;
import com.github.codebje.cc65debug.SourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This Frame displays the source associated with the Program Counter's value, if available.
 *
 */
public class SourceWindow extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(SourceWindow.class);

    private final DebugInfo debugInfo;
    private final Map<String, SourceFile> sourceFiles;

    private final JTextArea textArea;

    /**
     * Initialise a SourceWindow with debug information.
     *
     * @param debugInfo - the debug information to use
     */
    public SourceWindow(DebugInfo debugInfo) {
        this.debugInfo = debugInfo;

        sourceFiles = Stream.of(debugInfo.getSourceFiles())
                .collect(Collectors.toMap(com.github.codebje.cc65debug.SourceFile::getFileName,
                        s -> new SourceFile(s.getFileName(), s.getFileSize(), s.getModifiedTime())));

        setTitle("Source Code");
        setLayout(new BorderLayout());

        JPanel contentPanel = new JPanel();
        contentPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        contentPanel.setLayout(new BorderLayout());

        // Create a text area that can show 24 rows and 80 columns, no scroll bars
        textArea = new JTextArea("\n\n\n\n\n\n\n\n\n\n\nNo source file", 23, 80);
        textArea.setEditable(false);

        contentPanel.add(textArea);
        getContentPane().add(contentPanel);
        pack();
    }

    /**
     * Set the current program counter value.
     *
     * This will find the matching source file, load it if it's not already available, then
     * highlight and scroll to the relevant line.
     *
     * @param address - the program counter
     */
    public void setProgramCounter(int address) {

        SourceLocation loc = debugInfo.getSourceLocation(address);

        if (loc == null) {

            textArea.setSelectionStart(0);
            textArea.setSelectionEnd(0);
            setTitle("Source Code");

        } else {

            SourceFile source = sourceFiles.get(loc.getFileName());
            textArea.setText(source.getLinesAround(loc.getLineNumber()));
            textArea.getHighlighter().removeAllHighlights();
            setTitle("Source Code - " + loc.getFileName());

            try {

                int startIndex = textArea.getLineStartOffset(11);
                int endIndex = textArea.getLineEndOffset(11);
                Highlighter.HighlightPainter painter = new FullWidthHighlight();
                textArea.getHighlighter().addHighlight(startIndex, endIndex - 1, painter);

            } catch (BadLocationException ex) {
                // oh well, sucks to be us?
            }

        }
    }


    private class SourceFile {
        private final String[] lines;

        SourceFile(String name, long expectedSize, long expectedMTime) {

            File file = new File(name);

            if (!file.canRead()) {

                lines = new String[] { "Cannot read source file " + name };

            } else {
                if (file.lastModified()/1000 != expectedMTime || file.length() != expectedSize) {

                    lines = new String[] { "Source file " + name + " has been modified since build" };

                } else {

                    String[] temp;

                    try {

                        temp = Files.lines(file.toPath()).toArray(String[]::new);

                    } catch (IOException ex) {

                        logger.error("Error reading source file ", name, ex);
                        temp = new String[] { "Unable to read source file " + name + ": " + ex.getMessage() };

                    }

                    lines = temp;

                }
            }
        }

        /* Get a set of 24 lines around the selected line number */
        String getLinesAround(int line) {

            if (line >= lines.length) line = lines.length - 1;

            StringBuilder builder = new StringBuilder();

            for (int i = -12; i < 11; i++) {

                if (line + i >= 0 || line + i < lines.length)
                    builder.append(lines[line + i]);

                builder.append("\n");

            }

            return builder.toString();

        }

    }
}
