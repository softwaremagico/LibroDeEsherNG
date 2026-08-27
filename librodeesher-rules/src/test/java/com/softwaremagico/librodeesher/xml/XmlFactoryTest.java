package com.softwaremagico.librodeesher.xml;

import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.file.ModuleManager;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Verifies the generic behaviour of {@link XmlFactory}: reading a fixture XML file from the
 * classpath, merging elements coming from several modules (with "last enabled module wins" on id
 * collisions), and cache invalidation through {@link XmlFactory#reset()}.
 *
 * <p>Uses the test-only {@link SampleElementFactory}/{@link SampleElement} pair and two fixture
 * modules under {@code src/test/resources/modulo/}, so this test does not depend on any real
 * rulebook data (added later, once the migration tool has generated it).</p>
 */
@Test(groups = "xmlFactory")
public class XmlFactoryTest {

    private static final String MODULE_A = "EjemploTestA";
    private static final String MODULE_B = "EjemploTestB";

    @AfterMethod(alwaysRun = true)
    public void disableFixtureModules() {
        ModuleManager.disableModule(MODULE_A);
        ModuleManager.disableModule(MODULE_B);
        SampleElementFactory.getInstance().reset();
    }

    @Test
    public void readsElementsFromASingleModule() throws InvalidXmlElementException {
        ModuleManager.enableModule(MODULE_A);

        final List<SampleElement> elements = SampleElementFactory.getInstance().getElements();

        Assert.assertEquals(elements.size(), 2);
        Assert.assertEquals(SampleElementFactory.getInstance().getElement("alpha").getValue(), 1);
        Assert.assertEquals(SampleElementFactory.getInstance().getElement("beta").getValue(), 2);
    }

    @Test
    public void laterModuleOverridesElementsWithTheSameId() throws InvalidXmlElementException {
        ModuleManager.enableModule(MODULE_A);
        ModuleManager.enableModule(MODULE_B);

        final List<SampleElement> elements = SampleElementFactory.getInstance().getElements();

        // "alpha" is defined by both modules: module B (enabled after A) must win.
        Assert.assertEquals(elements.size(), 3);
        Assert.assertEquals(SampleElementFactory.getInstance().getElement("alpha").getValue(), 99);
        Assert.assertEquals(SampleElementFactory.getInstance().getElement("gamma").getValue(), 3);
    }

    @Test(expectedExceptions = InvalidXmlElementException.class)
    public void unknownIdThrowsInvalidXmlElementException() throws InvalidXmlElementException {
        ModuleManager.enableModule(MODULE_A);

        SampleElementFactory.getInstance().getElement("does-not-exist");
    }

    @Test
    public void resetForcesElementsToBeReReadOnNextAccess() throws InvalidXmlElementException {
        ModuleManager.enableModule(MODULE_A);
        Assert.assertEquals(SampleElementFactory.getInstance().getElements().size(), 2);

        ModuleManager.enableModule(MODULE_B);
        SampleElementFactory.getInstance().reset();

        Assert.assertEquals(SampleElementFactory.getInstance().getElements().size(), 3);
    }
}
