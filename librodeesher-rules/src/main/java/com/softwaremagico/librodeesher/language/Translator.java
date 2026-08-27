package com.softwaremagico.librodeesher.language;

/**
 * Selects which language {@link TranslatedText#getTranslatedText()} returns. Mirrors
 * ThinkMachine-4E's {@code com.softwaremagico.tm.language.Translator}.
 */
public final class Translator {

    public static final String SPANISH = "es";
    public static final String ENGLISH = "en";
    public static final String DEFAULT_LANGUAGE = SPANISH;

    private static String language = DEFAULT_LANGUAGE;

    private Translator() {
        // Utility class.
    }

    public static String getLanguage() {
        return language;
    }

    public static void setLanguage(String language) {
        Translator.language = language.toLowerCase();
    }
}
