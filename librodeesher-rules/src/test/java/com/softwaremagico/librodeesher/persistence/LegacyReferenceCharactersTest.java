package com.softwaremagico.librodeesher.persistence;

import com.softwaremagico.librodeesher.Element;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.character.SexType;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.rules.RulesCatalog;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Rules-module coverage for the legacy reference characters that previously only had PDF-side
 * tests: Kara Trace, Fthagtli Milupa, Gorju Ping and Skaag El Carnicero.
 */
@Test(groups = "persistence")
public class LegacyReferenceCharactersTest {

    private static final String KARA_N5 = "/legacy/Bellido__Kara_Trace_N5_Medio-elfo_Magente.rlm";
    private static final String KARA_N6 = "/legacy/Kara_Trace_N6_Medio-elfo_Magente.rlm";
    private static final String FTHAGTLI_N5 = "/legacy/Fthagtli_Milupa_N5_Garks_Monje Guerrero.rlm";
    private static final String GORJU_N5 = "/legacy/Gorju_Ping_N5_Kobolds_Lego.rlm";
    private static final String SKAAG_N8 = "/legacy/Skaag_El_Carnicero_N8_Troll de Guerra_Luchador.rlm";

    @Test
    public void importsKaraTraceAtLevelFive() throws Exception {
        final CharacterData data = importSnapshot(KARA_N5);
        assertIdentity(data, "Kara Trace", SexType.MALE, "halfElf", "woodland", "magente", 5, 28);
        assertRebuiltTotals(data, "Kara N5", 23, 61, 46, 162, 6, 22);
    }

    @Test
    public void importsKaraTraceAtLevelSix() throws Exception {
        final CharacterData data = importSnapshot(KARA_N6);
        assertIdentity(data, "Kara Trace", SexType.FEMALE, "halfElf", "woodland", "magente", 6, 28);
        assertRebuiltTotals(data, "Kara N6", 23, 68, 47, 179, 7, 26);
    }

    @Test
    public void importsFthagtliMilupaAtLevelFive() throws Exception {
        final CharacterData data = importSnapshot(FTHAGTLI_N5);
        assertIdentity(data, "Fthagtli Milupa", SexType.MALE, "garks", "woodlandBarbarian", "monkWarrior", 5, 22);
        assertRebuiltTotals(data, "Fthagtli N5", 24, 88, 91, 303, 0, 0);
    }

    @Test
    public void importsGorjuPingAtLevelFive() throws Exception {
        final CharacterData data = importSnapshot(GORJU_N5);
        assertIdentity(data, "Gorju Ping", SexType.MALE, "kobolds", "undergroundUrbanClassLow", "layman", 5, 27);
        assertRebuiltTotals(data, "Gorju N5", 28, 75, 94, 149, 0, 0);
    }

    @Test
    public void importsSkaagElCarniceroAtLevelEight() throws Exception {
        final CharacterData data = importSnapshot(SKAAG_N8);
        assertIdentity(data, "Skaag El Carnicero", SexType.MALE, "warTroll", "underground", "fighter", 8, 35);
        assertRebuiltTotals(data, "Skaag N8", 23, 122, 86, 266, 0, 0);
    }

    @Test
    public void snapshotsSurviveAJsonRoundTrip() throws Exception {
        for (final String snapshot : new String[]{KARA_N5, KARA_N6, FTHAGTLI_N5, GORJU_N5, SKAAG_N8}) {
            final String json = CharacterJsonManager.toJson(importSnapshot(snapshot));
            Assert.assertEquals(CharacterJsonManager.toJson(CharacterJsonManager.fromJson(json)), json, snapshot);
        }
    }

    private static CharacterData importSnapshot(String path) throws IOException, InvalidXmlElementException {
        RulesCatalog.getInstance();
        try (final InputStream in = LegacyReferenceCharactersTest.class.getResourceAsStream(path)) {
            Assert.assertNotNull(in, "Missing " + path);
            return LegacyCharacterJsonImporter.importLegacyJson(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private static void assertIdentity(CharacterData data, String name, SexType sex, String raceId,
                                       String cultureId, String professionId, int level, int appearance) {
        Assert.assertEquals(data.getName(), name);
        Assert.assertEquals(data.getSex(), sex);
        Assert.assertEquals(data.getRaceId(), raceId);
        Assert.assertEquals(data.getCultureId(), cultureId);
        Assert.assertEquals(data.getProfessionId(), professionId);
        Assert.assertEquals(data.getLevels().size(), level);
        Assert.assertTrue(data.isCharacteristicsConfirmed());
        Assert.assertEquals(data.getAppearance(), appearance);
    }

    private static void assertRebuiltTotals(CharacterData data, String label, int categoriesWithRanks,
                                            int categoryRanks, int skillsWithRanks, int skillRanks,
                                            int listsWithRanks, int listRanks) throws InvalidXmlElementException {
        final CharacterPlayer character = CharacterDataMapper.toCharacter(data);
        Assert.assertEquals(character.getLevel(), data.getLevels().size(), label);

        int countedCategories = 0;
        int countedCategoryRanks = 0;
        for (final Element category : RulesCatalog.getInstance().getCategories()) {
            final int ranks = character.getCategoryTotalRanks(category.getId());
            if (ranks > 0) {
                countedCategories++;
                countedCategoryRanks += ranks;
            }
        }
        int countedSkills = 0;
        int countedSkillRanks = 0;
        for (final Element skill : RulesCatalog.getInstance().getSkills()) {
            final int ranks = character.getSkillTotalRanks(skill.getId());
            if (ranks > 0) {
                countedSkills++;
                countedSkillRanks += ranks;
            }
        }
        int countedLists = 0;
        int countedListRanks = 0;
        for (final Element list : RulesCatalog.getInstance().getSpellLists()) {
            final int ranks = character.getSpellListTotalRanks(list.getId());
            if (ranks > 0) {
                countedLists++;
                countedListRanks += ranks;
            }
        }
        Assert.assertEquals(countedCategories, categoriesWithRanks, label + " categories");
        Assert.assertEquals(countedCategoryRanks, categoryRanks, label + " category ranks");
        Assert.assertEquals(countedSkills, skillsWithRanks, label + " skills");
        Assert.assertEquals(countedSkillRanks, skillRanks, label + " skill ranks");
        Assert.assertEquals(countedLists, listsWithRanks, label + " lists");
        Assert.assertEquals(countedListRanks, listRanks, label + " list ranks");
    }
}
