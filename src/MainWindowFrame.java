import Assembler.Disassembler;
import Assembler.CompilationError;
import Assembler.Assembler;
import StallingsCpu.StallingsCPU;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.SpringLayout;

import java.awt.GridLayout;
import java.awt.CardLayout;

import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JTable;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JMenuBar;

import java.awt.Canvas;

import javax.swing.JMenuItem;
import javax.swing.JMenu;

import java.awt.Font;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.awt.BorderLayout;


public class MainWindowFrame extends JFrame {

	private enum EmuState {
		PAUSED,RUNNING,STOPPED,NOT_LOADED
	}
	private String[] columnNames = {"Address", "Data", "Disassembly"};
	private Object[][]  data = {{"","",""}};
	private JLabel status;
	private StallingsCPU cpu;
	private Disassembler dis;
	private Assembler asm;
	private JTable table;
	private JPanel contentPane;
	private JEditorPane editA;
	private JEditorPane editB;
	private JEditorPane editC;
	private JEditorPane editD;
	private JEditorPane editCU;
	private JEditorPane editPC;
	private JEditorPane editMBR;
	private JEditorPane editMAR;
	private JEditorPane editIR;
	private EmuState state;
	private Emu emu;
	private final JFileChooser fc = new JFileChooser();

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainWindowFrame frame = new MainWindowFrame();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/*
	 * Populate the table data with a dump from the cpu
	 */
	private void populateTableData(){
		if (data == null || data.length == 1)
			data = new Object[StallingsCPU.MAX_MEM][3];
		
		int [] com = cpu.copyMemory(0, StallingsCPU.MAX_MEM);
		for(int i=0;i<StallingsCPU.MAX_MEM;i++){
			data[i][0] = String.format("0x%04x", i);
			data[i][1] = String.format("0x%04x", com[i]);
			data[i][2] = dis.dissasemble(com[i]);
		}
	}
	
	private synchronized void updateStatus(EmuState state) {
		switch(state){
		case PAUSED:
			status.setText("Paused");
			break;
		case RUNNING:
			status.setText("Running");
			break;
		case NOT_LOADED:
			status.setText("Nothing Loaded");
			break;
		case STOPPED:
			status.setText("Finished");
		}
	}
	
	/*
	 * Populate the Editable text boxes with the corresponding value from the CPU
	 */
	private void populateEditable(){
		String[] cpuArray = cpu.toString().split(",");
		editA.setText(cpuArray[0].split(":")[1]);
		editB.setText(cpuArray[1].split(":")[1]);
		editC.setText(cpuArray[2].split(":")[1]);
		editD.setText(cpuArray[3].split(":")[1]);
		editPC.setText(cpuArray[4].split(":")[1]);
		editIR.setText(cpuArray[5].split(":")[1]);
		editMBR.setText(cpuArray[6].split(":")[1]);
		editMAR.setText(cpuArray[7].split(":")[1]);
		editCU.setText(cpuArray[8].split(":")[1]);
	}
	
	/**
	 * Create the frame.
	 */
	public MainWindowFrame() {
		cpu = new StallingsCPU();
		dis = new Disassembler();
		asm = new Assembler();
		
		setTitle("Stallings TOY1 CPU");
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 800, 600);

		populateTableData();
		
		Font smallFont = new Font("Tahoma", Font.PLAIN, 18);
		
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		mnFile.setFont(new Font("Segoe UI", Font.PLAIN, 20));
		menuBar.add(mnFile);
		
		JMenuItem mntmOpenComFile = new JMenuItem("Open");
		mntmOpenComFile.addActionListener(new OpenButtonListener());
		mntmOpenComFile.setFont(new Font("Segoe UI", Font.PLAIN, 20));
		mnFile.add(mntmOpenComFile);
		
		JMenuItem mntmAssemble = new JMenuItem("Assemble");
		mntmAssemble.addActionListener(new AssembleButtonListener());
		mntmAssemble.setFont(new Font("Segoe UI", Font.PLAIN, 20));
		mnFile.add(mntmAssemble);
		
		JMenuItem mntmExit = new JMenuItem("Exit");
		mntmExit.setFont(new Font("Segoe UI", Font.PLAIN, 20));
		mntmExit.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		mnFile.add(mntmExit);
		
		JMenu mnHelp = new JMenu("Help");
		mnHelp.setFont(new Font("Segoe UI", Font.PLAIN, 20));
		menuBar.add(mnHelp);
		
