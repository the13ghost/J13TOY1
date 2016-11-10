package StallingsCpu.OpcodeStateMachines;

import StallingsCpu.StallingsCPU;

public class GOTO extends BaseOpcode {

    public GOTO(StallingsCPU parent) {
        super(parent, 2);
    }
    
    public void step(int counter) {
        switch(counter){
            case 0: cpu.MBR(cpu.IR()&0x3FF); break;
            case 1: cpu.PC(cpu.MBR()); break;
        } 
    }
}
