import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;


public class Disassembler {
	public HashMap<Integer,String> opSet;
	public HashMap<Integer,String> regSet;
	
	/*
	 * Initialize the disassembler engine
	 */
	public Disassembler() {
		opSet = new HashMap<Integer,String>();
		regSet = new HashMap<Integer,String>();
		
		for(Opcode o: EnumSet.range(Opcode.STOP,Opcode.SUBIM)){
			opSet.put(new Integer(o.ordinal()), o.toString());
			System.out.println(""+o);
		}
		for(Register o: EnumSet.range(Register.R0,Register.R3)){
			regSet.put(new Integer(o.ordinal()), o.toString());
			System.out.println(""+o);
		}
	}
	
	/*
	 * Disassemble an instruction, individually
	 */
	public String dissasemble(int instruction) {
		StringBuilder out = new StringBuilder();
		
		int addr = instruction & 0x3ff;
		int rn = (instruction&0xC00)>>10;
		int rm = (instruction&0x300)>>8;
		int op = (instruction&0xf000)>>12;
		
		if (instruction > 0xffff)
			return "Not recognized";
		
		out.append(opSet.get(op));
		out.append(" ");
		if(op < 1){
			
		} else if(op < 5){
			out.append(regSet.get(rn));
			out.append(", ");
			out.append(String.format("[%03xH]",addr));
		}
		else {
			return "Not Implemented";
		}
		
		return out.toString();
		
	}
}
