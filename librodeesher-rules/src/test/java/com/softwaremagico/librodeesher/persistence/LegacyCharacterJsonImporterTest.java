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
        Assert.assertEquals(levelEight.getFavouriteSkills(), List.of("bolas", "liderazgo", "sword"));
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
}