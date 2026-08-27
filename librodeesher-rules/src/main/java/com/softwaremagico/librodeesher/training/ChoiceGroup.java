package com.softwaremagico.librodeesher.training;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.Collections;
import java.util.List;

/**
 * A group of interchangeable options (characteristics or skills) from which exactly one must be
 * chosen; a group with a single option is not really a choice, it is simply a fixed grant. Used for
 * both the "AUMENTOS CARACTERÍSTICAS" section and the four skill sections (life/common/professional/
 * restricted) of a training, which share the same legacy "plain name, or {alt1;alt2} to choose one"
 * syntax.
 */
public class ChoiceGroup {

    @JacksonXmlElementWrapper(localName = "options")
    @JacksonXmlProperty(localName = "option")
    private List<String> options;

    public ChoiceGroup() {
        // Required by Jackson.
    }

    public ChoiceGroup(List<String> options) {
        this.options = options;
    }

    public List<String> getOptions() {
        return options == null ? Collections.emptyList() : options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    /** Whether this group is a single, fixed option rather than an actual choice. */
    public boolean isFixed() {
        return getOptions().size() == 1;
    }
}
