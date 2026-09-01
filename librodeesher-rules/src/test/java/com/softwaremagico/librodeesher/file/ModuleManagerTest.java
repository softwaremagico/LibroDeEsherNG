package com.softwaremagico.librodeesher.file;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Verifies module enable/disable bookkeeping and the reset-callback registry used by
 * {@link com.softwaremagico.librodeesher.xml.XmlFactory} to invalidate its cache.
 */
@Test(groups = "moduleManager")
public class ModuleManagerTest {

    @Test
    public void allTwentyRulebookModulesAreKnown() {
        Assert.assertEquals(ModuleManager.getAllModules().size(), 20);
        Assert.assertTrue(ModuleManager.getAllModules().contains(ModuleManager.CORE));
        Assert.assertTrue(ModuleManager.getAllModules().contains(ModuleManager.RACES_AND_CULTURES));
    }

    @Test
    public void allModulesAreEnabledByDefault() {
        Assert.assertTrue(ModuleManager.getEnabledModules().containsAll(ModuleManager.getAllModules()));
    }

    @Test
    public void disablingAModuleRemovesItFromTheEnabledSet() {
        ModuleManager.disableModule(ModuleManager.PULP);
        try {
            Assert.assertFalse(ModuleManager.getEnabledModules().contains(ModuleManager.PULP));
        } finally {
            ModuleManager.enableModule(ModuleManager.PULP);
        }
    }

    @Test
    public void resetModulesInvokesEveryRegisteredCallback() {
        final boolean[] invoked = {false};
        ModuleManager.registerResettable(() -> invoked[0] = true);

        ModuleManager.resetModules();

        Assert.assertTrue(invoked[0]);
    }
}
