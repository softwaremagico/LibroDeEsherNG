package com.softwaremagico.librodeesher.magic;

import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.xml.XmlFactory;

import java.util.List;

/**
 * Reads {@code hechizos.xml} from every enabled rulebook module.
 */
public class MagicSpellListFactory extends XmlFactory<MagicSpellList> {

    private static final String XML_FILE = "hechizos.xml";

    private static final class MagicSpellListFactoryHolder {
        private static final MagicSpellListFactory INSTANCE = new MagicSpellListFactory();
    }

    public static MagicSpellListFactory getInstance() {
        return MagicSpellListFactoryHolder.INSTANCE;
    }

    @Override
    public String getXmlFile() {
        return XML_FILE;
    }

    @Override
    public List<MagicSpellList> getElements() throws InvalidXmlElementException {
        return readXml(MagicSpellList.class);
    }

    public List<MagicSpellList> getSpellLists(RealmOfMagic realm) throws InvalidXmlElementException {
        return getElements().stream().filter(list -> list.getRealm() == realm).toList();
    }

    /** Every spell list a given profession or training name has access to, across every realm. */
    public List<MagicSpellList> getSpellListsOwnedBy(String professionOrTrainingName) throws InvalidXmlElementException {
        return getElements().stream().filter(list -> list.getOwners().contains(professionOrTrainingName)).toList();
    }
}
