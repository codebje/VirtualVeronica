package com.loomcom.symon;

import com.loomcom.symon.devices.Memory;
import com.loomcom.symon.exceptions.MemoryRangeException;
import junit.framework.TestCase;

public class CpuIndirectModeTest extends TestCase {

  protected Cpu cpu;
  protected Bus bus;
  protected Memory mem;

  protected void setUp() throws Exception {
    this.cpu = new Cpu();
    this.bus = new Bus(0x0000, 0xffff);
    this.mem = new Memory(0x0000, 0x10000);
    bus.addCpu(cpu);
    bus.addDevice(mem);

    // Load the reset vector.
    bus.write(0xfffc, Cpu.DEFAULT_BASE_ADDRESS & 0x00ff);
    bus.write(0xfffd, (Cpu.DEFAULT_BASE_ADDRESS & 0xff00)>>>8);

    cpu.reset();
    // Assert initial state
    assertEquals(0, cpu.getAccumulator());
    assertEquals(0, cpu.getXRegister());
    assertEquals(0, cpu.getYRegister());
    assertEquals(0x200, cpu.getProgramCounter());
    assertEquals(0xff, cpu.getStackPointer());
    assertEquals(0x20, cpu.getProcessorStatus());
  }

  /*
   * The following opcodes are tested for correctness in this file:
   *
   * JMP - $6c
   *
   */

  /* JMP - Jump - $6c */

  public void test_JMP() {
    bus.write(0x3400, 0x00);
    bus.write(0x3401, 0x54);
    bus.loadProgram(0x6c, 0x00, 0x34);
    cpu.step();
    assertEquals(0x5400, cpu.getProgramCounter());
    // No change to status flags.
    assertEquals(0x20, cpu.getProcessorStatus());
  }

}