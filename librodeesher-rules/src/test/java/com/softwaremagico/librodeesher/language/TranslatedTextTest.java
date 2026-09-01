package com.softwaremagico.librodeesher.language;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * Verifies {@link TranslatedText}: it returns the Spanish or English text depending on
 * {@link Translator#setLanguage(String)}, and defaults to Spanish (the language every rulebook file
 * originated in).
 */
@Test(groups = "translatedText")
public class TranslatedTextTest {

    @AfterMethod(alwaysRun = true)
    public void resetLanguage() {
        Translator.setLanguage(Translator.DEFAULT_LANGUAGE);
    }

    @Test
    public void defaultsToSpanish() {
        Assert.assertEquals(Translator.getLanguage(), Translator.SPANISH);
    }

    @Test
    public void returnsSpanishTextByDefault() {
        final TranslatedText text = new TranslatedText("Espada Larga", "Long Sword");

        Assert.assertEquals(text.getTranslatedText(), "Espada Larga");
    }

    @Test
    public void returnsEnglishTextWhenLanguageIsEnglish() {
        final TranslatedText text = new TranslatedText("Espada Larga", "Long Sword");

        Translator.setLanguage(Translator.ENGLISH);

        Assert.assertEquals(text.getTranslatedText(), "Long Sword");
    }
}
