/*
 * Copyrighi (c) 2016 Seth J. Morabito <web@loomcom.com>
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

package com.github.codebje;

import com.github.codebje.exceptions.MemoryAccessException;
import com.github.codebje.exceptions.MemoryRangeException;
import com.github.codebje.exceptions.SymonException;
import com.github.codebje.ui.*;
import com.github.codebje.devices.Memory;
import com.github.codebje.machines.Machine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

/**
 * Symon Simulator Interface and Control.
 * <p/>
 * This class provides a control and I/O system for the simulated 6502 system.
 * It includes the simulated CPU itself, as well as 32KB of RAM, 16KB of ROM,
 * and a simulated ACIA for serial I/O. The ACIA is attached to a dumb terminal
 * with a basic 80x25 character display.
 */
public class Simulator {

    private final static Logger logger = LoggerFactory.getLogger(Simulator.class.getName());

    // UI constants
    private static final int DEFAULT_FONT_SIZE = 12;

    // Clock periods, in NS, for each speed. 0MHz, 1MHz, 2MHz, 3MHz, 4MHz, 5MHz, 6MHz, 7MHz, 8MHz.
    private static final long[] CLOCK_PERIODS = {0, 1000, 500, 333, 250, 200, 167, 143, 125};

    // Since it is very expensive to update the UI with Swing's Event Dispatch Thread, we can't afford
    // to refresh the status view on every simulated clock cycle. Instead, we will only refresh the status view
    // after this number of steps when running normally.
    //
    // Since we're simulating a 1MHz 6502 here, we have a 1 us delay between steps. Setting this to 20000
    // should give us a status update about every 100 ms.
    //
    // TODO: Work around the event dispatch thread with custom painting code instead of relying on Swing.
    //
    private static final int MAX_STEPS_BETWEEN_UPDATES = 20000;

    // The simulated machine
    private Machine machine;

    // A counter to keep track of the number of UI updates that have been
    // requested
    private int stepsSinceLastUpdate = 0;

    // The number of steps to run per click of the "Step" button
    private int stepsPerClick = 1;

    /**
     * The Main Window is the primary control point for the simulator.
     * It is in charge of the menu, and sub-windows. It also shows the
     * CPU status at all times.
     */
    private JFrame mainWindow;

    /**
     * The Trace Window shows the most recent 50,000 CPU states.
     */
    private final TraceLog traceLog;

    /**
     * The Memory Window shows the contents of one page of memory.
     */
    private final MemoryWindow memoryWindow;

    private final VideoPanel videoPanel;

    private final BreakpointsWindow breakpointsWindow;

    private SimulatorMenu menuBar;

    private RunLoop runLoop;
    private StatusPanel statusPane;

    private JButton runStopButton;
    private JButton stepButton;
    private JComboBox<String> stepCountBox;

    private JFileChooser fileChooser;
    private PreferencesDialog preferences;

    private Breakpoints breakpoints;

    private final Object commandMonitorObject = new Object();

    private MainCommand command = MainCommand.NONE;

    public enum MainCommand {
        NONE,
        SELECTMACHINE
    }

    /**
     * The list of step counts that will appear in the "Step" drop-down.
     */
    private static final String[] STEPS = {"1", "5", "10", "20", "50", "100"};

    public Simulator(Class machineClass) throws Exception {
        this.breakpoints = new Breakpoints(this);

        this.machine = (Machine) machineClass.getConstructors()[0].newInstance();

        // Initialize final fields in the constructor.
        this.traceLog = new TraceLog();
        this.memoryWindow = new MemoryWindow(machine.getBus());
        this.breakpointsWindow = new BreakpointsWindow(breakpoints, mainWindow);

        if (machine.getGPU() != null) {
            videoPanel = new VideoPanel(machine.getGPU(),2, 2);
        } else {
            videoPanel = null;
        }

        if (videoPanel != null && machine.getIO() != null) {
            videoPanel.setKeyListener(machine.getIO());
        }
    }

