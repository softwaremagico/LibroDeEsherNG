package com.softwaremagico.librodeesher.skill;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Verifies {@link SkillNameParser} against the legacy mini-syntax found in rulebook text files:
 * rare marker ("*"), skill type suffixes ("(r)"/"(p)"/"(c)"), specialities ("[...]") and
 * enable-skills ("{...}", with "|" for OR and "&" for AND).
 */
@Test(groups = "skillNameParser")
public class SkillNameParserTest {

    @Test
    public void parsesAPlainSkillName() {
        final Skill skill = SkillNameParser.parse("Nadar");

        Assert.assertEquals(skill.getId(), "Nadar");
        Assert.assertFalse(skill.isRare());
        Assert.assertEquals(skill.getSkillType(), SkillType.STANDARD);
        Assert.assertTrue(skill.getSpecialities().isEmpty());
        Assert.assertTrue(skill.getEnableSkills().isEmpty());
    }

    @Test
    public void parsesARareSkill() {
        final Skill skill = SkillNameParser.parse("Xeno-Conocimientos*");

        Assert.assertEquals(skill.getId(), "Xeno-Conocimientos");
        Assert.assertTrue(skill.isRare());
    }

    @Test
    public void parsesARestrictedSkillType() {
        final Skill skill = SkillNameParser.parse("Control de la Licantropía (R)");

        Assert.assertEquals(skill.getId(), "Control de la Licantropía");
        Assert.assertEquals(skill.getSkillType(), SkillType.RESTRICTED);
    }

    @Test
    public void parsesSpecialitiesSeparatedBySemicolons() {
        final Skill skill = SkillNameParser.parse("Cuero Endurecido [TA9; TA10; TA11]");

        Assert.assertEquals(skill.getId(), "Cuero Endurecido");
        Assert.assertEquals(skill.getSpecialities(), List.of("TA9", "TA10", "TA11"));
    }

    @Test
    public void parsesEnableSkillsWithOrSemantics() {
        final Skill skill = SkillNameParser.parse("Frenesí {Furia Adrenal|Calma Adrenal}");

        Assert.assertEquals(skill.getId(), "Frenesí");
        Assert.assertEquals(skill.getEnableSkills(), List.of("Furia Adrenal", "Calma Adrenal"));
        Assert.assertFalse(skill.isAllEnabled());
    }

    @Test
    public void parsesEnableSkillsWithAndSemantics() {
        final Skill skill = SkillNameParser.parse("Trance Sanador {Trance de la Muerte&Trance Purificador}");

        Assert.assertEquals(skill.getEnableSkills(), List.of("Trance de la Muerte", "Trance Purificador"));
        Assert.assertTrue(skill.isAllEnabled());
    }

    @Test
    public void detectsChiSkillGroupFromPrefix() {
        final Skill skill = SkillNameParser.parse("Poderes Chi: Golpe de Palma");

        Assert.assertEquals(skill.getSkillGroup(), SkillGroup.CHI);
    }

    @Test
    public void detectsFirearmSkillGroupFromPrefix() {
        final Skill skill = SkillNameParser.parse("Fuego Rápido");

        Assert.assertEquals(skill.getSkillGroup(), SkillGroup.FIREARM);
    }
}
