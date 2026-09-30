package com.softwaremagico.librodeesher.persistence;

import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.character.SexType;
import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.decision.DecisionKey;
import com.softwaremagico.librodeesher.decision.DecisionKind;
import com.softwaremagico.librodeesher.equipment.BonusType;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.rules.RulesCatalog;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Verifies {@link LegacyCharacterJsonImporter} imports the reference legacy JSON snapshot (a level
 * 9 character saved in the old 2.0.0 format) into a {@link CharacterData} whose totals match the
 * reference bookkeeping and that {@link CharacterDataMapper} rebuilds byte-identically.
 */
@Test(groups = "persistence")
public class LegacyCharacterJsonImporterTest {

    private static final String REFERENCE_SNAPSHOT = "/Nihal_level9_2_0_0.json";

    @Test
    public void importsTheReferenceLevelNineSnapshot() throws IOException, InvalidXmlElementException {
        final CharacterData data = LegacyCharacterJsonImporter.importLegacyJson(readReferenceSnapshot());

        Assert.assertEquals(data.getName(), "Nihal Dragoniana");
        Assert.assertEquals(data.getSex(), SexType.FEMALE);
        Assert.assertEquals(data.getRaceId(), "laan");
        Assert.assertEquals(data.getCultureId(), "coastalUrbanClassHigh");
        Assert.assertEquals(data.getProfessionId(), "lordOfTheChaos");

        Assert.assertEquals(data.getLevels().size(), 9);
        Assert.assertEquals(data.getCurrentAge(), 10);
        Assert.assertEquals(data.getFinalAge(), 10);
        for (final LevelData level : data.getLevels()) {
            Assert.assertEquals(level.getAge(), 10);
        }

        Assert.assertEquals(data.getCharacteristicTemporalValues().get("AGILITY"), Integer.valueOf(93));
        Assert.assertEquals(data.getCharacteristicTemporalValues().get("SELF_DISCIPLINE"), Integer.valueOf(97));
        Assert.assertEquals(data.getCharacteristicTemporalValues().get("STRENGTH"), Integer.valueOf(100));
        Assert.assertEquals(data.getCharacteristicTemporalValues().get("APPEARANCE"), Integer.valueOf(0));
        Assert.assertEquals(data.getCharacteristicPotentialValues().get("CONSTITUTION"), Integer.valueOf(97));
        Assert.assertEquals(data.getCharacteristicPotentialValues().get("APPEARANCE"), Integer.valueOf(0));
        Assert.assertTrue(data.isCharacteristicsConfirmed());
        Assert.assertEquals(data.getAppearance(), 29);

        // Level 1: the culture's adolescence grants materialized as fixed ranks.
        final LevelData levelOne = data.getLevels().get(0);
        Assert.assertEquals(levelOne.getCategoryRanks().get("armorLight"), Integer.valueOf(1));
        Assert.assertEquals(levelOne.getCategoryRanks().get("weaponsEdged"), Integer.valueOf(2));
        Assert.assertEquals(levelOne.getCategoryRanks().get("weaponsThrown"), Integer.valueOf(1));
        Assert.assertEquals(levelOne.getCategoryRanks().get("urban"), Integer.valueOf(2));
        Assert.assertEquals(levelOne.getCategoryRanks().get("loreGeneral"), Integer.valueOf(3));
        Assert.assertEquals(levelOne.getSkillRanks().get("hardenedLeather"), Integer.valueOf(1));
        Assert.assertEquals(levelOne.getSkillRanks().get("swimming"), Integer.valueOf(5));
        Assert.assertEquals(levelOne.getSkillRanks().get("physicalDevelopment"), Integer.valueOf(2));
        Assert.assertEquals(levelOne.getSpellsUpdated(), List.of("essenceBarrierAgainstSpells",
                "essenceIlusionesMenores", "essencePathsOfApertura", "essencePathsOfDetection",
                "essenceWeaponOfTheChaos", "essenceArmorOfTheChaos", "essenceSummoningsOscuras",
                "essenceMasteryOfChaos", "essenceOscuridad"));

        for (int i = 1; i <= 5; i++) {
            Assert.assertTrue(data.getLevels().get(i).getCategoryRanks().isEmpty());
            Assert.assertTrue(data.getLevels().get(i).getSkillRanks().isEmpty());
        }

        // Level 7: the legacy "inserted" development.
        final LevelData inserted = data.getLevels().get(6);
        Assert.assertEquals(inserted.getCategoryRanks().get("weaponsEdged"), Integer.valueOf(7));
        Assert.assertEquals(inserted.getCategoryRanks().get("weaponsThrown"), Integer.valueOf(7));
        Assert.assertEquals(inserted.getCategoryRanks().get("loreDark"), Integer.valueOf(7));
        Assert.assertEquals(inserted.getCategoryRanks().get("powerAwareness"), Integer.valueOf(4));
        Assert.assertEquals(inserted.getSkillRanks().get("sword"), Integer.valueOf(7));
        Assert.assertEquals(inserted.getSkillRanks().get("bolas"), Integer.valueOf(8));
        Assert.assertEquals(inserted.getSkillRanks().get("physicalDevelopment"), Integer.valueOf(7));
        Assert.assertEquals(inserted.getSpellListRanks().get("essenceArmorOfTheChaos"), Integer.valueOf(10));
        Assert.assertEquals(inserted.getSpellListRanks().get("essenceBarrierAgainstSpells"), Integer.valueOf(2));
        Assert.assertEquals(inserted.getSpellListRanks().get("essenceSummoningsOscuras"), Integer.valueOf(8));
        Assert.assertEquals(inserted.getSpellListRanks().get("essenceWeaponOfTheChaos"), Integer.valueOf(7));

        // Level 8: the first legacy level-up.
        final LevelData levelEight = data.getLevels().get(7);
        Assert.assertEquals(levelEight.getCategoryRanks().get("weaponsEdged"), Integer.valueOf(1));
        Assert.assertEquals(levelEight.getSkillRanks().get("sword"), Integer.valueOf(1));
        Assert.assertEquals(levelEight.getSpellListRanks().get("essenceMasteryOfChaos"), Integer.valueOf(3));
        Assert.assertEquals(levelEight.getSpellListRanks().get("essencePathsOfDetection"), Integer.valueOf(2));
        Assert.assertEquals(levelEight.getSpellListRanks().get("essenceArmorOfTheChaos"), Integer.valueOf(1));
        Assert.assertEquals(levelEight.getSpellsUpdated(),
                List.of("essenceMasteryOfChaos", "essenceArmorOfTheChaos", "essencePathsOfDetection"));
        Assert.assertEquals(levelEight.getFavouriteSkills(), List.of("bolas", "essenceArmorOfTheChaos",
                "essenceMasteryOfChaos", "essenceOscuridad", "essenceWeaponOfTheChaos", "liderazgo", "sword"));
        Assert.assertEquals(levelEight.getCharacteristicUpdates().size(), 10);
        Assert.assertEquals(levelEight.getCharacteristicUpdates().get(0).getCharacteristicAbbreviation(),
                CharacteristicAbbreviation.AGILITY);
        Assert.assertEquals(levelEight.getCharacteristicUpdates().get(0).getCharacteristicTemporalValue(),
                Integer.valueOf(93));
        Assert.assertEquals(levelEight.getCharacteristicUpdates().get(0).getRoll().getFirstDice(), 8);
        Assert.assertEquals(levelEight.getCharacteristicUpdates().get(0).getRoll().getSecondDice(), 9);

        // Level 9: the second legacy level-up.
        final LevelData levelNine = data.getLevels().get(8);
        Assert.assertEquals(levelNine.getSpellListRanks().get("essenceArmorOfTheChaos"), Integer.valueOf(3));
        Assert.assertEquals(levelNine.getSpellListRanks().get("essenceOscuridad"), Integer.valueOf(1));
        Assert.assertEquals(levelNine.getSpellListRanks().get("essencePathsOfApertura"), Integer.valueOf(2));
        Assert.assertEquals(levelNine.getSpellsUpdated(),
                List.of("essenceOscuridad", "essencePathsOfApertura", "essenceArmorOfTheChaos"));
        Assert.assertEquals(levelNine.getCharacteristicUpdates().size(), 10);

        // The nine weapon cost tiers become character-wide decisions, filed by key.
        final Map<String, DecisionData> decisions = new TreeMap<>(data.getDecisions());
        Assert.assertEquals(decisions.size(), 9);
        final List<String> categoryByTier = List.of("weaponsEdged", "weaponsThrown", "weaponsBlunt",
                "weaponsTwoHanded", "weaponsPolearm", "weaponsMissile", "weaponsSiege",
                "weaponsFirearmOneHanded", "weaponsFirearmTwoHanded");
        for (int tier = 0; tier < 9; tier++) {
            final String key = DecisionKey.characterWide(DecisionKind.WEAPON_COST_TIER, "", tier).toString();
            final DecisionData decision = decisions.get(key);
            Assert.assertNotNull(decision, key);
            Assert.assertEquals(decision.getRecordedAtLevel(), 1);
            Assert.assertEquals(decision.getOfferedOptions(), List.of(categoryByTier.get(tier)));
            Assert.assertEquals(decision.getSelectedOptions(), List.of(categoryByTier.get(tier)));
        }

        Assert.assertEquals(data.getSelectedPerks().size(), 5);
        Assert.assertEquals(data.getSelectedPerks().get(0).getPerkId(), "herbExpert");
        Assert.assertEquals(data.getSelectedPerks().get(1).getPerkId(), "timeSkills");
        Assert.assertEquals(data.getSelectedPerks().get(2).getPerkId(), "intolerant");
        Assert.assertEquals(data.getSelectedPerks().get(3).getPerkId(), "leadershipSkills");
        Assert.assertEquals(data.getSelectedPerks().get(4).getPerkId(), "survivalInstinct");

        Assert.assertEquals(data.getBackground().getCategoryIds(), List.of("influence"));
        Assert.assertEquals(data.getBackground().getSkillIds(),
                List.of("bolas", "sword", "physicalDevelopment", "powerPointDevelopment"));

        Assert.assertEquals(data.getMagicItems().size(), 3);
        Assert.assertEquals(data.getMagicItems().get(0).getName().getSpanish(), "Espada");
        Assert.assertEquals(data.getMagicItems().get(0).getSkillBonus("sword"), 20);
        Assert.assertEquals(data.getMagicItems().get(1).getName().getSpanish(), "Botiquín");
        Assert.assertEquals(data.getMagicItems().get(1).getSkillBonus("primerosAuxilios"), 15);
        Assert.assertEquals(data.getMagicItems().get(2).getName().getSpanish(), "Armadura del Caos");
        Assert.assertEquals(data.getMagicItems().get(2).getObjectBonus(BonusType.DEFENSIVE_BONUS), 60);

        Assert.assertTrue(data.isFirearmsAllowed());
        Assert.assertTrue(data.isMagicAllowed());
        Assert.assertFalse(data.isChiPowersAllowed());
        Assert.assertFalse(data.isOtherRealmTrainingSpellsAllowed());
        Assert.assertFalse(data.isDarkSpellsAsBasicListsAllowed());
        Assert.assertFalse(data.isRecommendedFavouriteSkillsIncluded());
    }

