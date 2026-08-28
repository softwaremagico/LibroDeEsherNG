package com.softwaremagico.librodeesher.culture;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/** Verifies {@link Culture#isHobbySkillAllowed}. */
@Test(groups = "culture")
public class CultureTest {

    @Test
    public void explicitlyListedSkillIsAllowed() {
        final Culture culture = new Culture("test");
        culture.setHobbyIds(List.of("tracking", "stalking"));

        Assert.assertTrue(culture.isHobbySkillAllowed("tracking"));
        Assert.assertFalse(culture.isHobbySkillAllowed("hunting"));
    }

    @Test
    public void allMarkerAllowsAnySkill() {
        final Culture culture = new Culture("test");
        culture.setHobbyIds(List.of("all"));

        Assert.assertTrue(culture.isHobbySkillAllowed("hunting"));
        Assert.assertTrue(culture.isHobbySkillAllowed("anySkillAtAll"));
    }

    @Test
    public void excludeMarkerWinsOverTheAllMarker() {
        final Culture culture = new Culture("test");
        culture.setHobbyIds(List.of("all", "exclude:stalking"));

        Assert.assertTrue(culture.isHobbySkillAllowed("hunting"));
        Assert.assertFalse(culture.isHobbySkillAllowed("stalking"));
    }

    @Test
    public void noHobbyIdsAllowsNothing() {
        final Culture culture = new Culture("test");
        Assert.assertFalse(culture.isHobbySkillAllowed("hunting"));
    }
}
