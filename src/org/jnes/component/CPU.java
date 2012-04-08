package org.jnes.component;

import org.jnes.NESSystem;


public interface CPU {

    void setSystem(NESSystem system);

    void reset();

    int run(int cycles);
    
    void nmi();
    
    void irq();
}
