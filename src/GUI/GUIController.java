package GUI;

import Assembler.Assembler;
import Assembler.CompilationError;
import StallingsCpu.StallingsCPU;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

class GUIController {

    private enum EmuState {
        PAUSED, RUNNING, STOPPED, NOT_LOADED
    }

    private class Emu implements Runnable {

        private volatile boolean running = true;

        @Override
        public void run() {
            while (running && !cpu.isStopped()) {
                cpu.full_cycle();
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                }
            }

            reloadState();
            
            if (cpu.isStopped()) {
                updateStatus(EmuState.STOPPED);
            } else {
                updateStatus(EmuState.PAUSED);
            }
        }

        public boolean isRunning() {
            return running;
        }

        public void stopEmu() {
            running = false;
        }
    }

    private final MainDebuggerWindowFrame parent;
    private final StallingsCPU cpu;
    private final Assembler asm;
    private Emu emu;
    private Object[][] data = new Object[StallingsCPU.MAX_MEM][2];
    private final JFileChooser fc = new JFileChooser();

    public GUIController(MainDebuggerWindowFrame parent) {
        this.parent = parent;
        cpu = new StallingsCPU();
        asm = new Assembler();
    }

    public void hackyTableUpdate() {
        parent.memoryTable.setModel(new javax.swing.table.DefaultTableModel(
                data,
                new String[]{
                    "Address", "Data"
                }
        ) {
            Class[] types = new Class[]{
                java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean[]{
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
    }

    public void OnInitFinished() {
        updateStatus(EmuState.NOT_LOADED);
        populateTableData();

        // having a hard time changing this in real time so just override
        // cant trust the designer to give me the proper code 
        hackyTableUpdate();
    }

    void menuLoad() {
        if (fc.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            System.out.println("Wants to open file " + fc.getSelectedFile().getName());
            try {
                cpu.reset();
                cpu.loadProgram(fc.getSelectedFile());
                System.out.println("File Openend and Loaded");
                reloadState();
                cpu.start();
                updateStatus(EmuState.PAUSED);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(parent, "Invalid File for emulator");
            }

        }
    }

    void menuAssemble() {
        if (fc.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            System.out.println("Wants to open file " + fc.getSelectedFile().getName());
            System.out.println("Compiling program");
            try {
                int[] prog = asm.assemble(fc.getSelectedFile());
                JOptionPane.showMessageDialog(parent, "Program Compiled Correctly");
                cpu.reset();
                cpu.copyProgram(prog, 0);
                JOptionPane.showMessageDialog(parent, "Choose File to save COM file in");
                if (fc.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
                    int fileLength = prog.length * 2;
                    byte[] aOut = new byte[fileLength];
                    for (int i = 0; i < prog.length; i++) {
                        byte[] bytes = ByteBuffer.allocate(4).putInt(prog[i]).array();
                        aOut[i * 2] = bytes[2];
                        aOut[i * 2 + 1] = bytes[3];
                    }
                    BufferedOutputStream sOut = new BufferedOutputStream(new FileOutputStream(fc.getSelectedFile()));
                    sOut.write(aOut);
                    sOut.close();
                    System.out.println("File " + fc.getSelectedFile().getName() + " created and saved");
                    cpu.start();
                    reloadState();
                    updateStatus(EmuState.PAUSED);
                    JOptionPane.showMessageDialog(parent, "Program has been loaded into the emulator.");
                }
            } catch (CompilationError e1) {
                JOptionPane.showMessageDialog(parent, e1.getMessage(), "Compilation Error", JOptionPane.ERROR_MESSAGE);
            } catch (FileNotFoundException e1) {
                JOptionPane.showMessageDialog(parent, "Unable to create File");
            } catch (IOException e1) {
                JOptionPane.showMessageDialog(parent, "Unable to write to File");
            }
        }
    }

    void menuHelp() {
        JOptionPane.showMessageDialog(parent, "Load *.com files to run them. Compile .hex/txt and .asm files to create COM files.");
    }

    void menuHex() {
        if (fc.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            System.out.println("Wants to open file " + fc.getSelectedFile().getName());
            System.out.println("Compiling program");
            try {
                int[] prog = asm.assembleHex(fc.getSelectedFile());
                JOptionPane.showMessageDialog(parent, "Program Compiled Correctly");
                cpu.copyProgram(prog, 0);
                cpu.start();
                reloadState();
                updateStatus(EmuState.PAUSED);
            } catch (CompilationError e1) {
                JOptionPane.showMessageDialog(parent, e1.getMessage(), "Compilation Error", JOptionPane.ERROR_MESSAGE);
            } 
        }
    }

    void buttonRun() {
        cpu.stateReset();
        new Thread(emu = new Emu()).start();
        updateStatus(EmuState.RUNNING);
    }

    void buttonPause() {
        if (emu.isRunning()) {
            emu.stopEmu();
        }
    }

    void buttonReset() {
        cpu.reset();
        reloadState();
        updateStatus(EmuState.NOT_LOADED);
    }

    void reloadState() {
        populateEditable();
        populateTableData();
        int row = cpu.PC();

        parent.memoryTable.clearSelection();
        parent.memoryTable.setRowSelectionInterval(row, row);

        parent.memoryTable.repaint();
    }
    
    void buttonStep() {
        parent.stateLabel.setText("Stepping");

        if (!cpu.isStopped()) {
            cpu.step_cycle();
        }

        reloadState();

        if (!cpu.isStopped()) {
            updateStatus(EmuState.PAUSED);
        } else {
            updateStatus(EmuState.STOPPED);
        }

        parent.memoryTable.repaint();
        
        // check radio button
        switch (cpu.state){
            case FETCH:
                uncheckRadio();
                parent.fetchRadio.setSelected(true);
                break;
            case DECODE:
                uncheckRadio();
                parent.decodeRadio.setSelected(true);
                break;
            case EXECUTE:
                uncheckRadio();
                parent.executeRadio.setSelected(true);
                break;
        }
    }

    void uncheckRadio() {
        parent.fetchRadio.setSelected(false);
        parent.decodeRadio.setSelected(false);
        parent.executeRadio.setSelected(false);
    }
    
    void populateEditable() {
        String[] cpuArray = cpu.toString().split(",");
        parent.accField.setText(cpuArray[0].split(":")[1]);
        parent.pcField.setText(cpuArray[4].split(":")[1]);
        parent.irField.setText(cpuArray[5].split(":")[1]);
        parent.mbrField.setText(cpuArray[6].split(":")[1]);
        parent.marField.setText(cpuArray[7].split(":")[1]);
        parent.cuField.setText(cpuArray[8].split(":")[1]);
    }

    void populateTableData() {
        if (data == null || data.length == 1) {
            data = new Object[StallingsCPU.MAX_MEM][2];
        }

        int[] com = cpu.copyMemory(0, StallingsCPU.MAX_MEM);
        for (int i = 0; i < StallingsCPU.MAX_MEM; i++) {
            data[i][0] = String.format("0x%04x", i);
            data[i][1] = String.format("0x%04x", com[i]);
        }

        hackyTableUpdate();
    }

    void updateStatus(EmuState state) {
        switch (state) {
            case PAUSED:
                parent.stateLabel.setText("Paused");
                break;
            case RUNNING:
                parent.stateLabel.setText("Running");
                break;
            case NOT_LOADED:
                parent.stateLabel.setText("Nothing Loaded");
                break;
            case STOPPED:
                parent.stateLabel.setText("Finished");
        }
    }
}