    /**
     * Display the main simulator UI.
     */
    void createAndShowUi() throws IOException {
        mainWindow = new JFrame();
        mainWindow.setTitle("6502 Simulator - " + machine.getName());
        mainWindow.setResizable(false);
        mainWindow.getContentPane().setLayout(new BorderLayout());

        // UI components used for I/O.
        this.statusPane = new StatusPanel(machine);

        // File Chooser
        fileChooser = new JFileChooser(System.getProperty("user.dir"));
        preferences = new PreferencesDialog(mainWindow, true);

        // Panel for Buttons
        JPanel buttonContainer = new JPanel();

        buttonContainer.setLayout(new FlowLayout());

        runStopButton = new JButton("Run");
        stepButton = new JButton("Step");
        JButton softResetButton = new JButton("Soft Reset");
        JButton hardResetButton = new JButton("Hard Reset");

        stepCountBox = new JComboBox<>(STEPS);
        stepCountBox.addActionListener(actionEvent -> {
            try {
                JComboBox cb = (JComboBox) actionEvent.getSource();
                stepsPerClick = Integer.parseInt((String) cb.getSelectedItem());
            } catch (NumberFormatException ex) {
                stepsPerClick = 1;
                stepCountBox.setSelectedIndex(0);
            }
        });

        buttonContainer.add(runStopButton);
        buttonContainer.add(stepButton);
        buttonContainer.add(stepCountBox);
        buttonContainer.add(softResetButton);
        buttonContainer.add(hardResetButton);

        // Left side - video
        mainWindow.getContentPane().add(videoPanel, BorderLayout.LINE_START);

        // Right side - status pane
        mainWindow.getContentPane().add(statusPane, BorderLayout.LINE_END);

        // Bottom - buttons.
        mainWindow.getContentPane().add(buttonContainer, BorderLayout.PAGE_END);

        runStopButton.addActionListener(actionEvent -> {
            if (runLoop != null && runLoop.isRunning()) {
                Simulator.this.handleStop();
            } else {
                Simulator.this.handleStart();
            }
        });

        stepButton.addActionListener(actionEvent -> Simulator.this.handleStep(stepsPerClick));

        softResetButton.addActionListener(actionEvent ->
            Simulator.this.handleReset(false));

        hardResetButton.addActionListener(actionEvent ->
            Simulator.this.handleReset(true));

        mainWindow.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        // The Menu. This comes last, because it relies on other components having
        // already been initialized.
        menuBar = new SimulatorMenu();
        mainWindow.setJMenuBar(menuBar);

        mainWindow.pack();
        mainWindow.setVisible(true);

        videoPanel.requestFocus();
        handleReset(false);
    }

