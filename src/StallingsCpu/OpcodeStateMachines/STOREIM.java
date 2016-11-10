package StallingsCpu.OpcodeStateMachines;

import StallingsCpu.StallingsCPU;

public class STOREIM extends BaseOpcode {

    public STOREIM(StallingsCPU parent) {
        super(parent, 3);
    }
    
    public void step(int counter) {
        switch(counter) {
            case 0: cpu.MBR(cpu.IR()&0x3FF); break;
            case 1: 
                switch(cpu.step_rn){
                    case 0: cpu.MBR(cpu.A()); break;
                    case 1: cpu.MBR(cpu.B()); break;
                    case 2: cpu.MBR(cpu.C()); break;
                    case 3: cpu.MBR(cpu.D()); break;
		}
                break;
            case 2: cpu.WRITE(cpu.MAR(),cpu.MBR()); break;
        }
    }
}