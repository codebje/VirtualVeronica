package com.github.codebje.cc65debug;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.Key;
import java.util.Arrays;
import java.util.EnumMap;

import static org.junit.Assert.*;

/**
 * Test debug info loader
 */
public class DebugInfoTest {

    @Test
    public void testDebugLoad() throws IOException, ParseException {
        DebugInfo info = DebugInfo.loadDebugFile(new File(getClass().getResource("/veronica.dbg").getFile()));
    }

    @Test
    public void testParseLine() throws ParseException {
        String line = "line\tid=141,name=\"RETURN_L\",addrsize=zeropage,scope=1,def=570,ref=701+736+497+645+543+557+697+647+565+689+482+531+575+712+610+691+613+580+592,val=0x5E,type=equ";
        Keyword keys[] = { Keyword.ID, Keyword.NAME, Keyword.ADDRSIZE, Keyword.SCOPE, Keyword.DEF,
                           Keyword.REF, Keyword.TYPE, Keyword.VALUE, Keyword.LINETYPE};

        EnumMap<Keyword, Value> map = DebugInfo.parseLine(line);
        assertEquals(9, map.size());

        // Check keys all found
        Arrays.sort(keys);
        Keyword[] foundKeys = map.keySet().toArray(new Keyword[] {});
        Arrays.sort(foundKeys);
        assertArrayEquals(keys, foundKeys);

        // Test values
        assertEquals("line", map.get(Keyword.LINETYPE).getString());
        assertEquals(141, map.get(Keyword.ID).getInteger());
        assertEquals("RETURN_L", map.get(Keyword.NAME).getString());
        assertEquals(AddressSize.ZEROPAGE, AddressSize.lookup(map.get(Keyword.ADDRSIZE).getString()));
        assertEquals(DebugScope.CC65_SCOPE_MODULE, map.get(Keyword.SCOPE).getInteger());
        assertEquals(570, map.get(Keyword.DEF).getInteger());
        assertArrayEquals(new int[] { 570 }, map.get(Keyword.DEF).getArray());
        assertArrayEquals(new int[] { 701, 736, 497, 645, 543, 557, 697, 647, 565, 689, 482, 531, 575, 712, 610, 691, 613, 580, 592 },
                map.get(Keyword.REF).getArray());
        assertEquals(0x5E, map.get(Keyword.VALUE).getInteger());
        assertEquals(SymbolType.EQUATE, SymbolType.typeForCode(map.get(Keyword.TYPE).getString()));

        // Confirm throws
        try {
            map.get(Keyword.NAME).getInteger();
            fail("Treat NAME string as integer");
        } catch (NumberFormatException ex) {
            // success
        }
    }


}