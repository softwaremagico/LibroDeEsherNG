package com.softwaremagico.librodeesher.culture;

import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.xml.XmlFactory;

import java.util.List;

/** Reads {@code cultures.xml} from every enabled rulebook module. */
public class CultureFactory extends XmlFactory<Culture> {

    private static final String XML_FILE = "cultures.xml";

    private static final class CultureFactoryHolder {
        private static final CultureFactory INSTANCE = new CultureFactory();
    }

    public static CultureFactory getInstance() {
        return CultureFactoryHolder.INSTANCE;
    }

    @Override
    public String getXmlFile() {
        return XML_FILE;
    }

    @Override
    public List<Culture> getElements() throws InvalidXmlElementException {
        return readXml(Culture.class);
    }
}
