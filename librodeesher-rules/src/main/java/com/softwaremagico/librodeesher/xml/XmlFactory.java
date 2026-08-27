package com.softwaremagico.librodeesher.xml;

import com.softwaremagico.librodeesher.Element;
import com.softwaremagico.librodeesher.ObjectMapperFactory;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.exceptions.ResourceNotFoundException;
import com.softwaremagico.librodeesher.file.ModuleManager;
import com.softwaremagico.librodeesher.file.PathManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Reads a rulebook XML file (one per element type, e.g. {@code razas.xml}) from every enabled module
 * and exposes the combined list of elements.
 *
 * <p>Concrete subclasses (one per element type, e.g. {@code RaceFactory}) only need to provide:</p>
 * <ul>
 *     <li>{@link #getXmlFile()}: the file name to look for inside each module folder.</li>
 *     <li>{@link #getElements()}: usually a one-line call to {@link #readXml(Class)}.</li>
 * </ul>
 *
 * <p>Data is looked up purely through the classpath ({@link ClassLoader#getResourceAsStream}), which
 * keeps this class Android-compatible: the same lookup works whether {@code modulo/} is packaged
 * inside a desktop jar or inside an Android asset/resource. No {@code java.io.File} access,
 * {@code ProtectionDomain} inspection or AWT/Swing dependency is used, unlike the legacy
 * implementation.</p>
 *
 * @param <T> concrete element type handled by this factory.
 */
public abstract class XmlFactory<T extends Element> {

    private static final Logger LOGGER = LoggerFactory.getLogger(XmlFactory.class);

    /** Id -> element, populated lazily by {@link #readXml(Class)} and cleared by {@link #reset()}. */
    private Map<String, T> elementsById;

    protected XmlFactory() {
        ModuleManager.registerResettable(this::reset);
    }

    /** Name of the XML file (relative to each module folder) holding this factory's elements. */
    public abstract String getXmlFile();

    /** Returns every element visible with the currently enabled modules. */
    public abstract List<T> getElements() throws InvalidXmlElementException;

    /** Drops the cached elements, forcing the next {@link #getElements()} call to read the XML again. */
    public void reset() {
        elementsById = null;
    }

    /**
     * Returns the element with the given id.
     *
     * @throws InvalidXmlElementException if no enabled module defines such an id.
     */
    public T getElement(String id) throws InvalidXmlElementException {
        if (elementsById == null) {
            getElements();
        }
        if (id == null || id.isEmpty()) {
            return null;
        }
        final T element = elementsById.get(id);
        if (element == null) {
            throw new InvalidXmlElementException(
                    getClass().getName() + " has no element with id '" + id + "'.");
        }
        return element;
    }

    /** Returns every element sharing the given (nullable) group tag. */
    public List<T> getElementsByGroup(String group) throws InvalidXmlElementException {
        return getElements().stream().filter(element -> Objects.equals(group, element.getGroup())).toList();
    }

    /**
     * Reads {@link #getXmlFile()} from every enabled module and merges the results.
     *
     * <p>Modules are combined in {@link ModuleManager#getEnabledModules()} order: if two modules
     * define an element with the same id (e.g. an optional module overriding a core rule), the last
     * one read wins. Modules that do not define the requested file at all are silently skipped,
     * since not every module has every element type (e.g. only "RazasYCulturas" defines
     * {@code culturas.xml}).</p>
     */
    protected List<T> readXml(Class<T> entityClass) throws InvalidXmlElementException {
        final Map<String, T> combined = new LinkedHashMap<>();
        for (final String module : ModuleManager.getEnabledModules()) {
            try {
                for (final T element : readModuleXml(entityClass, module)) {
                    element.setModuleName(module);
                    combined.put(element.getId(), element);
                }
            } catch (final ResourceNotFoundException e) {
                LOGGER.debug("Module '{}' does not define '{}'.", module, getXmlFile());
            }
        }
        elementsById = combined;
        return new ArrayList<>(combined.values());
    }

    private List<T> readModuleXml(Class<T> entityClass, String module) throws ResourceNotFoundException,
            InvalidXmlElementException {
        final String resourcePath = PathManager.getModulePath(module) + getXmlFile();
        try (InputStream inputStream = openResource(resourcePath)) {
            if (inputStream == null) {
                throw new ResourceNotFoundException("Resource not found at '" + resourcePath + "'.");
            }
            return ObjectMapperFactory.getXmlObjectMapper().readerForListOf(entityClass).readValue(inputStream);
        } catch (final IOException e) {
            throw new InvalidXmlElementException("Error reading xml file '" + resourcePath + "'.", e);
        }
    }

    /**
     * Opens the given classpath resource, returning {@code null} if it does not exist. Kept as a
     * separate method (instead of inlining {@code getClass().getClassLoader()...}) so tests can rely
     * on it and so the lookup strategy is documented in a single place.
     */
    private InputStream openResource(String resourcePath) {
        final InputStream fromContextLoader = Thread.currentThread().getContextClassLoader() != null
                ? Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)
                : null;
        if (fromContextLoader != null) {
            return fromContextLoader;
        }
        return XmlFactory.class.getClassLoader().getResourceAsStream(resourcePath);
    }
}
