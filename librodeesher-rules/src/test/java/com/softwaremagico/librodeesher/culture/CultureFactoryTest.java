package com.softwaremagico.librodeesher.culture;

import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Verifies {@link CultureFactory} against real generated culture data. */
@Test(groups = "cultureFactory")
public class CultureFactoryTest {

    @Test
    public void readsCulturesFromEnabledModules() throws InvalidXmlElementException {
        Assert.assertTrue(CultureFactory.getInstance().getElements().size() >= 50);
    }

    @Test
    public void readsPulpIndustrialCulture() throws InvalidXmlElementException {
        final Culture culture = CultureFactory.getInstance().getElement("industrializedRuralClassHigh");

        Assert.assertEquals(culture.getName().getSpanish(), "Industrializado (Rural Clase Alta)");
        Assert.assertFalse(culture.getAdolescenceRanks().isEmpty());
        Assert.assertEquals(culture.getHobbyRanks(), Integer.valueOf(10));
    }

    @Test(expectedExceptions = InvalidXmlElementException.class)
    public void unknownCultureIdThrows() throws InvalidXmlElementException {
        CultureFactory.getInstance().getElement("doesNotExist");
    }

    /** Matches the legacy {@code ReadFilesTest#readCulturalOptionalLanguages}. */
    @Test
    public void cultureExposesItsOptionalLanguageSlots() throws InvalidXmlElementException {
        final Culture culture = CultureFactory.getInstance().getElement("undergroundUrbanClassHigh");
        Assert.assertEquals(culture.getOptionalLanguages().size(), 1);
    }
}
