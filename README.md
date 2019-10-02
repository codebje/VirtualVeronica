Virtual Veronica - A 6502 System Simulator
==========================================

**Version:** 1.0.0

See the file COPYING for license.

## About

The Virtual Veronica is a simulator for Quinn Dunki's homebrew 6502
based system, [Veronica](http://blondihacks.com/?page_id=1761). 

Currently, the simulator supports:

  - The 6502 processor (thanks to Symon)
  - Keyboard input with an emulated VIA6522
  - Video output using the classic Veronica GPU

## Credits

This software is very heavily based on the Symon 6502 simulator. More
has been removed than added or modified.

See [Symon's repository](https://github.com/sethm/symon) for full details.

See **Acknowledgements** for Symon's copyright and licensing information.

I have renamed the package and program, as the modifications made significantly
alter the available features and functionality - this is now a single machine simulator
for Veronica, with the removal of serial console devices, terminal emulation, and
the existing set of machines.

## Requirements

  - Java 1.8 or higher
  - Maven 2.0.x or higher (for building from source)
  - JUnit 4 or higher (for testing)

## Simulator features

The features of the simulator are largely courtesy of Symon.

### Veronica's Memory Map

The memory map in use is based on [this article](http://blondihacks.com/?p=1291) on Blondihacks:

  - `$0000 - $00FF` - 6502 zero page (RAM)
  - `$0100 - $01FF` - 6502 stack (RAM)
  - `$0200 - $DFFF` - program memory (RAM)
  - `$E000 - $E0FF` - VIA 6522 page (only first 16 bytes mapped)
  - `$E100 - $EFFE` - Reserved for IO and ROM-managed RAM
  - `$EFFF - $EFFF` - Veronica GPU IO register
  - `$F000 - $FFFF` - ROM

### ROM Loading

A new ROM image can be loaded via the "Load ROM..." action in the
"File" menu. The ROM for Veronica is 4K, and includes the vector table.
A file must be exactly 4K to be loaded.

### Memory Window

Memory contents can be viewed (and edited) one page at a time through the Memory Window.

This feature is unmodified from the Symon simulator.

### Trace Log

The last 20,000 execution steps are disassembled and logged to the Trace Log
Window.

This feature is unmodified from the Symon simulator.

### Breakpoints

Breakpoints can be set and removed through the Breakpoints window.

This feature is unmodified from the Symon simulator.

### Veronica GPU and I/O

The Veronica GPU as implemented in this simulator has the set of commands from the
complete code package available in the article [Veronica – GPU Recap](http://blondihacks.com/?p=1227).

| Command | Argument | Description |
| ------- | -------- | ----------- |
|   $01   | colour   | Clear screen to colour |
|   $02   | character | Plot character at cursor x, y |
|   $03   | character | Plot character and advance cursor |
|   $04   | colour   | Set font foreground colour |
|   $05   | colour   | Set font background colour |
|   $06   | coordinate | Set cursor X coordinate |
|   $07   | coordinate | Set cursor Y coordinate |

The simulated GPU's status register approximates a VGA video blanking signal using
Java's `System.nanoTime()`. The signal is noisy due to the JVM's operations: the simulated
CPU's clock isn't very accurate, so a busy loop looking for video sync and outputting
 status will see some variability.

It's highly likely the final Veronica GPU included more commands, sufficient to play Pong, but
the commands are not documented.

The Veronica 6522 IO system implements the keyboard protocol of Veronica,
sending a keycode on key down, and $F0 followed by the same keycode on key up.

However, the keycode map is defined by Java's KeyEvent, not any of the PS/2 keycode maps. This map
is much closer to ASCII than the PS/2 map used by Veronica, which makes some things easier…

The IFR register of the 6522 is correctly set on interrupt, but is currently never cleared.

Key interrupts are sent to the Veronica's CPU at a maximum rate of 100Hz. This introduces a little
keyboard lag, but prevents the CPU tripping up trying to read $F0 and the release keycode in very
quick succession.

## Usage

### Building the simulator

To build Virtual Veronica with Apache Maven, just type:

    $ mvn package

Maven will build Virtual Veronica, run unit tests, and produce a jar file in the
`target` directory containing the compiled simulator.

Virtual Veronica is meant to be invoked directly from the jar file. To run with
Java 1.8 or greater, just type:

    $ java -jar vveronica-1.0.0.jar

When Virtual Veronica is running, you should be presented with a simple graphical
interface.

### Building a ROM image

Included is a Makefile and machine configuration file for CC65 to produce a
ROM image. The only missing portion is the ROM source, which is currently not
freely distributable.

### ROM images

The simulator requires a ROM image loaded into memory to work
properly. Without a ROM in memory, the simulator will not be able to
reset, since the reset vector for the 6502 is located in the ROM
address space.

Virtual Veronica looks for a file named `veronica.rom` in the launch
directory at start-up. Alternatively, a ROM image can be loaded at
run-time, as described above.

### Loading a program

In addition to ROM images, programs in the form of raw binary object files can
be loaded directly into memory from "Load Program..." in the File menu.

Programs are loaded starting at address $0300.  After loading the program, the
simulated CPU's reset vector is loaded with the values $00, $03, and the CPU is
reset.

This feature is unmodified from the Symon simulator. It has not been
tested with Virtual Veronica as of version 1.0.0.

### Running

After loading a program or ROM image, clicking "Run" will start the simulator
running.

## Revision history

  - **1.0.0:** 2 October, 2019 - Virtual Veronica begins
  
### Symon revision history

  - **1.3.0:** 24 February, 2018 - Adds support for 65C02 opcodes.

  - **1.2.1:** 8 January, 2016 - Remove dependency on Java 8. Now
    supports compiling and running under Java 1.7.

  - **1.2.0:** 3 January, 2016 - Add symbolic disassembly to breakpoints
    window.

  - **1.1.1:** 2 January, 2016 - Minor enhancement: Allows breakpoints
    to be added with the Enter key.

  - **1.1.0:** 31 December, 2015 - Fixed delay loop to better
    simulate various clock speeds. Added ability to select clock
    speed at runtime. Status display now shows the next instruction
    to be executed, instead of the last instruction executed.
    Added support for breakpoints.

  - **1.0.0:** 10 August, 2014 - Added "Simple" machine
    implementation, pure RAM with no IO. Added Klaus Dormann's
    6502 Functional Tests for further machine verification (these
    tests must be run in the "Simple" machine).

  - **0.9.9.1:** 27 July, 2014 - Pressing 'Control' while clicking
    'Reset' now performs a memory clear.

  - **0.9.9:** 26 July, 2014 - MULTICOMP and multi-machine support
    contributed by Maik Merten &lt;maikmerten@googlemail.com&gt;

  - **0.9.1:** 26 January, 2014 - Support for IRQ and NMI handling.

  - **0.9.0:** 29 December, 2013 - First pass at a 6545 CRTC simulation.

  - **0.8.5:** 30 March, 2013 - ASCII display for memory window.
    Allows user to select a step count from a drop-down box.

  - **0.8.4:** 4 March, 2013 - Fix for ZPX, ZPY display in the trace log
    (change contributed by jsissom)

  - **0.8.3:** 12 January, 2013 - Added tool-tip text. Memory is no longer
    cleared when resetting. Fixed swapped register labels.

  - **0.8.2:** 01 January, 2013 - Fully passes Klaus Dormann's 6502 Functional Test suite!

  - **0.8.1:** 30 December, 2012

  - **0.8.0:** 29 December, 2012

  - **0.7.0:** 9 December, 2012

  - **0.6:** 5 November, 2012

  - **0.5:** 21 October, 2012 - Able to run Enhanced BASIC for the first time.

  - **0.3:** 14 October, 2012

  - **0.2:** 22 April, 2012

  - **0.1:** 20 January, 2010

## Acknowledgements

**Symon Copyright (c) 2014 Seth J. Morabito &lt;web@loomcom.com&gt;**

Portions Copyright (c) 2014 Maik Merten &lt;maikmerten@googlemail.com&gt;

Additional components used in this project are copyright their respective owners.

  - Enhanced 6502 BASIC Copyright (c) Lee Davison
  - 6502 Functional Tests Copyright (c) Klaus Dormann

This project would not have been possible without the following resources:

  - [Andrew Jacobs' 6502 Pages](http://www.obelisk.demon.co.uk/6502/), for
    wonderfully detailed information about the 6502

  - [Neil Parker's "The 6502/65C02/65C816 Instruction Set Decoded"](http://www.llx.com/~nparker/a2/opcodes.html),
    for information about how instructions are coded

## Licensing

Symon is free software.  It is distributed under the MIT License.
Please see the file 'COPYING' for full details of the license.

Virtual Veronica is under the same license.

Veronica's ROM is ALL RIGHTS RESERVED Quinn Dunki
and cannot be distributed in source or binary form.
