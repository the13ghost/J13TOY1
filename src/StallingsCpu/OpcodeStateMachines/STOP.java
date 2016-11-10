package StallingsCpu.OpcodeStateMachines;

import StallingsCpu.StallingsCPU;

/**
 * Currently a NOP on the Stepped
 */
public class STOP extends BaseOpcode {
    
    public STOP(StallingsCPU parent) {
        super(parent, 1);
    }
}
