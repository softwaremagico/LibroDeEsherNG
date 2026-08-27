package com.softwaremagico.librodeesher.training;

import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.xml.XmlFactory;

import java.util.List;

/**
 * Reads {@code adiestramientos.xml} from every enabled rulebook module.
 */
public class TrainingFactory extends XmlFactory<Training> {

    private static final String XML_FILE = "adiestramientos.xml";

    private static final class TrainingFactoryHolder {
        private static final TrainingFactory INSTANCE = new TrainingFactory();
    }

    public static TrainingFactory getInstance() {
        return TrainingFactoryHolder.INSTANCE;
    }

    @Override
    public String getXmlFile() {
        return XML_FILE;
    }

    @Override
    public List<Training> getElements() throws InvalidXmlElementException {
        return readXml(Training.class);
    }
}
