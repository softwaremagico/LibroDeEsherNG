package com.softwaremagico.librodeesher.perk;

import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.xml.XmlFactory;

import java.util.List;

/**
 * Reads {@code talentos.xml} from every enabled rulebook module.
 */
public class PerkFactory extends XmlFactory<Perk> {

    private static final String XML_FILE = "perks.xml";

    private static final class PerkFactoryHolder {
        private static final PerkFactory INSTANCE = new PerkFactory();
    }

    public static PerkFactory getInstance() {
        return PerkFactoryHolder.INSTANCE;
    }

    @Override
    public String getXmlFile() {
        return XML_FILE;
    }

    @Override
    public List<Perk> getElements() throws InvalidXmlElementException {
        return readXml(Perk.class);
    }
}
