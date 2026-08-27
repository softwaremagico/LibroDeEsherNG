package com.softwaremagico.librodeesher.rules;

import com.softwaremagico.librodeesher.category.Category;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.profession.Profession;
import com.softwaremagico.librodeesher.race.Race;
import com.softwaremagico.librodeesher.skill.Skill;
import com.softwaremagico.librodeesher.training.Training;

import java.util.List;
import java.util.Optional;

/** Resolves id references embedded in rule objects to the concrete rule objects they point to. */
public final class RuleReferenceResolver {

    private final RulesCatalog catalog;

    public RuleReferenceResolver() {
        this(RulesCatalog.getInstance());
    }

    public RuleReferenceResolver(RulesCatalog catalog) {
        this.catalog = catalog;
    }

    /** Resolves every category referenced by a weapon/training/race id field. */
    public Category category(String categoryId) {
        return required("category", categoryId, () -> catalog.getCategory(categoryId));
    }

    public Skill skill(String skillId) {
        return required("skill", skillId, () -> catalog.getSkill(skillId));
    }

    public Profession profession(String professionId) {
        return required("profession", professionId, () -> catalog.getProfession(professionId));
    }

    public Training training(String trainingId) {
        return required("training", trainingId, () -> catalog.getTraining(trainingId));
    }

    public Race race(String raceId) {
        return required("race", raceId, () -> catalog.getRace(raceId));
    }

    public List<Category> categories(List<String> categoryIds) {
        return categoryIds.stream().map(this::category).toList();
    }

    public List<Skill> skills(List<String> skillIds) {
        return skillIds.stream().map(this::skill).toList();
    }

    /** Optional lookup helper for UI/search flows where absence is not exceptional. */
    public Optional<Category> findCategory(String categoryId) {
        try {
            return Optional.of(catalog.getCategory(categoryId));
        } catch (InvalidXmlElementException e) {
            return Optional.empty();
        }
    }

    private <T> T required(String type, String id, XmlLookup<T> lookup) {
        try {
            return lookup.get();
        } catch (InvalidXmlElementException e) {
            throw new InvalidRuleReferenceException("Unknown " + type + " id '" + id + "'.");
        }
    }

    @FunctionalInterface
    private interface XmlLookup<T> {
        T get() throws InvalidXmlElementException;
    }
}
