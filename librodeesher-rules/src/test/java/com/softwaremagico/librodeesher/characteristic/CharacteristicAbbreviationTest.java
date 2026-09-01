package com.softwaremagico.librodeesher.characteristic;

import org.testng.Assert;
import org.testng.annotations.Test;

/** Verifies {@link CharacteristicAbbreviation#fromTag}. */
@Test(groups = "characteristic")
public class CharacteristicAbbreviationTest {

    @Test
    public void resolvesEveryStandardTag() {
        Assert.assertEquals(CharacteristicAbbreviation.fromTag("Ag"), CharacteristicAbbreviation.AGILITY);
        Assert.assertEquals(CharacteristicAbbreviation.fromTag("Fu"), CharacteristicAbbreviation.STRENGTH);
        Assert.assertEquals(CharacteristicAbbreviation.fromTag("Ap"), CharacteristicAbbreviation.APPEARANCE);
    }

    @Test
    public void unknownOrNullTagResolvesToNone() {
        Assert.assertEquals(CharacteristicAbbreviation.fromTag("Xx"), CharacteristicAbbreviation.NONE);
        Assert.assertEquals(CharacteristicAbbreviation.fromTag(null), CharacteristicAbbreviation.NONE);
    }
}
