package com.softwaremagico.librodeesher.persistence;

import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.character.SexType;
import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.decision.DecisionKey;
import com.softwaremagico.librodeesher.decision.DecisionKind;
import com.softwaremagico.librodeesher.equipment.BonusType;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.magic.RealmOfMagic;
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
    private static final String REFERENCE_SNAPSHOT_V211 = "/Ophiliaglii_Humbiton_N7_Jhordi_Monje_Zen.json";

    @Test
    public void importsTheReferenceLevelSevenV211Snapshot() throws IOException, InvalidXmlElementException {
        final CharacterData data = LegacyCharacterJsonImporter.importLegacyJson(readN7Snapshot());

        Assert.assertEquals(data.getName(), "Ophiliaglii Humbiton");
        Assert.assertEquals(data.getSex(), SexType.MALE);
        Assert.assertEquals(data.getRaceId(), "jhordi");
        Assert.assertEquals(data.getCultureId(), "plains");
        Assert.assertEquals(data.getProfessionId(), "monkZen");

        Assert.assertEquals(data.getLevels().size(), 7);
        Assert.assertEquals(data.getCurrentAge(), 10);
        Assert.assertEquals(data.getFinalAge(), 10);
        for (final LevelData level : data.getLevels()) {
            Assert.assertEquals(level.getAge(), 10);
        }

        Assert.assertEquals(data.getCharacteristicTemporalValues().get("AGILITY"), Integer.valueOf(95));
        Assert.assertEquals(data.getCharacteristicTemporalValues().get("SELF_DISCIPLINE"), Integer.valueOf(96));
        Assert.assertEquals(data.getCharacteristicTemporalValues().get("QUICKNESS"), Integer.valueOf(100));
        Assert.assertEquals(data.getCharacteristicTemporalValues().get("STRENGTH"), Integer.valueOf(99));
        Assert.assertEquals(data.getCharacteristicTemporalValues().get("APPEARANCE"), Integer.valueOf(0));
        Assert.assertEquals(data.getCharacteristicPotentialValues().get("MEMORY"), Integer.valueOf(68));
        Assert.assertEquals(data.getCharacteristicPotentialValues().get("PRESENCE"), Integer.valueOf(94));
        Assert.assertEquals(data.getCharacteristicPotentialValues().get("REASONING"), Integer.valueOf(97));
        Assert.assertTrue(data.isCharacteristicsConfirmed());
        Assert.assertEquals(data.getAppearance(), 18);

        // Level 1: the culture's adolescence grants materialized as fixed ranks, the chosen weapon
        // skills as skill ranks, and the first level-up's ranks (including its training's grants).
        final LevelData levelOne = data.getLevels().get(0);
        Assert.assertEquals(levelOne.getCategoryRanks().get("armorLight"), Integer.valueOf(1));
        Assert.assertEquals(levelOne.getCategoryRanks().get("weaponsEdged"), Integer.valueOf(1));
        Assert.assertEquals(levelOne.getCategoryRanks().get("weaponsMissile"), Integer.valueOf(2));
        Assert.assertEquals(levelOne.getCategoryRanks().get("loreGeneral"), Integer.valueOf(4));
        Assert.assertEquals(levelOne.getCategoryRanks().get("outdoorAnimals"), Integer.valueOf(3));
        Assert.assertEquals(levelOne.getSkillRanks().get("spear"), Integer.valueOf(2));
        Assert.assertEquals(levelOne.getSkillRanks().get("sword"), Integer.valueOf(1));
        Assert.assertEquals(levelOne.getSkillRanks().get("throwingDagger"), Integer.valueOf(1));
        Assert.assertEquals(levelOne.getSkillRanks().get("compositeBow"), Integer.valueOf(2));
        Assert.assertEquals(levelOne.getSkillRanks().get("staff"), Integer.valueOf(1));
        Assert.assertEquals(levelOne.getSkillRanks().get("inmovilizar"), Integer.valueOf(2));
        Assert.assertEquals(levelOne.getSkillRanks().get("strikesOfArtesMarciales"), Integer.valueOf(3));
        Assert.assertEquals(levelOne.getSkillRanks().get("physicalDevelopment"), Integer.valueOf(5));
        Assert.assertEquals(levelOne.getTrainings(), List.of("priestWarrior"));
        Assert.assertTrue(levelOne.getCharacteristicUpdates().size() == 10);

        // Level 2: the first level-up's spell list development.
        final LevelData levelTwo = data.getLevels().get(1);
        Assert.assertEquals(levelTwo.getSpellListRanks().get("mentalismConcentrationZen"), Integer.valueOf(1));
        Assert.assertEquals(levelTwo.getSpellListRanks().get("mentalismEvasiones"), Integer.valueOf(1));
        Assert.assertEquals(levelTwo.getSpellsUpdated(), List.of("mentalismConcentrationZen",
                "mentalismControlCorporal", "mentalismEvasiones", "mentalismRenewalOfTheBody",
                "mentalismShadowMental"));

        // The seven weapon cost tiers become character-wide decisions, filed by key.
        final Map<String, DecisionData> decisions = new TreeMap<>(data.getDecisions());
        Assert.assertEquals(decisions.size(), 9);
        final List<String> categoryByTier = List.of("weaponsTwoHanded", "weaponsThrown", "weaponsSiege",
                "weaponsPolearm", "weaponsBlunt", "weaponsEdged", "weaponsMissile");
        for (int tier = 0; tier < 7; tier++) {
            final String key = DecisionKey.characterWide(DecisionKind.WEAPON_COST_TIER, "", tier).toString();
            final DecisionData decision = decisions.get(key);
            Assert.assertNotNull(decision, key);
            Assert.assertEquals(decision.getRecordedAtLevel(), 1);
            Assert.assertEquals(decision.getOfferedOptions(), List.of(categoryByTier.get(tier)));
            Assert.assertEquals(decision.getSelectedOptions(), List.of(categoryByTier.get(tier)));
        }

        // The legacy realm of magic becomes a fixed profession-realm decision, so the rebuilt
        // character resolves a Mentalism caster.
        final String realmKey =
                DecisionKey.characterWide(DecisionKind.PROFESSION_REALM, data.getProfessionId(), 0).toString();
        final DecisionData realmDecision = decisions.get(realmKey);
        Assert.assertNotNull(realmDecision, realmKey);
        Assert.assertEquals(realmDecision.getRecordedAtLevel(), 1);
        Assert.assertEquals(realmDecision.getOfferedOptions(), List.of("MENTALISM"));
        Assert.assertEquals(realmDecision.getSelectedOptions(), List.of("MENTALISM"));

        // The chi skill unlocked by the martial arts style becomes a skill-enable decision.
        final String skillEnableKey =
                DecisionKey.characterWide(DecisionKind.SKILL_ENABLE, "styleOfTheCrane", 0).toString();
        final DecisionData skillEnableDecision = decisions.get(skillEnableKey);
        Assert.assertNotNull(skillEnableDecision, skillEnableKey);
        Assert.assertEquals(skillEnableDecision.getRecordedAtLevel(), 1);
        Assert.assertEquals(skillEnableDecision.getOfferedOptions(), List.of("chiPowerStrikesContinuous"));
        Assert.assertEquals(skillEnableDecision.getSelectedOptions(), List.of("chiPowerStrikesContinuous"));

        Assert.assertEquals(data.getSelectedPerks().size(), 17);
        Assert.assertEquals(data.getSelectedPerks().get(0).getPerkId(), "adherencia");
        Assert.assertEquals(data.getSelectedPerks().get(1).getPerkId(), "slightAddiction");
        Assert.assertEquals(data.getSelectedPerks().get(2).getPerkId(), "trainingInArtesMarcialesMaximum");
        Assert.assertEquals(data.getSelectedPerks().get(3).getPerkId(), "sensitiveSkin");
        Assert.assertEquals(data.getSelectedPerks().get(4).getPerkId(), "pielGruesaMajor");

        Assert.assertEquals(data.getBackground().getCategoryIds(), List.of("martialArtsStrikes",
                "martialArtsCombatManeuvers", "selfControl", "influence"));
        Assert.assertTrue(data.getBackground().getSkillIds().isEmpty());
        Assert.assertEquals(data.getBackground().getLanguageRanks().size(), 4);
        Assert.assertEquals(data.getBackground().getLanguageRanks().get("Hablar Habla Común"), Integer.valueOf(2));
        Assert.assertEquals(data.getBackground().getLanguageRanks().get("Hablar Habla de las Llanuras"),
                Integer.valueOf(2));
        Assert.assertEquals(data.getBackground().getLanguageRanks().get("Hablar Habla Alta"), Integer.valueOf(4));
        Assert.assertEquals(data.getBackground().getLanguageRanks().get("Escribir Habla Común"), Integer.valueOf(1));
        Assert.assertEquals(data.getHobbySkillRanks().size(), 12);

        Assert.assertTrue(data.isChiPowersAllowed());
        Assert.assertFalse(data.isFirearmsAllowed());
        Assert.assertFalse(data.isOtherRealmTrainingSpellsAllowed());
        Assert.assertFalse(data.isDarkSpellsAsBasicListsAllowed());
        Assert.assertFalse(data.isRecommendedFavouriteSkillsIncluded());
    }

    @Test
    public void rebuiltV211CharacterTotalsMatchTheReferenceBook() throws IOException, InvalidXmlElementException {
        final CharacterPlayer character = CharacterDataMapper.toCharacter(
                LegacyCharacterJsonImporter.importLegacyJson(readN7Snapshot()));

        Assert.assertEquals(character.getLevel(), 7);
        Assert.assertEquals(character.getAgeAtLevel(7), 10);

        int categoriesWithRanks = 0;
        int categoryRanks = 0;
        for (final com.softwaremagico.librodeesher.Element category : RulesCatalog.getInstance().getCategories()) {
            final int ranks = character.getCategoryTotalRanks(category.getId());
            if (ranks > 0) {
                categoriesWithRanks++;
                categoryRanks += ranks;
            }
        }
        Assert.assertEquals(categoriesWithRanks, 23);
        Assert.assertEquals(categoryRanks, 84);

        int skillsWithRanks = 0;
        int skillRanks = 0;
        for (final com.softwaremagico.librodeesher.Element skill : RulesCatalog.getInstance().getSkills()) {
            final int ranks = character.getSkillTotalRanks(skill.getId());
            if (ranks > 0) {
                skillsWithRanks++;
                skillRanks += ranks;
            }
        }
        Assert.assertEquals(skillsWithRanks, 77);
        Assert.assertEquals(skillRanks, 124);

        int listsWithRanks = 0;
        int listRanks = 0;
        for (final com.softwaremagico.librodeesher.Element list : RulesCatalog.getInstance().getSpellLists()) {
            final int ranks = character.getSpellListTotalRanks(list.getId());
            if (ranks > 0) {
                listsWithRanks++;
                listRanks += ranks;
            }
        }
        Assert.assertEquals(listsWithRanks, 6);
        Assert.assertEquals(listRanks, 30);

        // Spot totals matching the reference character sheet.
        Assert.assertEquals(character.getCategoryTotalRanks("martialArtsStrikes"), Integer.valueOf(7));
        Assert.assertEquals(character.getCategoryTotalRanks("selfControl"), Integer.valueOf(7));
        Assert.assertEquals(character.getCategoryTotalRanks("influence"), Integer.valueOf(7));
        Assert.assertEquals(character.getSkillTotalRanks("strikesOfArtesMarciales"), Integer.valueOf(9));
        Assert.assertEquals(character.getSkillTotalRanks("physicalDevelopment"), Integer.valueOf(7));
        Assert.assertEquals(character.getSkillTotalRanks("defenseAdrenal"), Integer.valueOf(7));
        Assert.assertEquals(character.getSkillTotalRanks("styleOfTheCrane"), Integer.valueOf(6));
        Assert.assertEquals(character.getSkillTotalRanks("spear"), Integer.valueOf(2));
        Assert.assertEquals(character.getSkillTotalRanks("sword"), Integer.valueOf(1));
        Assert.assertEquals(character.getSkillTotalRanks("meditation"), Integer.valueOf(1));
        Assert.assertEquals(character.getSpellListTotalRanks("mentalismShadowMental"), Integer.valueOf(7));
        Assert.assertEquals(character.getSpellListTotalRanks("mentalismConcentrationZen"), Integer.valueOf(6));
        Assert.assertEquals(character.getSpellListTotalRanks("mentalismEvasiones"), Integer.valueOf(6));
        Assert.assertEquals(character.getSpellListTotalRanks("mentalismRenewalOfTheBody"), Integer.valueOf(5));
        Assert.assertEquals(character.getSpellListTotalRanks("mentalismControlCorporal"), Integer.valueOf(5));
        Assert.assertEquals(character.getSpellListTotalRanks("mentalismPerceptionZen"), Integer.valueOf(1));

        Assert.assertEquals(character.getWeaponCategoryCostTierCount(), 7);
        Assert.assertEquals(character.getDecisions().getAll().size(), 9);
        Assert.assertEquals(character.getRealmsOfMagic().size(), 1);
        Assert.assertEquals(character.getRealmsOfMagic().get(0), RealmOfMagic.MENTALISM);
        Assert.assertTrue(character.getPowerPoints() > 0);
    }

    @Test
    public void v211SnapshotSurvivesRebuildByteForByte() throws IOException, InvalidXmlElementException {
        final CharacterData data = LegacyCharacterJsonImporter.importLegacyJson(readN7Snapshot());
        final String json = CharacterJsonManager.toJson(data);

        final CharacterPlayer character = CharacterDataMapper.toCharacter(data);
        Assert.assertEquals(CharacterJsonManager.toJson(CharacterDataMapper.toData(character)), json);
    }

    private static String readN7Snapshot() throws IOException {
        return readFixture(REFERENCE_SNAPSHOT_V211);
    }

    private static String readFixture(String path) throws IOException {
        try (final InputStream in = LegacyCharacterJsonImporterTest.class.getResourceAsStream(path)) {
            Assert.assertNotNull(in, "Missing test resource " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

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

        for (int i = 1; i <= 4; i++) {
            Assert.assertTrue(data.getLevels().get(i).getCategoryRanks().isEmpty());
            Assert.assertTrue(data.getLevels().get(i).getSkillRanks().isEmpty());
        }

        // Level 6: the legacy "inserted" development (insertedLevels = 6).
        final LevelData inserted = data.getLevels().get(5);
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

        // Level 7: the first legacy level-up, which is no longer folded into level 1. It only
        // refreshed the spell lists there, its development being part of the inserted record.
        final LevelData levelSeven = data.getLevels().get(6);
        Assert.assertEquals(levelSeven.getSpellsUpdated(), List.of("essenceBarrierAgainstSpells",
                "essenceIlusionesMenores", "essencePathsOfApertura", "essencePathsOfDetection",
                "essenceWeaponOfTheChaos", "essenceArmorOfTheChaos", "essenceSummoningsOscuras",
                "essenceMasteryOfChaos", "essenceOscuridad"));
        Assert.assertTrue(levelSeven.getCategoryRanks().isEmpty());
        Assert.assertTrue(levelSeven.getSkillRanks().isEmpty());
        Assert.assertTrue(levelSeven.getCharacteristicUpdates().isEmpty());

        // Level 8: the second legacy level-up.
        final LevelData levelEight = data.getLevels().get(7);
        Assert.assertEquals(levelEight.getCategoryRanks().get("weaponsEdged"), Integer.valueOf(1));
        Assert.assertEquals(levelEight.getSkillRanks().get("sword"), Integer.valueOf(1));
        Assert.assertEquals(levelEight.getSpellListRanks().get("essenceMasteryOfChaos"), Integer.valueOf(3));
        Assert.assertEquals(levelEight.getSpellListRanks().get("essencePathsOfDetection"), Integer.valueOf(2));
        Assert.assertEquals(levelEight.getSpellListRanks().get("essenceArmorOfTheChaos"), Integer.valueOf(1));
        Assert.assertEquals(levelEight.getSpellsUpdated(),
                List.of("essenceMasteryOfChaos", "essenceArmorOfTheChaos", "essencePathsOfDetection"));
        Assert.assertEquals(levelEight.getFavouriteSkills(), List.of("bolas", "liderazgo", "sword"));
        Assert.assertEquals(levelEight.getCharacteristicUpdates().size(), 10);
        Assert.assertEquals(levelEight.getCharacteristicUpdates().get(0).getCharacteristicAbbreviation(),
                CharacteristicAbbreviation.AGILITY);
        Assert.assertEquals(levelEight.getCharacteristicUpdates().get(0).getCharacteristicTemporalValue(),
                Integer.valueOf(93));
        Assert.assertEquals(levelEight.getCharacteristicUpdates().get(0).getRoll().getFirstDice(), 8);
        Assert.assertEquals(levelEight.getCharacteristicUpdates().get(0).getRoll().getSecondDice(), 9);

        // Level 9: the third legacy level-up.
        final LevelData levelNine = data.getLevels().get(8);
        Assert.assertEquals(levelNine.getSpellListRanks().get("essenceArmorOfTheChaos"), Integer.valueOf(3));
        Assert.assertEquals(levelNine.getSpellListRanks().get("essenceOscuridad"), Integer.valueOf(1));
        Assert.assertEquals(levelNine.getSpellListRanks().get("essencePathsOfApertura"), Integer.valueOf(2));
        Assert.assertEquals(levelNine.getSpellsUpdated(),
                List.of("essenceOscuridad", "essencePathsOfApertura", "essenceArmorOfTheChaos"));
        Assert.assertEquals(levelNine.getCharacteristicUpdates().size(), 10);

        // The nine weapon cost tiers become character-wide decisions, filed by key.
        final Map<String, DecisionData> decisions = new TreeMap<>(data.getDecisions());
        Assert.assertEquals(decisions.size(), 10);
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

        // The legacy realm of magic becomes a fixed profession-realm decision, so the rebuilt
        // character resolves a caster of the Essence realm.
        final String realmKey =
                DecisionKey.characterWide(DecisionKind.PROFESSION_REALM, data.getProfessionId(), 0).toString();
        final DecisionData realmDecision = decisions.get(realmKey);
        Assert.assertNotNull(realmDecision, realmKey);
        Assert.assertEquals(realmDecision.getRecordedAtLevel(), 1);
        Assert.assertEquals(realmDecision.getOfferedOptions(), List.of("ESSENCE"));
        Assert.assertEquals(realmDecision.getSelectedOptions(), List.of("ESSENCE"));

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
        Assert.assertEquals(skillsWithRanks, 63);
        Assert.assertEquals(skillRanks, 159);

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
        Assert.assertEquals(character.getDecisions().getAll().size(), 10);
        Assert.assertEquals(character.getRealmsOfMagic().size(), 1);
        Assert.assertEquals(character.getRealmsOfMagic().get(0), RealmOfMagic.ESSENCE);
        Assert.assertTrue(character.getPowerPoints() > 0);
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

    // ---------------------------------------------------------------------------------------------
    // Morticia Innodan, the same character saved at three consecutive levels. The 2.1.1 snapshots
    // (N10, N11) record the first nine levels folded into a single "inserted" development, while the
    // 2.0.3 snapshot (N12) keeps the same shape under a background/configuration pair of sections.
    // ---------------------------------------------------------------------------------------------

    private static final String MORTICIA_N10 = "/Morticia_Innodan_N10_Ilourianos_Elementalista.json";
    private static final String MORTICIA_N11 = "/Morticia_Innodan_N11_Ilourianos_Elementalista.json";
    private static final String MORTICIA_N12 = "/Morticia_Innodan_N12_Ilourianos_Elementalista.json";

    @Test
    public void importsTheMorticiaLevelTenSnapshot() throws IOException, InvalidXmlElementException {
        assertMorticiaIdentity(importMorticia(MORTICIA_N10), 10);
    }

    @Test
    public void importsTheMorticiaLevelElevenSnapshot() throws IOException, InvalidXmlElementException {
        assertMorticiaIdentity(importMorticia(MORTICIA_N11), 11);
    }

    @Test
    public void importsTheMorticiaLevelTwelveSnapshot() throws IOException, InvalidXmlElementException {
        assertMorticiaIdentity(importMorticia(MORTICIA_N12), 12);
    }

    @Test
    public void morticiaSnapshotsSurviveRebuildByteForByte() throws IOException, InvalidXmlElementException {
        for (final String snapshot : List.of(MORTICIA_N10, MORTICIA_N11, MORTICIA_N12)) {
            final CharacterData data = LegacyCharacterJsonImporter.importLegacyJson(readFixture(snapshot));
            final String json = CharacterJsonManager.toJson(data);

            final CharacterPlayer character = CharacterDataMapper.toCharacter(data);
            Assert.assertEquals(CharacterJsonManager.toJson(CharacterDataMapper.toData(character)), json, snapshot);
        }
    }

    @Test
    public void rebuiltMorticiaTotalsMatchTheReferenceBook() throws IOException, InvalidXmlElementException {
        // Reference totals recomputed from the snapshots' own "inserted" record and level-ups, with
        // the skill ranks of the level 10 "Mago del Aire" training added on top of the recorded ones.
        assertMorticiaTotals(importMorticia(MORTICIA_N10), 10, 22, 81, 50, 216, 16, 187, 7, 8);
        assertMorticiaTotals(importMorticia(MORTICIA_N11), 11, 22, 89, 58, 245, 16, 193, 7, 8);
        assertMorticiaTotals(importMorticia(MORTICIA_N12), 12, 22, 91, 62, 251, 18, 211, 9, 10);
    }

    private static CharacterData importMorticia(String snapshot) throws IOException, InvalidXmlElementException {
        return LegacyCharacterJsonImporter.importLegacyJson(readFixture(snapshot));
    }

    /** The identity and the level layout that all three Morticia snapshots share. */
    private static void assertMorticiaIdentity(CharacterData data, int level) {
        Assert.assertEquals(data.getName(), "Morticia Innodan");
        Assert.assertEquals(data.getSex(), SexType.FEMALE);
        Assert.assertEquals(data.getRaceId(), "ilourianos");
        Assert.assertEquals(data.getCultureId(), "woodland");
        Assert.assertEquals(data.getProfessionId(), "elementalist");
        Assert.assertEquals(data.getCurrentAge(), 10);
        Assert.assertEquals(data.getFinalAge(), 10);

        // The final values are the base rolls plus the folded levels' development; the level-up that
        // follows a folded one only carries a placeholder (temporal 31, potential 0) and must not
        // overwrite them.
        Assert.assertEquals(data.getCharacteristicTemporalValues().get("CONSTITUTION"), Integer.valueOf(92));
        Assert.assertEquals(data.getCharacteristicTemporalValues().get("AGILITY"), Integer.valueOf(95));
        Assert.assertEquals(data.getCharacteristicTemporalValues().get("EMPATHY"), Integer.valueOf(96));
        Assert.assertEquals(data.getCharacteristicTemporalValues().get("MEMORY"), Integer.valueOf(99));
        Assert.assertEquals(data.getCharacteristicTemporalValues().get("INTUITION"), Integer.valueOf(76));
        Assert.assertEquals(data.getCharacteristicTemporalValues().get("PRESENCE"), Integer.valueOf(75));
        Assert.assertEquals(data.getCharacteristicTemporalValues().get("STRENGTH"), Integer.valueOf(76));
        Assert.assertEquals(data.getCharacteristicTemporalValues().get("APPEARANCE"), Integer.valueOf(0));
        Assert.assertEquals(data.getCharacteristicPotentialValues(), data.getCharacteristicTemporalValues());
        Assert.assertEquals(data.getAppearance(), 24);
        Assert.assertTrue(data.isCharacteristicsConfirmed());

        // The background is the character's five spell-list categories plus physical development.
        Assert.assertEquals(data.getBackground().getCategoryIds(), List.of("listsAbiertasOfSpells",
                "listsBasicOfSpells", "directedSpells", "listsCerradasOfSpells", "powerAwareness"));
        Assert.assertEquals(data.getBackground().getSkillIds(), List.of("physicalDevelopment"));

        Assert.assertEquals(data.getMagicItems().size(), 3);

        // One real level per recorded level-up on top of the nine folded ones.
        Assert.assertEquals(data.getLevels().size(), level);
        for (final LevelData data1 : data.getLevels()) {
            Assert.assertEquals(data1.getAge(), 10);
        }

        // Level 1: the Silvana culture's adolescence grants materialized as fixed ranks.
        final LevelData levelOne = data.getLevels().get(0);
        Assert.assertEquals(levelOne.getCategoryRanks().get("outdoorEnvironment"), Integer.valueOf(5));
        Assert.assertEquals(levelOne.getCategoryRanks().get("subterfugeStealth"), Integer.valueOf(4));
        Assert.assertEquals(levelOne.getCategoryRanks().get("loreGeneral"), Integer.valueOf(3));
        Assert.assertEquals(levelOne.getCategoryRanks().get("weaponsMissile"), Integer.valueOf(3));
        Assert.assertEquals(levelOne.getSkillRanks().get("alerta"), Integer.valueOf(6));
        Assert.assertEquals(levelOne.getSkillRanks().get("swimming"), Integer.valueOf(3));
        Assert.assertEquals(levelOne.getSkillRanks().get("physicalDevelopment"), Integer.valueOf(1));

        // Levels 2 to 8 were folded away and stay empty.
        for (int i = 1; i <= 7; i++) {
            Assert.assertTrue(data.getLevels().get(i).getCategoryRanks().isEmpty());
            Assert.assertTrue(data.getLevels().get(i).getSkillRanks().isEmpty());
        }

        // Level 9: the folded development, holding the whole of the first nine levels.
        final LevelData inserted = data.getLevels().get(8);
        Assert.assertEquals(sum(inserted.getCategoryRanks()), 55);
        Assert.assertEquals(inserted.getCategoryRanks().size(), 13);
        Assert.assertEquals(sum(inserted.getSkillRanks()), 178);
        Assert.assertEquals(inserted.getSkillRanks().size(), 41);
        Assert.assertEquals(sum(inserted.getSpellListRanks()), 187);
        Assert.assertEquals(inserted.getSpellListRanks().size(), 16);
        Assert.assertEquals(inserted.getSpellListRanks().get("essenceControlOfAir"), Integer.valueOf(30));
        Assert.assertEquals(inserted.getSpellListRanks().get("essenceMasteryOfEscudos"), Integer.valueOf(20));
        Assert.assertEquals(inserted.getSpellListRanks().get("essenceLawOfWind"), Integer.valueOf(18));
        Assert.assertEquals(inserted.getSpellListRanks().get("essenceMasteryOfWind"), Integer.valueOf(14));
        Assert.assertEquals(inserted.getSpellListRanks().get("essenceMasteryOfSpirits"), Integer.valueOf(13));
        Assert.assertEquals(inserted.getSpellListRanks().get("essenceSummoningsElementales"), Integer.valueOf(12));
        Assert.assertEquals(inserted.getSpellListRanks().get("essencePathOfInvisibilidad"), Integer.valueOf(4));

        // Level 10: the first real level-up, which trained as a "Mago del Aire" and whose
        // characteristic updates are the placeholders the folded level left behind.
        final LevelData levelTen = data.getLevels().get(9);
        Assert.assertEquals(sum(levelTen.getSkillRanks()), 6);
        Assert.assertEquals(levelTen.getSkillRanks().size(), 4);
        Assert.assertEquals(levelTen.getCharacteristicUpdates().size(), 10);
        Assert.assertEquals(levelTen.getTrainings().size(), 1);
        Assert.assertTrue(levelTen.getTrainings().contains("wizardOfTheAir"));
    }

    private static void assertMorticiaTotals(CharacterData data, int level, int categoriesWithRanks,
                                             int categoryRanks, int skillsWithRanks, int skillRanks,
                                             int listsWithRanks, int listRanks, int weaponTiers, int decisions)
            throws InvalidXmlElementException {
        final CharacterPlayer character = CharacterDataMapper.toCharacter(data);
        Assert.assertEquals(character.getLevel(), level);
        Assert.assertEquals(character.getAgeAtLevel(level), 10);

        int countedCategories = 0;
        int countedCategoryRanks = 0;
        for (final com.softwaremagico.librodeesher.Element category : RulesCatalog.getInstance().getCategories()) {
            final int ranks = character.getCategoryTotalRanks(category.getId());
            if (ranks > 0) {
                countedCategories++;
                countedCategoryRanks += ranks;
            }
        }
        Assert.assertEquals(countedCategories, categoriesWithRanks);
        Assert.assertEquals(countedCategoryRanks, categoryRanks);

        int countedSkills = 0;
        int countedSkillRanks = 0;
        for (final com.softwaremagico.librodeesher.Element skill : RulesCatalog.getInstance().getSkills()) {
            final int ranks = character.getSkillTotalRanks(skill.getId());
            if (ranks > 0) {
                countedSkills++;
                countedSkillRanks += ranks;
            }
        }
        Assert.assertEquals(countedSkills, skillsWithRanks);
        Assert.assertEquals(countedSkillRanks, skillRanks);

        int countedLists = 0;
        int countedListRanks = 0;
        for (final com.softwaremagico.librodeesher.Element list : RulesCatalog.getInstance().getSpellLists()) {
            final int ranks = character.getSpellListTotalRanks(list.getId());
            if (ranks > 0) {
                countedLists++;
                countedListRanks += ranks;
            }
        }
        Assert.assertEquals(countedLists, listsWithRanks);
        Assert.assertEquals(countedListRanks, listRanks);

        // Spot totals matching the reference character sheets.
        Assert.assertEquals(character.getCategoryTotalRanks("outdoorEnvironment"), Integer.valueOf(5));
        Assert.assertEquals(character.getSkillTotalRanks("physicalDevelopment"), Integer.valueOf(1));
        Assert.assertEquals(character.getSkillTotalRanks("demonology"), Integer.valueOf(3));
        Assert.assertEquals(character.getSpellListTotalRanks("essenceControlOfAir"), Integer.valueOf(30));
        Assert.assertEquals(character.getSpellListTotalRanks("essenceMasteryOfEscudos"), Integer.valueOf(20));

        Assert.assertEquals(character.getWeaponCategoryCostTierCount(), weaponTiers);
        Assert.assertEquals(character.getDecisions().getAll().size(), decisions);
        Assert.assertEquals(character.getRealmsOfMagic().size(), 1);
        Assert.assertEquals(character.getRealmsOfMagic().get(0), RealmOfMagic.ESSENCE);
        Assert.assertTrue(character.getPowerPoints() > 0);
    }

    private static int sum(Map<String, Integer> ranks) {
        int total = 0;
        for (final Integer rank : ranks.values()) {
            total += rank;
        }
        return total;
    }
}