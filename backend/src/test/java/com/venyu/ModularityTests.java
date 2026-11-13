package com.venyu;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

    ApplicationModules modules = ApplicationModules.of(VenyuApplication.class);

    @Test
    void verifiesModularStructure() {
        modules.verify();
    }
}
