package com.softwaremagico.librodeesher.xml;

import com.softwaremagico.librodeesher.Element;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Minimal {@link Element} subclass used only by {@link XmlFactoryTest} to exercise the generic
 * reading/merging behaviour of {@link XmlFactory} without depending on any real rulebook data.
 */
public class SampleElement extends Element {

    @JsonProperty("value")
    private int value;

    public SampleElement() {
        super();
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
