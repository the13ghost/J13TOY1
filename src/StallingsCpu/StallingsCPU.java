package StallingsCpu;

import StallingsCpu.OpcodeStateMachines.BaseOpcode;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


public class StallingsCPU {
	private boolean running;

	private int stateCounter;
	private int stateMax;

	/*
	 * Test the CPU
	 */
	public static void main(String[] args) {
		StallingsCPU cpu =new StallingsCPU();

		int[] program = {0x1004,
				0x4005,
				0x2004,
				0x0000,
				0x001E,
				0x0014};

		cpu.copyProgram(program, 0);
		cpu.start();

		while(!cpu.isStopped()){
			cpu.full_cycle();
		}

		System.out.println(cpu.toString());

		int[] copy = cpu.copyMemory(3, 6);
		for(int x: copy) {
			System.out.println(Integer.toHexString(x) + ", ");
		}
	}

	// 16 bit registers
	private int A;
	private int B;
	private int C;
	private int D;

	// CPU DATA
	private int PC;
	private int IR;
	private int MBR;
	private int MAR;

	// CU
	private int CU;

	// Memory 1024x16bit words... which is 0x400 on addressing hmmm
	public static final int MAX_MEM = 1024;
	private int[] MEM;

	// CYCLE STATE
	private CycleState state;

	// DONT KNOW YET
	// IOAR
	// IOBR

        public int step_rn;
        public int step_rm;
        public int step_bufn;
        public int step_bufm;
        
        private BaseOpcode[] opcodeStateMachines;
        
	/*
	 * Initializes memory and the cpu
	 */
	public StallingsCPU(){
		MEM = new int[MAX_MEM];
                
                // initialize opcode statemachines LookupTable
                opcodeStateMachines = new BaseOpcode[Opcode.values().length];
                opcodeStateMachines[0] = new StallingsCpu.OpcodeStateMachines.STOP(this);
                opcodeStateMachines[1] = new StallingsCpu.OpcodeStateMachines.LOAD(this);
                opcodeStateMachines[2] = new StallingsCpu.OpcodeStateMachines.STORE(this);
                opcodeStateMachines[3] = new StallingsCpu.OpcodeStateMachines.ADD(this);
                opcodeStateMachines[4] = new StallingsCpu.OpcodeStateMachines.SUB(this);
                opcodeStateMachines[5] = new StallingsCpu.OpcodeStateMachines.GOTO(this);
                opcodeStateMachines[6] = new StallingsCpu.OpcodeStateMachines.IFZER(this);
                opcodeStateMachines[7] = new StallingsCpu.OpcodeStateMachines.IFNEG(this);
                opcodeStateMachines[8] = new StallingsCpu.OpcodeStateMachines.LOADIN(this);
                opcodeStateMachines[9] = new StallingsCpu.OpcodeStateMachines.STOREIN(this);
                opcodeStateMachines[10] = new StallingsCpu.OpcodeStateMachines.ADDIN(this);
                opcodeStateMachines[11] = new StallingsCpu.OpcodeStateMachines.SUBIN(this);
                opcodeStateMachines[12] = new StallingsCpu.OpcodeStateMachines.LOADIM(this);
                opcodeStateMachines[13] = new StallingsCpu.OpcodeStateMachines.STOREIM(this);
                opcodeStateMachines[14] = new StallingsCpu.OpcodeStateMachines.ADDIM(this);
                opcodeStateMachines[15] = new StallingsCpu.OpcodeStateMachines.SUBIM(this);
	}

	/*
	 * full emulation cycle
	 */
	public void full_cycle(){
		if(running){
			MAR(PC()); 
			MBR(READ(MAR()));
			IR(MBR());
			PC(PC()+1);
			CU(decode(IR()));
			execute(CU());
		}
	}

	/*
	 * Pedagogical emulation cycle
	 */
	public void step_cycle(){

		switch(state){
		case FETCH:
                        stateMax = 3;
			step_fetch();
			break;
		case DECODE:
                        stateMax = 1;
			step_decode();
			break;
		case EXECUTE:
			step_execute(CU());
			break;
		}
		stateCounter++;
		if(stateCounter==stateMax){
			state = CycleState.values()[state.ordinal()+1];
			if(state == null)
				state = CycleState.FETCH;
		}
	}
        
