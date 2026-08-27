package com.softwaremagico.librodeesher.language;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Verifies {@link Translations}: exact-phrase lookup, the "Category·Subcategory" and
 * "Prefix: ProperNoun" splitting rules, and the word-by-word fallback.
 */
@Test(groups = "translations")
public class TranslationsTest {

    @Test
    public void translatesAnExactPhrase() {
        Assert.assertEquals(Translations.toEnglish("Fuerza"), "Strength");
    }

    @Test
    public void splitsOnMiddleDotAndTranslatesBothSides() {
        Assert.assertEquals(Translations.toEnglish("Armadura·Ligera"), "Armor: Light");
    }

    @Test
    public void keepsProperNounsAfterAColonUntranslated() {
        // The weapon-type prefix is translated, but the specific firearm model is a proper noun.
        Assert.assertEquals(Translations.toEnglish("Revolver: Colt Trooper"), "Revolver: Colt Trooper");
        Assert.assertEquals(Translations.toEnglish("Pistola Automática: Colt M1911"), "Automatic Pistol: Colt M1911");
    }

    @Test
    public void fallsBackToWordByWordTranslation() {
        Assert.assertEquals(Translations.toEnglish("Nadar"), "Swimming");
    }

    @Test
    public void preservesCapitalizationWhenFallingBackToWords() {
        // "Nadar" only exists in the word-level dictionary (not as an exact phrase), so its
        // translation goes through the case-preserving fallback path.
        Assert.assertEquals(Translations.toEnglish("NADAR"), "SWIMMING");
    }

    @Test
    public void leavesUnknownWordsUntranslatedInstead() {
        // No dictionary entry for a made-up word: better to keep it than guess wrong.
        Assert.assertEquals(Translations.toEnglish("Zzyzx"), "Zzyzx");
    }

    @Test
    public void returnsNullOrBlankInputUnchanged() {
        Assert.assertNull(Translations.toEnglish(null));
        Assert.assertEquals(Translations.toEnglish(""), "");
    }
}
