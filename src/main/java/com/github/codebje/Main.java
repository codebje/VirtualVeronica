/*
 * Copyright (c) 2016 Seth J. Morabito <web@loomcom.com>
 *                    Maik Merten <maikmerten@googlemail.com>
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

import com.github.codebje.machines.Veronica;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    
     /**
     * Main entry point to the simulator. Creates a simulator and shows the main
     * window.
     *
     * @param args Program arguments
     */
    public static void main(String args[]) throws Exception {
        
        Class machineClass = Veronica.class;

        final Simulator simulator = new Simulator(machineClass);
        
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                // Create the main UI window
                simulator.createAndShowUi();

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        simulator.waitForCommand();
    }
}
