/*
 * Copyright (c) 2016 Seth J. Morabito <web@loomcom.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.github.codebje.devices;

import com.github.codebje.exceptions.MemoryAccessException;
import com.github.codebje.exceptions.MemoryRangeException;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Deque;
import java.util.EnumMap;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Very basic implementation of a MOS 6522 VIA.
 *
 * TODO: Implement timers as threads.
 */
public class Via6522 extends Pia implements KeyListener {
    private static final Logger logger = Logger.getLogger(Via6522.class.getName());

    private static final int VIA_SIZE = 16;
    private static final int IRQ_RATE = 15;  // ms

    enum Register {
        ORB, ORA, DDRB, DDRA, T1C_L, T1C_H, T1L_L, T1L_H,
        T2C_L, T2C_H, SR, ACR, PCR, IFR, IER, ORA_H
    }

    // The register map stores a value for each register.
    private final EnumMap<Register,Byte> registerMap;

    // Received keycodes are sent to the CPU at a rate slow enough that it has a chance to
    // process each IRQ, via a queue. As this emulates a PS2 keyboard, each released key is
    // sent as two codes - 0xF0 first, then the released key's code.
    private final ScheduledExecutorService scheduler;
    private final Deque<Byte> codeQueue = new ConcurrentLinkedDeque<>();

    public Via6522(int address) throws MemoryRangeException {
        super(address, address + VIA_SIZE - 1, "MOS 6522 VIA");
        registerMap = new EnumMap<>(Register.class);
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            Byte code;
            if ((code = codeQueue.poll()) != null) {
                registerMap.put(Register.IFR,
                        (byte) (registerMap.getOrDefault(Register.IFR, (byte) 0) | 0x02));
                registerMap.put(Register.ORA, code);
                getBus().assertIrq();

            }
        }, IRQ_RATE, IRQ_RATE, TimeUnit.MILLISECONDS);
    }

    @Override
    public void write(int address, int data) throws MemoryAccessException {
        Register[] registers = Register.values();

        if (address >= registers.length) {
            throw new MemoryAccessException("Unknown register: " + address);
        }

        Register r = registers[address];

        switch (r) {
            case ORA:
            case ORB:
            case DDRA:
            case DDRB:
            case T1C_L:
            case T1C_H:
            case T1L_L:
            case T1L_H:
            case T2C_L:
            case T2C_H:
            case SR:
            case ACR:
            case PCR:
            case IFR:
            case IER:
            case ORA_H:
                registerMap.put(r, (byte)data);
                break;
            default:
        }
    }

    @Override
    public int read(int address, boolean cpuAccess) throws MemoryAccessException {
        Register[] registers = Register.values();

        if (address >= registers.length) {
            throw new MemoryAccessException("Unknown register: " + address);
        }

        Register r = registers[address];
        switch (r) {
            case ORA:
            case ORB:
            case DDRA:
            case DDRB:
            case T1C_L:
            case T1C_H:
            case T1L_L:
            case T1L_H:
            case T2C_L:
            case T2C_H:
            case SR:
            case ACR:
            case PCR:
            case IFR:
            case IER:
            case ORA_H:
                byte value = registerMap.getOrDefault(r, (byte)0);
                return (int)value;
            default:
        }

        return 0;
    }

    @Override
    public void reset() throws MemoryAccessException {
        registerMap.clear();
        codeQueue.clear();
    }

    public void keyPressed(KeyEvent keyEvent) {
        if (keyEvent.getKeyCode() < 256) {
            codeQueue.add((byte)keyEvent.getKeyCode());
        }
        keyEvent.consume();
    }

    public void keyReleased(KeyEvent keyEvent) {
        if (keyEvent.getKeyCode() < 256) {
            codeQueue.add((byte)0xf0);
            codeQueue.add((byte)keyEvent.getKeyCode());
        }
        keyEvent.consume();
    }

    public void keyTyped(KeyEvent keyEvent) {
        // Veronica will work this out for herself.
        keyEvent.consume();
    }

}
