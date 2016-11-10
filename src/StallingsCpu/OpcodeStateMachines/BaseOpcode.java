package StallingsCpu.OpcodeStateMachines;

import StallingsCpu.StallingsCPU;

public class BaseOpcode {
    protected StallingsCPU cpu;
    
    public int stateMax;
    
    public BaseOpcode(StallingsCPU parent, int steps){
        cpu = parent;
        stateMax = steps;
    }
    
    public void step(int counter){};
    
    
}
