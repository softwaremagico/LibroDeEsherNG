package com.softwaremagico.librodeesher.profession;

import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.xml.XmlFactory;

import java.util.List;

/**
 * Reads {@code profesiones.xml} from every enabled rulebook module.
 */
public class ProfessionFactory extends XmlFactory<Profession> {

    private static final String XML_FILE = "professions.xml";

    private static final class ProfessionFactoryHolder {
        private static final ProfessionFactory INSTANCE = new ProfessionFactory();
    }

    public static ProfessionFactory getInstance() {
        return ProfessionFactoryHolder.INSTANCE;
    }

    @Override
    public String getXmlFile() {
        return XML_FILE;
    }

    @Override
    public List<Profession> getElements() throws InvalidXmlElementException {
        return readXml(Profession.class);
    }
}
