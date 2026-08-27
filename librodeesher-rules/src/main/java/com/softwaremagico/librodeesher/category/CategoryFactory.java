package com.softwaremagico.librodeesher.category;

import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.xml.XmlFactory;

import java.util.List;

/**
 * Reads {@code categorias.xml} from every enabled rulebook module.
 */
public class CategoryFactory extends XmlFactory<Category> {

    private static final String XML_FILE = "categorias.xml";

    private static final class CategoryFactoryHolder {
        private static final CategoryFactory INSTANCE = new CategoryFactory();
    }

    public static CategoryFactory getInstance() {
        return CategoryFactoryHolder.INSTANCE;
    }

    @Override
    public String getXmlFile() {
        return XML_FILE;
    }

    @Override
    public List<Category> getElements() throws InvalidXmlElementException {
        return readXml(Category.class);
    }
}
