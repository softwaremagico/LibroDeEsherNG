package com.softwaremagico.librodeesher.rules;

import com.softwaremagico.librodeesher.category.Category;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.race.Race;
import com.softwaremagico.librodeesher.skill.Skill;
import com.softwaremagico.librodeesher.weapon.Weapon;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Verifies the high-level catalog and reference resolver over real generated XML data. */
@Test(groups = "rulesCatalog")
public class RulesCatalogTest {

    @Test
    public void exposesAllMajorRuleGroups() throws InvalidXmlElementException {
        final RulesCatalog catalog = RulesCatalog.getInstance();

        Assert.assertFalse(catalog.getCategories().isEmpty());
        Assert.assertFalse(catalog.getSkills().isEmpty());
        Assert.assertFalse(catalog.getPerks().isEmpty());
        Assert.assertFalse(catalog.getWeapons().isEmpty());
        Assert.assertFalse(catalog.getTrainings().isEmpty());
        Assert.assertFalse(catalog.getProfessions().isEmpty());
        Assert.assertFalse(catalog.getSpellLists().isEmpty());
        Assert.assertFalse(catalog.getRaces().isEmpty());
    }

    @Test
    public void resolvesDirectCategoryAndSkillReferences() throws InvalidXmlElementException {
        final RulesCatalog catalog = RulesCatalog.getInstance();
        final RuleReferenceResolver resolver = new RuleReferenceResolver(catalog);

        final Skill estiloDeLaGrulla = catalog.getSkill("styleOfTheCrane");
        final Category martialArtsManeuvers = resolver.category(estiloDeLaGrulla.getCategoryId());

        Assert.assertEquals(martialArtsManeuvers.getId(), "martialArtsCombatManeuvers");
        Assert.assertEquals(resolver.skill("chiPowerShadowlessAttack").getId(), "chiPowerShadowlessAttack");
    }

    @Test
    public void resolvesWeaponCategoryReference() throws InvalidXmlElementException {
        final RulesCatalog catalog = RulesCatalog.getInstance();
        final RuleReferenceResolver resolver = new RuleReferenceResolver(catalog);

        final Weapon dagger = catalog.getWeapon("dagger");

        Assert.assertEquals(resolver.category(dagger.getCategoryId()).getId(), "weaponsEdged");
    }

    @Test
    public void readsRaceThroughCatalog() throws InvalidXmlElementException {
        final Race grayOrc = RulesCatalog.getInstance().getRace("grayOrc");

        Assert.assertEquals(grayOrc.getName().getSpanish(), "Orco Gris");
        Assert.assertFalse(grayOrc.getCharacteristicBonuses().isEmpty());
    }

    @Test(expectedExceptions = InvalidRuleReferenceException.class)
    public void missingReferenceThrowsRuleException() {
        new RuleReferenceResolver().category("missingCategory");
    }
}
