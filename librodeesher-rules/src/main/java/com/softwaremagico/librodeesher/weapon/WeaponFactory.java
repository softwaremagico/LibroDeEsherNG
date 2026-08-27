package com.softwaremagico.librodeesher.weapon;

import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.xml.XmlFactory;

import java.util.List;

/**
 * Reads {@code armas.xml} from every enabled rulebook module.
 */
public class WeaponFactory extends XmlFactory<Weapon> {

    private static final String XML_FILE = "armas.xml";

    private static final class WeaponFactoryHolder {
        private static final WeaponFactory INSTANCE = new WeaponFactory();
    }

    public static WeaponFactory getInstance() {
        return WeaponFactoryHolder.INSTANCE;
    }

    @Override
    public String getXmlFile() {
        return XML_FILE;
    }

    @Override
    public List<Weapon> getElements() throws InvalidXmlElementException {
        return readXml(Weapon.class);
    }

    public List<Weapon> getWeaponsByType(WeaponType type) throws InvalidXmlElementException {
        return getElements().stream().filter(weapon -> weapon.getType() == type).toList();
    }
}
