package com.softwaremagico.librodeesher.language;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.Objects;

/**
 * A piece of text (a name, a description...) available in Spanish and English, as
 * {@code <es>...</es><en>...</en>}.
 *
 * <p>Every rulebook text file is originally Spanish-only; the English translation is produced by the
 * migration tools (see {@code com.softwaremagico.librodeesher.language.Translations}) since the
 * legacy application never had one.</p>
 */
public class TranslatedText implements Comparable<TranslatedText> {

    @JsonProperty("es")
    @JacksonXmlProperty(localName = "es")
    private String spanish;

    @JsonProperty("en")
    @JacksonXmlProperty(localName = "en")
    private String english;

    public TranslatedText() {
        spanish = "";
        english = "";
    }

    public TranslatedText(String spanish, String english) {
        this.spanish = spanish;
        this.english = english;
    }

    public String getSpanish() {
        return spanish;
    }

    public void setSpanish(String spanish) {
        this.spanish = spanish;
    }

    public String getEnglish() {
        return english;
    }

    public void setEnglish(String english) {
        this.english = english;
    }

    /** Returns the text in whichever language {@link Translator#getLanguage()} currently selects. */
    @JsonIgnore
    public String getTranslatedText() {
        if (Objects.equals(Translator.ENGLISH, Translator.getLanguage())) {
            return getEnglish();
        }
        return getSpanish();
    }

    @Override
    public int compareTo(TranslatedText other) {
        if (getTranslatedText() == null) {
            return other.getTranslatedText() == null ? 0 : -1;
        }
        if (other.getTranslatedText() == null) {
            return 1;
        }
        return getTranslatedText().compareTo(other.getTranslatedText());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TranslatedText that)) {
            return false;
        }
        return Objects.equals(spanish, that.spanish) && Objects.equals(english, that.english);
    }

    @Override
    public int hashCode() {
        return Objects.hash(spanish, english);
    }

    @Override
    public String toString() {
        return getTranslatedText();
    }
}
