package com.softwaremagico.librodeesher.race;

import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.xml.XmlFactory;

import java.util.List;

/** Reads {@code races.xml} from every enabled rulebook module. */
public class RaceFactory extends XmlFactory<Race> {

    private static final String XML_FILE = "races.xml";

    private static final class RaceFactoryHolder {
        private static final RaceFactory INSTANCE = new RaceFactory();
    }

    public static RaceFactory getInstance() {
        return RaceFactoryHolder.INSTANCE;
    }

    @Override
    public String getXmlFile() {
        return XML_FILE;
    }

    @Override
    public List<Race> getElements() throws InvalidXmlElementException {
        return readXml(Race.class);
    }
}
