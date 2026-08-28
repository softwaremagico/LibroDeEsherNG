package com.softwaremagico.librodeesher.rules;

import com.softwaremagico.librodeesher.category.Category;
import com.softwaremagico.librodeesher.category.CategoryFactory;
import com.softwaremagico.librodeesher.culture.Culture;
import com.softwaremagico.librodeesher.culture.CultureFactory;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.magic.MagicSpellList;
import com.softwaremagico.librodeesher.magic.MagicSpellListFactory;
import com.softwaremagico.librodeesher.perk.Perk;
import com.softwaremagico.librodeesher.perk.PerkFactory;
import com.softwaremagico.librodeesher.profession.Profession;
import com.softwaremagico.librodeesher.profession.ProfessionFactory;
import com.softwaremagico.librodeesher.race.Race;
import com.softwaremagico.librodeesher.race.RaceFactory;
import com.softwaremagico.librodeesher.skill.Skill;
import com.softwaremagico.librodeesher.skill.SkillFactory;
import com.softwaremagico.librodeesher.training.Training;
import com.softwaremagico.librodeesher.training.TrainingFactory;
import com.softwaremagico.librodeesher.weapon.Weapon;
import com.softwaremagico.librodeesher.weapon.WeaponFactory;
import com.softwaremagico.librodeesher.weapon.WeaponType;

import java.util.List;

/**
 * Public read-only entry point for the migrated Rolemaster rule catalog.
 *
 * <p>The lower-level {@code *Factory} classes remain available individually, but this class provides
 * a single cohesive API for code that wants to consume the rule data without knowing which XML
 * file/factory owns each concept. It is intentionally stateless and Android-safe: all data is still
 * read via classpath resources by the underlying factories.</p>
 */
public final class RulesCatalog {

    private static final RulesCatalog INSTANCE = new RulesCatalog();

    private RulesCatalog() {
        // Singleton entry point, like the underlying XML factories.
    }

    public static RulesCatalog getInstance() {
        return INSTANCE;
    }

    public List<Category> getCategories() throws InvalidXmlElementException {
        return CategoryFactory.getInstance().getElements();
    }

    public Category getCategory(String id) throws InvalidXmlElementException {
        return CategoryFactory.getInstance().getElement(id);
    }

    public List<Skill> getSkills() throws InvalidXmlElementException {
        return SkillFactory.getInstance().getElements();
    }

    public Skill getSkill(String id) throws InvalidXmlElementException {
        return SkillFactory.getInstance().getElement(id);
    }

    public List<Perk> getPerks() throws InvalidXmlElementException {
        return PerkFactory.getInstance().getElements();
    }

    public Perk getPerk(String id) throws InvalidXmlElementException {
        return PerkFactory.getInstance().getElement(id);
    }

    public List<Weapon> getWeapons() throws InvalidXmlElementException {
        return WeaponFactory.getInstance().getElements();
    }

    public List<Weapon> getWeaponsByType(WeaponType type) throws InvalidXmlElementException {
        return WeaponFactory.getInstance().getWeaponsByType(type);
    }

    public Weapon getWeapon(String id) throws InvalidXmlElementException {
        return WeaponFactory.getInstance().getElement(id);
    }

    public List<Training> getTrainings() throws InvalidXmlElementException {
        return TrainingFactory.getInstance().getElements();
    }

    public Training getTraining(String id) throws InvalidXmlElementException {
        return TrainingFactory.getInstance().getElement(id);
    }

    public List<Profession> getProfessions() throws InvalidXmlElementException {
        return ProfessionFactory.getInstance().getElements();
    }

    public Profession getProfession(String id) throws InvalidXmlElementException {
        return ProfessionFactory.getInstance().getElement(id);
    }

    public List<MagicSpellList> getSpellLists() throws InvalidXmlElementException {
        return MagicSpellListFactory.getInstance().getElements();
    }

    public MagicSpellList getSpellList(String id) throws InvalidXmlElementException {
        return MagicSpellListFactory.getInstance().getElement(id);
    }

    public List<Race> getRaces() throws InvalidXmlElementException {
        return RaceFactory.getInstance().getElements();
    }

    public Race getRace(String id) throws InvalidXmlElementException {
        return RaceFactory.getInstance().getElement(id);
    }

    public List<Culture> getCultures() throws InvalidXmlElementException {
        return CultureFactory.getInstance().getElements();
    }

    public Culture getCulture(String id) throws InvalidXmlElementException {
        return CultureFactory.getInstance().getElement(id);
    }
}