	/*
	 * Fetch Stepped
	 */
	public void step_fetch(){
		//fetch
		switch(stateCounter){
		case 0:
			MAR(PC());
			break;
		case 1:
			MBR(READ(MAR()));
			break;
		case 2:
			IR(MBR());
			PC(PC()+1);
			break;
		}
	}
        
        /*
	 * Decode Stepped
	 */
	public void step_decode(){
		CU(decode(IR()));
                step_rn = (IR()&0xC00)>>10;
                step_bufn = step_rn==0?A():step_rn==1?B():step_rn==2?C():D();
                step_rm = (IR()&0x300)>>8;
                step_bufm = step_rm==0?A():step_rm==1?B():step_rm==2?C():D();
	}

	/*
	 * Execute Stepped
	 * TODO: FINISH
	 */
	public void step_execute(int control) {
            opcodeStateMachines[control].step(stateCounter);
            stateMax = opcodeStateMachines[control].stateMax;
	}
        
	/*
	 * Full Decoding
	 */
	public int decode(int instruction) {
		return (instruction&0xf000)>>12;
	}

	/*
	 * Full Execution
	 */
	public void execute(int control) {
		Opcode op = Opcode.values()[control];

		int rn = (IR()&0xC00)>>10;
		int bufn = rn==0?A():rn==1?B():rn==2?C():D();
		int rm = (IR()&0x300)>>8;
		int bufm = rm==0?A():rm==1?B():rm==2?C():D();

		switch(op){
		case STOP:
			running = false;
			break;
		case GOTO:
			MBR(IR()&0x3FF);
			PC(MBR());
			break;
		case IFZER:
			MBR(bufn);
			MAR(IR()&0x3FF);
			if(MBR()==0)
				PC(MAR());
			break;
		case IFNEG:
			MBR(bufn);
			MAR(IR()&0x3FF);
			if(MBR()>0x7FFF)
				PC(MAR());
			break;

		case ADD:
			MAR(IR()&0x3FF);
			MBR(READ(MAR()));
			switch(rn){
			case 0: A(A()+MBR()); break;
			case 1: B(B()+MBR()); break;
			case 2: C(C()+MBR()); break;
			case 3: D(D()+MBR()); break;
			}
			break;
		case SUB:
			MAR(IR()&0x3FF);
			MBR(READ(MAR()));
			switch(rn){
			case 0: A(A()-MBR()); break;
			case 1: B(B()-MBR()); break;
			case 2: C(C()-MBR()); break;
			case 3: D(D()-MBR()); break;
			}
			break;
		case LOAD:
			MAR(IR()&0x3FF);
			MBR(READ(MAR()));
			switch(rn){
			case 0: A(MBR()); break;
			case 1: B(MBR()); break;
			case 2: C(MBR()); break;
			case 3: D(MBR()); break;
			}
			break;
		case STORE:
			MAR(IR()&0x3FF);
			switch(rn){
			case 0: MBR(A()); break;
			case 1: MBR(B()); break;
			case 2: MBR(C()); break;
			case 3: MBR(D()); break;
			}
			WRITE(MAR(),MBR());
			break;

		case ADDIN:
			MAR(bufm);
			MBR(READ(MAR()));
			switch(rn){
			case 0: A(A()+MBR()); break;
			case 1: B(B()+MBR()); break;
			case 2: C(C()+MBR()); break;
			case 3: D(D()+MBR()); break;
			}
			break;
		case SUBIN:
			MAR(bufm);
			MBR(READ(MAR()));
			switch(rn){
			case 0: A(A()-MBR()); break;
			case 1: B(B()-MBR()); break;
			case 2: C(C()-MBR()); break;
			case 3: D(D()-MBR()); break;
			}
			break;
		case LOADIN:
			MAR(bufm);
			MBR(READ(MAR()));
			switch(rn){
			case 0: A(MBR()); break;
			case 1: B(MBR()); break;
			case 2: C(MBR()); break;
			case 3: D(MBR()); break;
			}
			break;
		case STOREIN:
			MAR(bufm);
			switch(rn){
			case 0: MBR(A()); break;
			case 1: MBR(B()); break;
			case 2: MBR(C()); break;
			case 3: MBR(D()); break;
			}
			WRITE(MAR(),MBR());
			break;
		case ADDIM:
			MBR(IR()&0x3FF);
			switch(rn){
			case 0: A(A()+MBR()); break;
			case 1: B(B()+MBR()); break;
			case 2: C(C()+MBR()); break;
			case 3: D(D()+MBR()); break;
			}
			break;
		case SUBIM:
			MBR(IR()&0x3FF);
			switch(rn){
			case 0: A(A()-MBR()); break;
			case 1: B(B()-MBR()); break;
			case 2: C(C()-MBR()); break;
			case 3: D(D()-MBR()); break;
			}
			break;
		case LOADIM:
			MAR(IR()&0x3FF);
			MBR(READ(MAR()));
			switch(rn){
			case 0: A(MBR()); break;
			case 1: B(MBR()); break;
			case 2: C(MBR()); break;
			case 3: D(MBR()); break;
			}
			break;
		case STOREIM:
			MBR(IR()&0x3FF);
			switch(rn){
			case 0: MAR(A()); break;
			case 1: MAR(B()); break;
			case 2: MAR(C()); break;
			case 3: MAR(D()); break;
			}
			WRITE(MAR(),MBR());
			break;
		}
	}