    @Test
    public void rebuiltCharacterTotalsMatchTheReferenceBook() throws IOException, InvalidXmlElementException {
        final CharacterPlayer character = CharacterDataMapper.toCharacter(
                LegacyCharacterJsonImporter.importLegacyJson(readReferenceSnapshot()));

        Assert.assertEquals(character.getLevel(), 9);
        Assert.assertEquals(character.getAgeAtLevel(9), 10);

        int categoriesWithRanks = 0;
        int categoryRanks = 0;
        for (final com.softwaremagico.librodeesher.Element category : RulesCatalog.getInstance().getCategories()) {
            final int ranks = character.getCategoryTotalRanks(category.getId());
            if (ranks > 0) {
                categoriesWithRanks++;
                categoryRanks += ranks;
            }
        }
        Assert.assertEquals(categoriesWithRanks, 22);
        Assert.assertEquals(categoryRanks, 96);

        int skillsWithRanks = 0;
        int skillRanks = 0;
        for (final com.softwaremagico.librodeesher.Element skill : RulesCatalog.getInstance().getSkills()) {
            final int ranks = character.getSkillTotalRanks(skill.getId());
            if (ranks > 0) {
                skillsWithRanks++;
                skillRanks += ranks;
            }
        }
        Assert.assertEquals(skillsWithRanks, 59);
        Assert.assertEquals(skillRanks, 135);

        int listsWithRanks = 0;
        int listRanks = 0;
        for (final com.softwaremagico.librodeesher.Element list : RulesCatalog.getInstance().getSpellLists()) {
            final int ranks = character.getSpellListTotalRanks(list.getId());
            if (ranks > 0) {
                listsWithRanks++;
                listRanks += ranks;
            }
        }
        Assert.assertEquals(listsWithRanks, 9);
        Assert.assertEquals(listRanks, 58);

        // Spot totals matching the reference character sheet.
        Assert.assertEquals(character.getCategoryTotalRanks("weaponsEdged"), Integer.valueOf(10));
        Assert.assertEquals(character.getCategoryTotalRanks("weaponsThrown"), Integer.valueOf(10));
        Assert.assertEquals(character.getCategoryTotalRanks("loreDark"), Integer.valueOf(9));
        Assert.assertEquals(character.getSkillTotalRanks("sword"), Integer.valueOf(9));
        Assert.assertEquals(character.getSkillTotalRanks("bolas"), Integer.valueOf(10));
        Assert.assertEquals(character.getSkillTotalRanks("physicalDevelopment"), Integer.valueOf(11));
        Assert.assertEquals(character.getSkillTotalRanks("demonology"), Integer.valueOf(6));
        Assert.assertEquals(character.getSpellListTotalRanks("essenceArmorOfTheChaos"), Integer.valueOf(14));
        Assert.assertEquals(character.getSpellListTotalRanks("essenceSummoningsOscuras"), Integer.valueOf(8));

        Assert.assertEquals(character.getWeaponCategoryCostTierCount(), 9);
        Assert.assertEquals(character.getDecisions().getAll().size(), 9);
    }

    @Test
    public void snapshotSurvivesRebuildByteForByte() throws IOException, InvalidXmlElementException {
        final CharacterData data = LegacyCharacterJsonImporter.importLegacyJson(readReferenceSnapshot());
        final String json = CharacterJsonManager.toJson(data);

        final CharacterPlayer character = CharacterDataMapper.toCharacter(data);
        Assert.assertEquals(CharacterJsonManager.toJson(CharacterDataMapper.toData(character)), json);
    }

    private static String readReferenceSnapshot() throws IOException {
        try (final InputStream in = LegacyCharacterJsonImporterTest.class.getResourceAsStream(REFERENCE_SNAPSHOT)) {
            Assert.assertNotNull(in, "Missing test resource " + REFERENCE_SNAPSHOT);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}