		JMenuItem mntmAbout = new JMenuItem("About");
		mntmAbout.setFont(new Font("Segoe UI", Font.PLAIN, 20));
		mnHelp.add(mntmAbout);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		SpringLayout sl_contentPane = new SpringLayout();
		contentPane.setLayout(sl_contentPane);
		
		JButton btnNewButton = new JButton("Run ");
		btnNewButton.setFont(smallFont);
		btnNewButton.addActionListener(new RunButtonListener());
		contentPane.add(btnNewButton);
		
		JButton btnNewButton_1 = new JButton("Pause");
		btnNewButton_1.setFont(smallFont);
		btnNewButton_1.addActionListener(new PauseButtonListener());
		sl_contentPane.putConstraint(SpringLayout.WEST, btnNewButton_1, 20, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.WEST, btnNewButton, 0, SpringLayout.WEST, btnNewButton_1);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, btnNewButton, -6, SpringLayout.NORTH, btnNewButton_1);
		contentPane.add(btnNewButton_1);
		
		JButton btnNewButton_2 = new JButton("Step ");
		btnNewButton_2.addActionListener(new StepButtonListener());
		btnNewButton_2.setFont(smallFont);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, btnNewButton_1, -6, SpringLayout.NORTH, btnNewButton_2);
		sl_contentPane.putConstraint(SpringLayout.WEST, btnNewButton_2, 20, SpringLayout.WEST, contentPane);
		contentPane.add(btnNewButton_2);
		
		JButton btnNewButton_3 = new JButton("Reset");
		btnNewButton_3.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
                            cpu.reset();
                            populateEditable();
                            populateTableData();
                            int row = cpu.PC();
			
                            table.clearSelection();
                            table.setRowSelectionInterval(row, row);
			
                            table.repaint();
			}
		});
		btnNewButton_3.setFont(smallFont);
		sl_contentPane.putConstraint(SpringLayout.NORTH, btnNewButton_3, 116, SpringLayout.NORTH, contentPane);
		sl_contentPane.putConstraint(SpringLayout.WEST, btnNewButton_3, 20, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, btnNewButton_2, -6, SpringLayout.NORTH, btnNewButton_3);
		contentPane.add(btnNewButton_3);
		
		table = new JTable(data,columnNames);
		table.setFont(smallFont);
		table.getTableHeader().setFont(smallFont);
		JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.setFont(new Font("Tahoma",Font.PLAIN,18));
		sl_contentPane.putConstraint(SpringLayout.NORTH, scrollPane, 44, SpringLayout.SOUTH, btnNewButton_3);
		sl_contentPane.putConstraint(SpringLayout.WEST, scrollPane, 20, SpringLayout.WEST, contentPane);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, scrollPane, -20, SpringLayout.SOUTH, contentPane);
		sl_contentPane.putConstraint(SpringLayout.EAST, scrollPane, -20, SpringLayout.EAST, contentPane);
		contentPane.add(scrollPane);
		
		status = new JLabel("CPU STATUS:");
		status.setFont(smallFont);
		sl_contentPane.putConstraint(SpringLayout.NORTH, status, 0, SpringLayout.NORTH, btnNewButton);
		sl_contentPane.putConstraint(SpringLayout.WEST, status, 42, SpringLayout.EAST, btnNewButton);
		contentPane.add(status);
		
		JLabel lblA = new JLabel("R0:");
		lblA.setFont(smallFont);
		sl_contentPane.putConstraint(SpringLayout.NORTH, lblA, 17, SpringLayout.SOUTH, status);
		sl_contentPane.putConstraint(SpringLayout.WEST, lblA, 0, SpringLayout.WEST, status);
		contentPane.add(lblA);
		
		JLabel lblB = new JLabel("R1:");
		lblB.setFont(smallFont);
		sl_contentPane.putConstraint(SpringLayout.NORTH, lblB, 17, SpringLayout.SOUTH, lblA);
		sl_contentPane.putConstraint(SpringLayout.WEST, lblB, 0, SpringLayout.WEST, lblA);
		contentPane.add(lblB);
		
		JLabel lblC = new JLabel("R2:");
		lblC.setFont(smallFont);
		sl_contentPane.putConstraint(SpringLayout.NORTH, lblC, 17, SpringLayout.SOUTH, lblB);
		sl_contentPane.putConstraint(SpringLayout.WEST, lblC, 0, SpringLayout.WEST, lblB);
		contentPane.add(lblC);
		
		JLabel lblD = new JLabel("R3:");
		lblD.setFont(smallFont);
		sl_contentPane.putConstraint(SpringLayout.NORTH, lblD, 17, SpringLayout.SOUTH, lblC);
		sl_contentPane.putConstraint(SpringLayout.WEST, lblD, 0, SpringLayout.WEST, lblC);
		contentPane.add(lblD);
		
		editA = new JEditorPane();
		editA.setFont(smallFont);
		sl_contentPane.putConstraint(SpringLayout.WEST, editA, 6, SpringLayout.EAST, lblA);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, editA, 0, SpringLayout.SOUTH, lblA);
		contentPane.add(editA);
		
		editB = new JEditorPane();
		editB.setFont(smallFont);
		sl_contentPane.putConstraint(SpringLayout.WEST, editB, 0, SpringLayout.WEST, editA);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, editB, 0, SpringLayout.SOUTH, lblB);
		contentPane.add(editB);
		
		editC = new JEditorPane();
		editC.setFont(smallFont);
		sl_contentPane.putConstraint(SpringLayout.WEST, editC, 0, SpringLayout.WEST, editB);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, editC, 0, SpringLayout.SOUTH, lblC);
		contentPane.add(editC);
		
		editD = new JEditorPane();
		editD.setFont(smallFont);
		sl_contentPane.putConstraint(SpringLayout.WEST, editD, 0, SpringLayout.WEST, editC);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, editD, 0, SpringLayout.SOUTH, lblD);
		contentPane.add(editD);
		
		JLabel lPC = new JLabel("PC:");
		lPC.setFont(smallFont);
		sl_contentPane.putConstraint(SpringLayout.WEST, lPC, 39, SpringLayout.EAST, editA);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, lPC, 0, SpringLayout.SOUTH, lblA);
		contentPane.add(lPC);
		
		JLabel lMBR = new JLabel("MBR:");
		lMBR.setFont(smallFont);
		sl_contentPane.putConstraint(SpringLayout.NORTH, lMBR, 17, SpringLayout.SOUTH, lPC);
		sl_contentPane.putConstraint(SpringLayout.EAST, lMBR, 0, SpringLayout.EAST, lPC);
		contentPane.add(lMBR);
		
		JLabel lMAR = new JLabel("MAR:");
		lMAR.setFont(smallFont);
		sl_contentPane.putConstraint(SpringLayout.NORTH, lMAR, 17, SpringLayout.SOUTH, lMBR);
		sl_contentPane.putConstraint(SpringLayout.EAST, lMAR, 0, SpringLayout.EAST, lMBR);
		contentPane.add(lMAR);
		
		JLabel lIR = new JLabel("IR:");
		lIR.setFont(smallFont);
		sl_contentPane.putConstraint(SpringLayout.NORTH, lIR, 17, SpringLayout.SOUTH, lMAR);
		sl_contentPane.putConstraint(SpringLayout.EAST, lIR, 0, SpringLayout.EAST, lMAR);
		contentPane.add(lIR);
		
		editPC = new JEditorPane();
		editPC.setFont(smallFont);
		sl_contentPane.putConstraint(SpringLayout.WEST, editPC, 6, SpringLayout.EAST, lPC);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, editPC, 0, SpringLayout.SOUTH, lblA);
		contentPane.add(editPC);
		
		editMBR = new JEditorPane();
		editMBR.setFont(smallFont);
		sl_contentPane.putConstraint(SpringLayout.WEST, editMBR, 6, SpringLayout.EAST, lMBR);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, editMBR, 0, SpringLayout.SOUTH, editB);
		contentPane.add(editMBR);
		
		editMAR = new JEditorPane();
		editMAR.setFont(smallFont);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, editMAR, 0, SpringLayout.SOUTH, lMAR);
		sl_contentPane.putConstraint(SpringLayout.EAST, editMAR, 0, SpringLayout.EAST, editPC);
		contentPane.add(editMAR);
		
		editIR = new JEditorPane();
		editIR.setFont(smallFont);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, editIR, 0, SpringLayout.SOUTH, lIR);
		sl_contentPane.putConstraint(SpringLayout.EAST, editIR, 0, SpringLayout.EAST, editPC);
		contentPane.add(editIR);
		
		JLabel lCU = new JLabel("CU:");
		lCU.setFont(smallFont);
		sl_contentPane.putConstraint(SpringLayout.NORTH, lCU, 0, SpringLayout.NORTH, btnNewButton);
		sl_contentPane.putConstraint(SpringLayout.EAST, lCU, 0, SpringLayout.EAST, lPC);
		contentPane.add(lCU);
		
		editCU = new JEditorPane();
		editCU.setFont(smallFont);
		sl_contentPane.putConstraint(SpringLayout.WEST, editCU, 0, SpringLayout.WEST, editPC);
		sl_contentPane.putConstraint(SpringLayout.SOUTH, editCU, 0, SpringLayout.SOUTH, status);
		contentPane.add(editCU);
		
		status.setText("Stopped");
		Canvas canvas = new Canvas();
		sl_contentPane.putConstraint(SpringLayout.NORTH, canvas, 0, SpringLayout.NORTH, editCU);
		sl_contentPane.putConstraint(SpringLayout.WEST, canvas, 6, SpringLayout.EAST, editPC);
		contentPane.add(canvas);
		

		populateTableData();
		populateEditable();
	}
	
	
	private class Emu implements Runnable {
		
		private volatile boolean running = true;
		@Override
		public void run() {
			while(running&&!cpu.isStopped()){
				cpu.full_cycle();
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			populateEditable();
			populateTableData();
			int row = cpu.PC();
			
			table.clearSelection();
			table.setRowSelectionInterval(row, row);
			
			table.repaint();
			if(cpu.isStopped())
				updateStatus(EmuState.STOPPED);
			else
				updateStatus(EmuState.PAUSED);
		}
		public boolean isRunning(){
			return running;
		}
		
		public void stopEmu(){
			running = false;
		}
	}
	private class RunButtonListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			new Thread(emu = new Emu()).start();
			updateStatus(EmuState.RUNNING);
		}
		
	}
	
	private class StepButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent arg0) {
			status.setText("Stepping");
			
			if(!cpu.isStopped())
				cpu.step_cycle();
			
			populateTableData();
			
			int row = cpu.PC();
			
			table.clearSelection();
			table.setRowSelectionInterval(row, row);
			
			populateEditable();
			
			if(!cpu.isStopped())
				updateStatus(EmuState.PAUSED);
			else
				updateStatus(EmuState.STOPPED);
			
			table.repaint();
		}

	}
	
	private class PauseButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent arg0) {
			if(emu.isRunning())
				emu.stopEmu();
		}

	}
	
	private class AssembleButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if(fc.showOpenDialog(MainWindowFrame.this) == JFileChooser.APPROVE_OPTION){
				System.out.println("Wants to open file " + fc.getSelectedFile().getName());
				System.out.println("Compiling program");
				try {
					int[] prog = asm.assemble(fc.getSelectedFile());
					JOptionPane.showMessageDialog(MainWindowFrame.this, "Program Compiled Correctly");
					cpu.copyProgram(prog, 0);
					JOptionPane.showMessageDialog(MainWindowFrame.this, "Choose File to save COM file in");
					if(fc.showSaveDialog(MainWindowFrame.this) == JFileChooser.APPROVE_OPTION){
						int fileLength = prog.length*2;
						byte[] aOut = new byte[fileLength];
						for(int i=0;i<prog.length;i++){
							byte[] bytes = ByteBuffer.allocate(4).putInt(prog[i]).array();
							aOut[i*2] = bytes[2];
							aOut[i*2+1] = bytes[3];
						}
						BufferedOutputStream sOut = new BufferedOutputStream(new FileOutputStream(fc.getSelectedFile()));
						sOut.write(aOut);
						sOut.close();
						System.out.println("File "+fc.getSelectedFile().getName() + " created and saved");
						cpu.start();
					}
				} catch (CompilationError e1) {
					JOptionPane.showMessageDialog(MainWindowFrame.this, e1.getMessage(), "Compilation Error", JOptionPane.ERROR_MESSAGE);
					e1.printStackTrace();
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					JOptionPane.showMessageDialog(MainWindowFrame.this, "Unable to create File");
					e1.printStackTrace();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					JOptionPane.showMessageDialog(MainWindowFrame.this, "Unable to write to File");
					e1.printStackTrace();
				}
				
			}
		}
	}
	
	private class OpenButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent arg0) {
			if(fc.showOpenDialog(MainWindowFrame.this) == JFileChooser.APPROVE_OPTION){
				System.out.println("Wants to open file " + fc.getSelectedFile().getName());
				try {
					cpu.loadProgram(fc.getSelectedFile());
					populateEditable();
					populateTableData();
					System.out.println("File Openend and Loaded");
					table.repaint();
					cpu.start();
					updateStatus(EmuState.PAUSED);
				} catch (IOException e) {
					JOptionPane.showMessageDialog(MainWindowFrame.this, "Invalid File for emulator");
					e.printStackTrace();
				}
				
			}
			
		}
	}
}