	public int A() {
		return A;
	}
	public void A(int value) {
		A = (value&0xffff);
	}
	public int B() {
		return B;
	}
	public void B(int value) {
		B = (value&0xffff);
	}
	public int C() {
		return C;
	}
	public void C(int value) {
		C = (value&0xffff);
	}
	public int D() {
		return D;
	}
	public void D(int value) {
		D = (value&0xffff);
	}

	public int PC() {
		return PC;
	}
	public void PC(int value) {
		PC = (value&0xffff);
	}
	public int IR() {
		return IR;
	}
	public void IR(int value) {
		IR = (value&0xffff);
	}
	public int MBR() {
		return MBR;
	}
	public void MBR(int value) {
		MBR = (value&0xffff);
	}
	public int MAR() {
		return MAR;
	}
	public void MAR(int value) {
		MAR = (value&0xffff);
	}
	public int CU() {
		return CU;
	}
	public void CU(int value) {
		CU = (value&0xffff);
	}
	public boolean isStopped() {
		return running==false;
	}
	public void start() {
		running = true;
	}
	public int READ(int address) {
		return MEM[address&(MAX_MEM-1)];
	}
	public void WRITE(int address, int value) {
		MEM[address&(MAX_MEM-1)] = value&0xffff;
	}
	public void copyProgram(int[] program, int where){
		for (int i=0; i<program.length;i++){
			WRITE(where+i,program[i]);
		}
	}
	public int[] copyMemory(int start, int end){
		int[] out = new int[end-start];
		for(int i=0; i < end-start; i++){
			out[i]=READ(start+i);
		}
		return out;
	}
	public String toString(){
		StringBuilder out = new StringBuilder();
		out.append("A:"+String.format("0x%04x", A())+",");
		out.append("B:"+String.format("0x%04x", B())+",");
		out.append("C:"+String.format("0x%04x", C())+",");
		out.append("D:"+String.format("0x%04x", D())+",");
		out.append("PC:"+String.format("0x%04x", PC())+",");
		out.append("IR:"+String.format("0x%04x", IR())+",");
		out.append("MBR:"+String.format("0x%04x", MBR())+",");
		out.append("MAR:"+String.format("0x%04x", MAR())+",");
		out.append("CU:"+String.format("0x%04x", CU()));
		return out.toString();
	}
	private void reset() {
		MEM = new int[MAX_MEM];
		A(0);
		B(0);
		C(0);
		D(0);
		MBR(0);
		MAR(0);
		PC(0);
		IR(0);
		CU(0);

	}

	public void loadProgram(File selectedFile) throws IOException {
		if(selectedFile.length()%2L !=0 || selectedFile.length()>0x400)
			throw new IOException("Invalid File type");

		byte[] aBytes = new byte[(int) selectedFile.length()];
		int totalBytesRead = 0;
		BufferedInputStream input = new BufferedInputStream(new FileInputStream(selectedFile));
		while(totalBytesRead < aBytes.length){
			int left = aBytes.length - totalBytesRead;
			int bytesRead = input.read(aBytes, totalBytesRead, left); 
			if (bytesRead > 0){
				totalBytesRead = totalBytesRead + bytesRead;
			}
		}

		input.close();

		int[] aInt = new int[aBytes.length/2];
		for(int i=0;i<aInt.length;i++)
			aInt[i] = (aBytes[i*2]&255)<<8|aBytes[i*2+1]&255;

		this.reset();
		this.copyProgram(aInt, 0);
	}

	public void stop() {
		running = false;
	}
}
