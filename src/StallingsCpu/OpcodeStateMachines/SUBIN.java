package StallingsCpu.OpcodeStateMachines;

import StallingsCpu.StallingsCPU;

public class SUBIN extends BaseOpcode {

    public SUBIN(StallingsCPU parent) {
        super(parent, 3);
    }

    public void step(int counter) {
        switch(counter) {
            case 0: cpu.MAR(cpu.step_bufm); break;
            case 1: cpu.MBR(cpu.READ(cpu.MAR())); break;
            case 2: 
                switch(cpu.step_rn){
                    case 0: cpu.A(cpu.A()-cpu.MBR()); break;
                    case 1: cpu.B(cpu.B()-cpu.MBR()); break;
                    case 2: cpu.C(cpu.C()-cpu.MBR()); break;
                    case 3: cpu.D(cpu.D()-cpu.MBR()); break;
		}
                break;
        }
    }
}