package com.softwaremagico.librodeesher.xml;

import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;

import java.util.List;

/**
 * Test-only factory reading {@code samples.xml} from every enabled module, used by
 * {@link XmlFactoryTest} as a template-method example of a concrete {@link XmlFactory}.
 */
public class SampleElementFactory extends XmlFactory<SampleElement> {

    private static final String XML_FILE = "samples.xml";

    private static final class SampleElementFactoryHolder {
        private static final SampleElementFactory INSTANCE = new SampleElementFactory();
    }

    public static SampleElementFactory getInstance() {
        return SampleElementFactoryHolder.INSTANCE;
    }

    @Override
    public String getXmlFile() {
        return XML_FILE;
    }

    @Override
    public List<SampleElement> getElements() throws InvalidXmlElementException {
        return readXml(SampleElement.class);
    }
}
