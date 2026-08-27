package com.softwaremagico.librodeesher.skill;

import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.xml.XmlFactory;

import java.util.List;

/**
 * Reads {@code habilidades.xml} from every enabled rulebook module.
 */
public class SkillFactory extends XmlFactory<Skill> {

    private static final String XML_FILE = "habilidades.xml";

    private static final class SkillFactoryHolder {
        private static final SkillFactory INSTANCE = new SkillFactory();
    }

    public static SkillFactory getInstance() {
        return SkillFactoryHolder.INSTANCE;
    }

    @Override
    public String getXmlFile() {
        return XML_FILE;
    }

    @Override
    public List<Skill> getElements() throws InvalidXmlElementException {
        return readXml(Skill.class);
    }
}
