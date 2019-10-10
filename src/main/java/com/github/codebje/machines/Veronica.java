package com.github.codebje.machines;

import com.github.codebje.Bus;
import com.github.codebje.devices.*;
import com.github.codebje.exceptions.MemoryAccessException;
import com.github.codebje.Cpu;
import com.github.codebje.InstructionTable;
import com.github.codebje.exceptions.MemoryRangeException;

import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;

/**
 * The Veronica implements blondihack's 6502-based homebrew machine.
 */
public class Veronica implements Machine {

    private static final int BUS_BOTTOM = 0x0000;
    private static final int BUS_TOP    = 0xffff;
    private static final int ROM_BASE   = 0xf000;
    private static final int ROM_TOP    = 0xffff;
    private static final int VIA_BASE   = 0xe000;

    private final Bus bus;
    private final Memory ram;
    private final Cpu cpu;
    private final VeronicaGPU gpu;
    private final Via6522 via;

    private Memory rom;

    public Veronica() throws MemoryRangeException, MemoryAccessException, IOException {
        this.bus = new Bus(BUS_BOTTOM, BUS_TOP);
        this.ram = new Memory(BUS_BOTTOM, BUS_TOP, false);
        this.cpu = new Cpu(InstructionTable.CpuBehavior.CMOS_6502);
        this.gpu = new VeronicaGPU();
        this.via = new Via6522(VIA_BASE);
        this.rom = null;

        bus.addCpu(cpu);
        bus.addDevice(ram, 0);  // RAM at lower priority
        bus.addDevice(gpu, 1);
        bus.addDevice(via, 1);

    }

    @Override
    public File getDefaultRomFile() {
        return new File("veronica.rom");
    }

    @Override
    public Bus getBus() {
        return bus;
    }

    @Override
    public Cpu getCpu() {
        return cpu;
    }

    @Override
    public Memory getRam() {
        return ram;
    }

    @Override
    public Pia getPia() {
        return via;
    }

    @Override
    public VideoDevice getGPU() {
        return gpu;
    }

    @Override
    public KeyListener getIO() {
        return via;
    }

    @Override
    public Memory getRom() {
        return rom;
    }

    @Override
    public void setRom(Memory rom) throws MemoryRangeException {
        if (this.rom != null) {
            this.bus.removeDevice(this.rom);
        }
        this.rom = rom;
        this.bus.addDevice(this.rom, 1);
    }

    @Override
    public int getRomBase() {
        return ROM_BASE;
    }

    @Override
    public int getRomSize() {
        return ROM_TOP - ROM_BASE + 1;
    }

    @Override
    public void reset() throws MemoryAccessException {
        cpu.reset();
        gpu.reset();
        via.reset();
    }

    @Override
    public int getMemorySize() {
        return BUS_TOP + 1;
    }

    @Override
    public String getName() {
        return "Veronica";
    }

}
