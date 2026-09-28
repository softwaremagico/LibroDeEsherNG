package com.softwaremagico.librodeesher.race;

import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.language.LanguageSlot;
import com.softwaremagico.librodeesher.language.Translations;
import com.softwaremagico.librodeesher.rules.RulesCatalog;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Verifies {@link RaceFactory} against the real generated {@code races.xml} data. */
@Test(groups = "raceFactory")
public class RaceFactoryTest {

    @Test
    public void readsRacesFromEveryEnabledModule() throws InvalidXmlElementException {
        Assert.assertTrue(RaceFactory.getInstance().getElements().size() >= 60);
    }

    @Test
    public void grayOrcHasExpectedRaceData() throws InvalidXmlElementException {
        final Race grayOrc = RaceFactory.getInstance().getElement("grayOrc");

        Assert.assertEquals(grayOrc.getName().getSpanish(), "Orco Gris");
        Assert.assertFalse(grayOrc.getCharacteristicBonuses().isEmpty());
        Assert.assertFalse(grayOrc.getResistanceBonuses().isEmpty());
        Assert.assertFalse(grayOrc.getProgressionRankValues().isEmpty());
        Assert.assertTrue(grayOrc.getExpectedLifeYears() > 0);
    }

    /**
     * The legacy {@code Race#setOtherSpecials} also read a flat skill/category bonus out of the
     * "ESPECIALES" section (a "+10\tAcechar" line) instead of keeping it as text, so every shipped
     * race bonus must reach the data files: 28 skill bonuses over 10 races and 2 category bonuses
     * (the "+10 Armas\u00b7Arrojadizas" of the two troglodyte races).
     */
    @Test
    public void raceBonusesAreMigratedInsteadOfBeingLeftAsSpecialText() throws InvalidXmlElementException {
        final Map<String, Integer> skillBonuses = new LinkedHashMap<>();
        final Map<String, Integer> categoryBonuses = new LinkedHashMap<>();
        for (final Race race : RaceFactory.getInstance().getElements()) {
            race.getSkillBonuses().forEach((id, bonus) -> skillBonuses.merge(race.getId() + "/" + id, bonus, Integer::sum));
            race.getCategoryBonuses().forEach((id, bonus) -> categoryBonuses.merge(race.getId() + "/" + id, bonus, Integer::sum));
        }

        Assert.assertEquals(categoryBonuses, Map.of("trogli/weaponsThrown", 10, "troglodytes/weaponsThrown", 10));
        Assert.assertEquals(skillBonuses.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList(), List.of(
                Map.entry("dyari/detectingEmboscadas", 20), Map.entry("dyari/hearing", 20), Map.entry("dyari/vista", 20),
                Map.entry("erlini/detectingEmboscadas", 20), Map.entry("erlini/hearing", 20), Map.entry("erlini/vista", 20),
                Map.entry("greyElf/hearing", 10),
                Map.entry("highElf/attunement", 20), Map.entry("highElf/hearing", 10),
                Map.entry("linaeri/detectingEmboscadas", 20), Map.entry("linaeri/hearing", 20), Map.entry("linaeri/vista", 20),
                Map.entry("loari/detectingEmboscadas", 20), Map.entry("loari/hearing", 20), Map.entry("loari/vista", 20),
                Map.entry("shuluri/detectingEmboscadas", 20), Map.entry("shuluri/hearing", 20), Map.entry("shuluri/vista", 20),
                Map.entry("trogli/abrirLocks", 10), Map.entry("trogli/contortion", 20),
                Map.entry("trogli/walkingByTheRopeSlack", 10),
                Map.entry("troglodytes/abrirLocks", 10), Map.entry("troglodytes/contortion", 20),
                Map.entry("troglodytes/walkingByTheRopeSlack", 10),
                Map.entry("woodElf/gamesOfHands", 10), Map.entry("woodElf/hearing", 10),
                Map.entry("woodElf/hiding", 10), Map.entry("woodElf/stalking", 10)));

        // A bonus line whose target resolves must not survive as free text in the "specials" list.
        // The ones that do survive (e.g. "+10\tReparaciones", a typo for no real skill, or "+30\t
        // Contra Frío y Calor (BD y TR)", a note about resistances) are exactly the ones the legacy
        // kept as text after logging them too.
        final List<String> stillText = RaceFactory.getInstance().getElements().stream()
                .flatMap(race -> race.getSpecials().stream())
                .map(special -> special.getText().getSpanish())
                .filter(text -> text.contains("\t") && text.split("\t").length == 2)
                .filter(text -> isBonusTargetKnown(text.split("\t")[1]))
                .toList();
        Assert.assertEquals(stillText, List.of());
    }

    /** Whether {@code spanishName} is a real skill or category in the shipped catalog. */
    private static boolean isBonusTargetKnown(String spanishName) {
        final String id = Translations.toEnglishId(spanishName);
        try {
            RulesCatalog.getInstance().getSkill(id);
            return true;
        } catch (final InvalidXmlElementException notASkill) {
            try {
                RulesCatalog.getInstance().getCategory(id);
                return true;
            } catch (final InvalidXmlElementException notACategory) {
                return false;
            }
        }
    }

    @Test(expectedExceptions = InvalidXmlElementException.class)
    public void unknownRaceIdThrows() throws InvalidXmlElementException {
        RaceFactory.getInstance().getElement("doesNotExist");
    }

    /**
     * Matches the legacy {@code ReadFilesTest#readRaceOptionalLanguages}, against the same 3 real
     * races.
     */
    @Test
    public void racesExposeTheirOptionalLanguageSlots() throws InvalidXmlElementException {
        final Race doppleganger = RaceFactory.getInstance().getElement("doppleganger");
        Assert.assertEquals(doppleganger.getOptionalRaceLanguages().size(), 1);
        final LanguageSlot slot = doppleganger.getOptionalRaceLanguages().get(0);
        Assert.assertEquals(slot.getStartingSpeakingRanks(), Integer.valueOf(0));
        Assert.assertEquals(slot.getStartingWritingRanks(), Integer.valueOf(0));
        Assert.assertEquals(slot.getMaxSpeakingRanks(), Integer.valueOf(10));
        Assert.assertEquals(slot.getMaxWritingRanks(), Integer.valueOf(10));

        final Race laan = RaceFactory.getInstance().getElement("laan");
        Assert.assertEquals(laan.getOptionalRaceLanguages().size(), 2);

        final Race punkari = RaceFactory.getInstance().getElement("punkari");
        Assert.assertEquals(punkari.getOptionalRaceLanguages().size(), 1);
    }
}
