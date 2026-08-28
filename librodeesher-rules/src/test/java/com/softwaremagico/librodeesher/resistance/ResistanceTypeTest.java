package com.softwaremagico.librodeesher.resistance;

import org.testng.Assert;
import org.testng.annotations.Test;

/** Verifies {@link ResistanceType#fromTag} and abbreviation lookup. */
@Test(groups = "resistance")
public class ResistanceTypeTest {

    @Test
    public void resolvesEverySpanishColumnName() {
        Assert.assertEquals(ResistanceType.fromTag("Veneno"), ResistanceType.POISON);
        Assert.assertEquals(ResistanceType.fromTag("Enfermedad"), ResistanceType.DISEASE);
        Assert.assertEquals(ResistanceType.fromTag("Canalización"), ResistanceType.CHANNELING);
    }

    @Test
    public void unknownTagResolvesToNull() {
        Assert.assertNull(ResistanceType.fromTag("Desconocido"));
    }

    @Test
    public void abbreviationLookupOnlyMatchesKnownAbbreviations() {
        Assert.assertTrue(ResistanceType.isResistanceAbbreviation("Vn"));
        Assert.assertFalse(ResistanceType.isResistanceAbbreviation("Xx"));
    }
}
