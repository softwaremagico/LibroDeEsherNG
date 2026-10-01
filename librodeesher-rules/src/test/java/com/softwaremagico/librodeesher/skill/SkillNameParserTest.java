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

    @Test
    public void plainNameAgreesWithTheParsedSkillName() {
        for (final String rawToken : new String[]{"Nadar", "Xeno-Conocimientos*", "Control de la Licantropía (R)",
                "Estilo de la Grulla {Poderes Chi: Ataque Sin Sombra | Poderes Chi: Golpes Contínuos}",
                "Artes Marciales·Barridos [algo; otra]", "*  Ley del Fuego", "Nadar (p)", "Nadar (c)"}) {
            Assert.assertEquals(SkillNameParser.plainName(rawToken),
                    SkillNameParser.parse(rawToken).getName().getSpanish(),
                    "plainName must return the very same name parse() stores for: " + rawToken);
        }
    }

    /**
     * A raw token quoted outside a skills column (e.g. a category's "Habilidades" cell) has to go
     * through {@link #plainName} before being turned into an id: leaving the enable-skills block glued
     * on, or the "(R)" suffix attached, makes the phrase lookup in {@code Translations} miss and yields
     * a per-word id no skill actually has.
     */
    @Test
    public void plainNameStripsEverythingButTheName() {
        Assert.assertEquals(SkillNameParser.plainName("Estilo de la Grulla {Poderes Chi: Ataque Sin Sombra | "
                + "Poderes Chi: Golpes Contínuos}"), "Estilo de la Grulla");
        Assert.assertEquals(SkillNameParser.plainName("Poderes Chi: Contacto Contínuo (R)"), "Poderes Chi: Contacto Contínuo");
        Assert.assertEquals(SkillNameParser.plainName("*  Ley del Fuego"), "Ley del Fuego");
        Assert.assertEquals(SkillNameParser.plainName("Artes Marciales·Barridos [algo; otra]"), "Artes Marciales·Barridos");
    }
}
