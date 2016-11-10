package StallingsCpu.OpcodeStateMachines;

import StallingsCpu.StallingsCPU;

public class ADDIM extends BaseOpcode {

    public ADDIM(StallingsCPU parent) {
        super(parent, 2);
    }

    public void step(int counter) {
        switch(counter) {
            case 0: cpu.MBR(cpu.IR()&0x3FF); break;
            case 1: 
                switch(cpu.step_rn){
                    case 0: cpu.A(cpu.A()+cpu.MBR()); break;
                    case 1: cpu.B(cpu.B()+cpu.MBR()); break;
                    case 2: cpu.C(cpu.C()+cpu.MBR()); break;
                    case 3: cpu.D(cpu.D()+cpu.MBR()); break;
		}
                break;
        }
    }
}
