package StallingsCpu.OpcodeStateMachines;

import StallingsCpu.StallingsCPU;

public class IFZER extends BaseOpcode {

    public IFZER(StallingsCPU parent) {
        super(parent, 3);
    }
    
    public void step(int counter){
        switch(counter){
            case 0: cpu.MBR(cpu.step_bufn); break;
            case 1: cpu.MAR(cpu.IR()&0x3FF); break;
            case 2: if(cpu.MBR()==0) cpu.PC(cpu.MAR());
                break;
        }
    }
}
