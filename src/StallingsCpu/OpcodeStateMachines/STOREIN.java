package StallingsCpu.OpcodeStateMachines;

import StallingsCpu.StallingsCPU;

public class STOREIN extends BaseOpcode {

    public STOREIN(StallingsCPU parent) {
        super(parent, 4);
    }
    
    public void step(int counter) {
        switch(counter) {
            case 0: cpu.MAR(cpu.step_bufm); break;
            case 1: cpu.MBR(cpu.READ(cpu.MAR())); break;
            case 2: 
                switch(cpu.step_rn){
                    case 0: cpu.MBR(cpu.A()); break;
                    case 1: cpu.MBR(cpu.B()); break;
                    case 2: cpu.MBR(cpu.C()); break;
                    case 3: cpu.MBR(cpu.D()); break;
		}
                break;
            case 3: cpu.WRITE(cpu.MAR(),cpu.MBR()); break;
        }
    }
}