    MainCommand waitForCommand() {
        synchronized (commandMonitorObject) {
            try {
                commandMonitorObject.wait();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        return command;
    }


    private void handleStart() {
        // Shift focus to the video.
        videoPanel.requestFocus();
        // Spin up the new run loop
        runLoop = new RunLoop();
        runLoop.start();
        traceLog.simulatorDidStart();
    }

    private void handleStop() {
        runLoop.requestStop();
        runLoop.interrupt();
        runLoop = null;
    }

    /*
     * Perform a reset.
     */
    private void handleReset(boolean isColdReset) {
        if (runLoop != null && runLoop.isRunning()) {
            runLoop.requestStop();
            runLoop.interrupt();
            runLoop = null;
        }

        try {
            logger.debug("Reset requested. Resetting CPU.");
            // Reset machine
            machine.reset();
            // Clear the console.
//            console.reset();
            // Reset the trace log.
            traceLog.reset();
            // If we're doing a cold reset, clear the memory.
            if (isColdReset) {
                Memory mem = machine.getRam();
                if (mem != null) {
                    mem.fill(0);
                }
            }
            // Update status.
            updateVisibleState();
        } catch (MemoryAccessException ex) {
            logger.error("Exception during simulator reset", ex);
        }
    }

    /**
     * Step the requested number of times, and immediately refresh the UI.
     */
    private void handleStep(int numSteps) {
        try {
            for (int i = 0; i < numSteps; i++) {
                step();
            }
            updateVisibleState();
        } catch (SymonException ex) {
            logger.error("Exception during simulator step", ex);
            ex.printStackTrace();
        }
    }

    /**
     * Perform a single step of the simulated system.
     */
    private void step() throws MemoryAccessException {
        machine.getCpu().step();

        traceLog.append(machine.getCpu().getCpuState());

        // This is a very expensive update, and we're doing it without
        // a delay, so we don't want to overwhelm the Swing event processing thread
        // with requests. Limit the number of ui updates that can be performed.
        if (stepsSinceLastUpdate++ > MAX_STEPS_BETWEEN_UPDATES) {
            updateVisibleState();
            stepsSinceLastUpdate = 0;
        }
    }

    /**
     * Load a program into memory at the simulatorDidStart address.
     */
    private void loadProgram(byte[] program, int startAddress) throws MemoryAccessException {
        int addr = startAddress, i;
        for (i = 0; i < program.length; i++) {
            machine.getBus().write(addr++, program[i] & 0xff);
        }

        logger.info("Loaded {} bytes at address 0x{}", i, Integer.toString(startAddress, 16));

        // After loading, be sure to reset and
        // Reset (but don't clear memory, naturally)
        machine.getCpu().reset();

        // Reset the stack program counter
        machine.getCpu().setProgramCounter(preferences.getProgramStartAddress());

        // Immediately update the UI.
        updateVisibleState();
    }

    /**
     * The main run thread.
     */
    class RunLoop extends Thread {
        private boolean isRunning = false;

        boolean isRunning() {
            return isRunning;
        }

        void requestStop() {
            isRunning = false;
        }

        public void run() {
            logger.debug("Starting main run loop.");
            isRunning = true;

            SwingUtilities.invokeLater(() -> {
                // Don't allow step while the simulator is running
                stepButton.setEnabled(false);
                stepCountBox.setEnabled(false);
                menuBar.simulatorDidStart();
                // Toggle the state of the run button
                runStopButton.setText("Stop");
            });

            try {
                do {
                    step();
                } while (shouldContinue());
            } catch (SymonException ex) {
                logger.error("Exception in main simulator run thread. Exiting run.", ex);
            }

            SwingUtilities.invokeLater(() -> {
                statusPane.updateState();
                memoryWindow.updateState();
                runStopButton.setText("Run");
                stepButton.setEnabled(true);
                stepCountBox.setEnabled(true);
                if (traceLog.isVisible()) {
                    traceLog.refresh();
                }
                menuBar.simulatorDidStop();
                traceLog.simulatorDidStop();
            });

            isRunning = false;
        }

        /**
         * @return True if the run loop should proceed to the next step.
         */
        private boolean shouldContinue() {
            return !breakpoints.contains(machine.getCpu().getProgramCounter()) &&
                    isRunning &&
                    !(preferences.getHaltOnBreak() && machine.getCpu().getInstruction() == 0x00);
        }
    }

    String disassembleOpAtAddress(int address) throws MemoryAccessException {
        return machine.getCpu().disassembleOpAtAddress(address);
    }

    class LoadProgramAction extends AbstractAction {
        LoadProgramAction() {
            super("Load Program...", null);
            putValue(SHORT_DESCRIPTION, "Load a program into memory");
            putValue(MNEMONIC_KEY, KeyEvent.VK_L);
        }

        public void actionPerformed(ActionEvent actionEvent) {
            try {
                int retVal = fileChooser.showOpenDialog(mainWindow);
                if (retVal == JFileChooser.APPROVE_OPTION) {
                    File f = fileChooser.getSelectedFile();
                    if (f.canRead()) {
                        long fileSize = f.length();

                        if (fileSize > machine.getMemorySize()) {
                            throw new IOException("File will not fit in " +
                                    "available memory ($" +
                                    Integer.toString(machine.getMemorySize(), 16) +
                                    " bytes)");
                        } else {
                            byte[] program = new byte[(int) fileSize];
                            int i = 0;
                            FileInputStream fis = new FileInputStream(f);
                            BufferedInputStream bis = new BufferedInputStream(fis);
                            DataInputStream dis = new DataInputStream(bis);
                            while (dis.available() != 0) {
                                program[i++] = dis.readByte();
                            }

                            // Now load the program at the starting address.
                            loadProgram(program, preferences.getProgramStartAddress());

                            SwingUtilities.invokeLater(() -> breakpoints.refresh());

                            // TODO: "Don't Show Again" checkbox
                            JOptionPane.showMessageDialog(mainWindow,
                                    "Loaded Successfully At " +
                                            String.format("$%04X", preferences.getProgramStartAddress()),
                                    "OK",
                                    JOptionPane.PLAIN_MESSAGE);
                        }
                    }
                }
            } catch (IOException ex) {
                logger.error("Unable to read program file.", ex);
                JOptionPane.showMessageDialog(mainWindow, ex.getMessage(), "Failure", JOptionPane.ERROR_MESSAGE);
            } catch (MemoryAccessException ex) {
                logger.error("Memory access error loading program", ex);
                JOptionPane.showMessageDialog(mainWindow, ex.getMessage(), "Failure", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    class LoadRomAction extends AbstractAction {
        LoadRomAction() {
            super("Load ROM...", null);
            putValue(SHORT_DESCRIPTION, "Load a ROM image");
            putValue(MNEMONIC_KEY, KeyEvent.VK_R);
        }

        public void actionPerformed(ActionEvent actionEvent) {
            try {
                int retVal = fileChooser.showOpenDialog(mainWindow);
                if (retVal == JFileChooser.APPROVE_OPTION) {
                    File romFile = fileChooser.getSelectedFile();
                    if (romFile.canRead()) {
                        long fileSize = romFile.length();

                        if (fileSize != machine.getRomSize()) {
                            throw new IOException("ROM file must be exactly " + String.valueOf(machine.getRomSize()) + " bytes.");
                        }

                        // Load the new ROM image
                        Memory rom = Memory.makeROM(machine.getRomBase(), machine.getRomBase() + machine.getRomSize() - 1, romFile);
                        machine.setRom(rom);

                        // Now, reset
                        machine.getCpu().reset();

                        updateVisibleState();

                        // Refresh breakpoints to show new memory contents.
                        breakpoints.refresh();

                        logger.info("ROM File `{}' loaded at {}", romFile.getName(),
                                String.format("0x%04X", machine.getRomBase()));
                        // TODO: "Don't Show Again" checkbox
                        JOptionPane.showMessageDialog(mainWindow,
                                "Loaded Successfully At " +
                                        String.format("$%04X", machine.getRomBase()),
                                "OK",
                                JOptionPane.PLAIN_MESSAGE);

                    }
                }
            } catch (IOException ex) {
                logger.error("Unable to read ROM file: {}", ex.getMessage());
                JOptionPane.showMessageDialog(mainWindow, ex.getMessage(), "Failure", JOptionPane.ERROR_MESSAGE);
            } catch (MemoryRangeException ex) {
                logger.error("Memory range error while loading ROM file: {}", ex.getMessage());
                JOptionPane.showMessageDialog(mainWindow, ex.getMessage(), "Failure", JOptionPane.ERROR_MESSAGE);
            } catch (MemoryAccessException ex) {
                logger.error("Memory access error while loading ROM file: {}", ex.getMessage());
                JOptionPane.showMessageDialog(mainWindow, ex.getMessage(), "Failure", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    class ShowPrefsAction extends AbstractAction {
        ShowPrefsAction() {
            super("Preferences...", null);
            putValue(SHORT_DESCRIPTION, "Show Preferences Dialog");
            putValue(MNEMONIC_KEY, KeyEvent.VK_P);
        }

        public void actionPerformed(ActionEvent actionEvent) {
            preferences.getDialog().setVisible(true);
        }
    }

    class QuitAction extends AbstractAction {
        QuitAction() {
            super("Quit", null);
            putValue(SHORT_DESCRIPTION, "Exit the Simulator");
            putValue(MNEMONIC_KEY, KeyEvent.VK_Q);
        }

        public void actionPerformed(ActionEvent actionEvent) {
            if (runLoop != null && runLoop.isRunning()) {
                runLoop.requestStop();
                runLoop.interrupt();
            }
            System.exit(0);
        }
    }

    class SetFontAction extends AbstractAction {
        SetFontAction(int size) {
            super(Integer.toString(size) + " pt", null);
            putValue(SHORT_DESCRIPTION, "Set font to " + size + "pt.");
        }

        public void actionPerformed(ActionEvent actionEvent) {
            SwingUtilities.invokeLater(() -> mainWindow.pack());
        }
    }

    class SetSpeedAction extends AbstractAction {
        private int speed;

        SetSpeedAction(int speed) {
            super(Integer.toString(speed) + " MHz", null);
            this.speed = speed;
            putValue(SHORT_DESCRIPTION, "Set simulated speed to " + speed + " MHz.");
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            if (speed < 1 || speed > CLOCK_PERIODS.length - 1) {
                return;
            }

            machine.getCpu().setClockPeriodInNs(CLOCK_PERIODS[speed]);
        }
    }

    class SetCpuAction extends AbstractAction {
        private Cpu.CpuBehavior behavior;

        SetCpuAction(String cpu, Cpu.CpuBehavior behavior) {
            super(cpu, null);
            this.behavior = behavior;
            putValue(SHORT_DESCRIPTION, "Set CPU to " + cpu);
        }

        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            machine.getCpu().setBehavior(behavior);
        }
    }

    class ToggleTraceWindowAction extends AbstractAction {
        ToggleTraceWindowAction() {
            super("Trace Log", null);
            putValue(SHORT_DESCRIPTION, "Show or Hide the Trace Log Window");
        }

        public void actionPerformed(ActionEvent actionEvent) {
            synchronized (traceLog) {
                if (traceLog.isVisible()) {
                    traceLog.setVisible(false);
                } else {
                    traceLog.refresh();
                    traceLog.setVisible(true);
                }
            }
        }
    }

    class ToggleMemoryWindowAction extends AbstractAction {
        ToggleMemoryWindowAction() {
            super("Memory Window", null);
            putValue(SHORT_DESCRIPTION, "Show or Hide the Memory Window");
        }

        public void actionPerformed(ActionEvent actionEvent) {
            synchronized (memoryWindow) {
                if (memoryWindow.isVisible()) {
                    memoryWindow.setVisible(false);
                } else {
                    memoryWindow.setVisible(true);
                }
            }
        }
    }

    class ToggleBreakpointWindowAction extends AbstractAction {
        ToggleBreakpointWindowAction() {
            super("Breakpoints...", null);
            putValue(SHORT_DESCRIPTION, "Show or Hide Breakpoints");
        }

        public void actionPerformed(ActionEvent actionEvent) {
            synchronized (breakpointsWindow) {
                if (breakpointsWindow.isVisible()) {
                    breakpointsWindow.setVisible(false);
                } else {
                    breakpointsWindow.setVisible(true);
                }
            }
        }
    }

    class SimulatorMenu extends JMenuBar {
        // Menu Items
        private JMenuItem loadProgramItem;
        private JMenuItem loadRomItem;

        /**
         * Create a new SimulatorMenu instance.
         */
        SimulatorMenu() {
            initMenu();
        }

        /**
         * Disable menu items that should not be available during simulator execution.
         */
        void simulatorDidStart() {
            loadProgramItem.setEnabled(false);
            if (loadRomItem != null) {
                loadRomItem.setEnabled(false);
            }
        }

        /**
         * Enable menu items that should be available while the simulator is stopped.
         */
        void simulatorDidStop() {
            loadProgramItem.setEnabled(true);
            if (loadRomItem != null) {
                loadRomItem.setEnabled(true);
            }
        }

        private void initMenu() {
            /*
             * File Menu
             */

            JMenu fileMenu = new JMenu("File");

            loadProgramItem = new JMenuItem(new LoadProgramAction());
            fileMenu.add(loadProgramItem);

            // Simple Machine does not implement a ROM, so it makes no sense to
            // offer a ROM load option.
            if (machine.getRom() != null) {
                loadRomItem = new JMenuItem(new LoadRomAction());
                fileMenu.add(loadRomItem);
            }

            JMenuItem prefsItem = new JMenuItem(new ShowPrefsAction());
            fileMenu.add(prefsItem);

            JMenuItem quitItem = new JMenuItem(new QuitAction());
            fileMenu.add(quitItem);

            add(fileMenu);

            /*
             * View Menu
             */

            JMenu viewMenu = new JMenu("View");

            final JCheckBoxMenuItem showTraceLog = new JCheckBoxMenuItem(new ToggleTraceWindowAction());
            // Un-check the menu item if the user closes the window directly
            traceLog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    showTraceLog.setSelected(false);
                }
            });
            viewMenu.add(showTraceLog);

            final JCheckBoxMenuItem showMemoryTable = new JCheckBoxMenuItem(new ToggleMemoryWindowAction());
            // Un-check the menu item if the user closes the window directly
            memoryWindow.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    showMemoryTable.setSelected(false);
                }
            });
            viewMenu.add(showMemoryTable);

            add(viewMenu);

            /*
             * Simulator Menu
             */


            JMenu simulatorMenu = new JMenu("Simulator");

            // "CPU" sub-menu
            JMenu cpuTypeMenu = new JMenu("CPU");
            ButtonGroup cpuGroup = new ButtonGroup();

            makeCpuMenuItem("NMOS 6502", Cpu.CpuBehavior.NMOS_6502, cpuTypeMenu, cpuGroup);
            makeCpuMenuItem("CMOS 65C02", Cpu.CpuBehavior.CMOS_6502, cpuTypeMenu, cpuGroup);

            // "Clock Speed" sub-menu
            JMenu speedSubMenu = new JMenu("Clock Speed");
            ButtonGroup speedGroup = new ButtonGroup();

            makeSpeedMenuItem(1, speedSubMenu, speedGroup);
            makeSpeedMenuItem(2, speedSubMenu, speedGroup);
            makeSpeedMenuItem(4, speedSubMenu, speedGroup);
            makeSpeedMenuItem(8, speedSubMenu, speedGroup);

            simulatorMenu.add(speedSubMenu);
            simulatorMenu.add(cpuTypeMenu);

            // "Breakpoints"
            final JCheckBoxMenuItem showBreakpoints = new JCheckBoxMenuItem(new ToggleBreakpointWindowAction());
            // Un-check the menu item if the user closes the window directly
            breakpointsWindow.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    showBreakpoints.setSelected(false);
                }
            });
            simulatorMenu.add(showBreakpoints);

            add(simulatorMenu);
        }

        private void makeFontSizeMenuItem(int size, JMenu subMenu, ButtonGroup group) {
            Action action = new SetFontAction(size);

            JCheckBoxMenuItem item = new JCheckBoxMenuItem(action);
            item.setSelected(size == DEFAULT_FONT_SIZE);
            subMenu.add(item);
            group.add(item);
        }

        private void makeSpeedMenuItem(int speed, JMenu subMenu, ButtonGroup group) {
            if (speed < 1 || speed > CLOCK_PERIODS.length - 1) {
                return;
            }

            Action action = new SetSpeedAction(speed);

            JCheckBoxMenuItem item = new JCheckBoxMenuItem(action);
            item.setSelected(CLOCK_PERIODS[speed] == Cpu.DEFAULT_CLOCK_PERIOD_IN_NS);
            subMenu.add(item);
            group.add(item);
        }

        private void makeCpuMenuItem(String cpu, Cpu.CpuBehavior behavior, JMenu subMenu, ButtonGroup group) {

            Action action = new SetCpuAction(cpu, behavior);

            JCheckBoxMenuItem item = new JCheckBoxMenuItem(action);
            item.setSelected(machine.getCpu().getBehavior() == behavior);
            subMenu.add(item);
            group.add(item);
        }

    }

    private void updateVisibleState() {
        // Immediately update the UI.
        SwingUtilities.invokeLater(() -> {
            // Now update the state
            statusPane.updateState();
            memoryWindow.updateState();
            if (traceLog.shouldUpdate()) {
                traceLog.refresh();
            }
        });
    }

}
