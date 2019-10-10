package com.github.codebje.cc65debug;

import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.EnumMap;

/**
 * CC65 debug info: maps memory locations to files, lines, segments, etc
 */
public class DebugInfo {
    public final static int CC65_INV_ID = -1;

    private final RangeMap<Integer, SourceLocation> memoryMap;
    private final SourceFile[] sourceFiles;

    DebugInfo(RangeMap<Integer, SourceLocation> memoryMap, SourceFile[] sourceFiles) {
        this.memoryMap = memoryMap;
        this.sourceFiles = sourceFiles;
    }

    /**
     * Load debug information from a file.
     *
     * @param debugFile the source file to read
     * @return the debug information obtained
     * @throws IOException if the file cannot be read
     * @throws ParseException if the file cannot be parsed correctly
     */
    public static DebugInfo loadDebugFile(File debugFile) throws IOException, ParseException {

        try (BufferedReader rdr = new BufferedReader(new FileReader(debugFile))) {

            checkVersion(rdr.readLine());
            EnumMap<Keyword, Value> info = parseLine(rdr.readLine());
            if (!info.get(Keyword.LINETYPE).getString().equals("info"))
                throw new ParseException("expecting info, got " + info.get(Keyword.LINETYPE).getString());

            int     file = info.get(Keyword.FILE).getInteger(),
                    line = info.get(Keyword.LINE).getInteger(),
                    seg  = info.get(Keyword.SEGMENT).getInteger(),
                    span = info.get(Keyword.SPAN).getInteger();

            SourceFile[] files = new SourceFile[file];
            DebugLine[] lines = new DebugLine[line];
            DebugSegment[] segs = new DebugSegment[seg];
            DebugSpan[] spans = new DebugSpan[span];

            String record;
            while ((record = rdr.readLine()) != null) {

                EnumMap<Keyword, Value> map = parseLine(record);
                switch (map.get(Keyword.LINETYPE).getString()) {

                    case "file":
                        insertFile(files, map);
                        break;
                    case "line":
                        insertLine(lines, map);
                        break;
                    case "mod":
                        break;
                    case "seg":
                        insertSegment(segs, map);
                        break;
                    case "span":
                        insertSpan(spans, map);
                        break;
                    case "scope":
                        break;
                    case "sym":
                        break;
                    case "type":
                        break;
                    default:
                        throw new ParseException("unknown or unsupported debug information type "
                                + map.get(Keyword.LINETYPE).getString());

                }
            }

            /* Construct the range map */
            ImmutableRangeMap.Builder<Integer, SourceLocation> builder = new ImmutableRangeMap.Builder<>();

            for (int i = 0; i < line; i++) {

                DebugLine l = lines[i];
                if (l == null) throw new ParseException("Missing line with ID " + i);

                // maybe need a macro overlay? for now just discard them..
                if (l.getType() == DebugLine.TYPE_MACRO) continue;

                SourceFile f = files[l.getFileId()];
                if (f == null) throw new ParseException("Missing file with ID " + l.getFileId());

                SourceLocation loc = new SourceLocation(f, l);

                for (int sId : l.getSpans()) {

                    DebugSpan s = spans[sId];
                    if (s == null) throw new ParseException("Missing span with ID " + sId);

                    DebugSegment segment = segs[s.getSegmentId()];
                    if (segment == null) throw new ParseException("Missing segment with ID " + s.getSegmentId());

                    try {
                        int startAddress = segment.getStartAddress() + s.getStartAddress();
                        int endAddress = startAddress + s.getSize();
                        builder.put(Range.closedOpen(startAddress, endAddress), loc);
                    } catch (IllegalArgumentException ex) {
                        throw new ParseException("Overlapping spans detected on line " + i);
                    }

                }
            }

            return new DebugInfo(builder.build(), files);

        }

    }

    /**
     * Get the source location corresponding to a memory address.
     *
     * @param address the 64k memory address to look up
     * @return the source location if one is known, else null
     */
    public SourceLocation getSourceLocation(int address) {
        return memoryMap.get(address);
    }

    /**
     * Get the list of source files.
     *
     * @return the list of source files
     */
    public SourceFile[] getSourceFiles() {
        return sourceFiles;
    }

    private static void insertFile(SourceFile[] files, EnumMap<Keyword, Value> map) {
        int id = map.get(Keyword.ID).getInteger();
        files[id] = new SourceFile(
                map.get(Keyword.NAME).getString(),
                map.get(Keyword.SIZE).getInteger(),
                map.get(Keyword.MTIME).getInteger()
        );
    }

    private static void insertLine(DebugLine[] lines, EnumMap<Keyword, Value> map) {
        int id = map.get(Keyword.ID).getInteger();
        lines[id] = new DebugLine(
                map.get(Keyword.FILE).getInteger(),
                map.get(Keyword.LINE).getInteger(),
                map.getOrDefault(Keyword.SPAN, new Value("")).getArray(),
                map.getOrDefault(Keyword.TYPE, new Value("-1")).getInteger(),
                map.getOrDefault(Keyword.COUNT, new Value("0")).getInteger()
        );
    }

    private static void insertSegment(DebugSegment[] segments, EnumMap<Keyword, Value> map) {
        int id = map.get(Keyword.ID).getInteger();
        segments[id] = new DebugSegment(
                map.get(Keyword.NAME).getString(),
                map.get(Keyword.START).getInteger(),
                map.get(Keyword.SIZE).getInteger(),
                map.getOrDefault(Keyword.OUTPUTNAME, new Value("")).getString(),
                map.getOrDefault(Keyword.OFFS, new Value("-1")).getInteger()
        );
    }

    private static void insertSpan(DebugSpan[] spans, EnumMap<Keyword, Value> map) {
        int id = map.get(Keyword.ID).getInteger();
        spans[id] = new DebugSpan(
                map.get(Keyword.START).getInteger(),
                map.get(Keyword.SIZE).getInteger(),
                map.get(Keyword.SEGMENT).getInteger(),
                map.getOrDefault(Keyword.TYPE, new Value("-1")).getInteger()
        );
    }

    private static void checkVersion(String source) throws ParseException {
        if (source == null)
            throw new ParseException("Missing version line");
        if (!"version\tmajor=2,minor=0".equals(source))
            throw new ParseException("Invalid version line " + source);
    }

    static EnumMap<Keyword,Value> parseLine(String line) throws ParseException {
        final EnumMap<Keyword,Value> result = new EnumMap<>(Keyword.class);

        // Extract line type first
        String[] parts = line.split("\t", 2);
        if (parts.length != 2)
            throw new ParseException("Missing line type " + line);
        result.put(Keyword.LINETYPE, new Value(parts[0]));
        line = parts[1];

        while (!line.isEmpty()) {
            parts = line.split("=", 2);
            if (parts.length != 2)
                throw new ParseException("missing value in " + line);
            Keyword kwd = Keyword.findKeyword(parts[0]);
            if (kwd ==  null)
                throw new ParseException("unknown keyword " + parts[0]);

            // if parts[1] begins with a double quote, hunt for its match, else consume up to comma
            String[] remainder;
            if (parts[1].startsWith("\"")) {
                remainder = parts[1].substring(1).split("\"", 2);
                if (remainder[1].length() > 0) {
                    if (!remainder[1].startsWith(","))
                        throw new ParseException("Expecting COMMA in " + remainder[1]);
                    remainder[1] = remainder[1].substring(1);
                }
            } else {
                remainder = parts[1].split(",", 2);
            }
            result.put(kwd, new Value(remainder[0]));
            line = remainder.length == 2 ? remainder[1] : "";
        }

        return result;
    }
}
