package com.softwaremagico.librodeesher.rules;

import com.softwaremagico.librodeesher.Element;
import com.softwaremagico.librodeesher.ObjectMapperFactory;
import com.softwaremagico.librodeesher.category.Category;
import com.softwaremagico.librodeesher.culture.Culture;
import com.softwaremagico.librodeesher.file.ModuleManager;
import com.softwaremagico.librodeesher.file.PathManager;
import com.softwaremagico.librodeesher.magic.MagicSpellList;
import com.softwaremagico.librodeesher.perk.Perk;
import com.softwaremagico.librodeesher.profession.Profession;
import com.softwaremagico.librodeesher.race.Race;
import com.softwaremagico.librodeesher.skill.Skill;
import com.softwaremagico.librodeesher.training.Training;
import com.softwaremagico.librodeesher.weapon.Weapon;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Every rulebook XML shipped in the library, in every module, must be well formed and must still
 * deserialize completely into its own model.
 *
 * <p>The {@code *Factory} classes only read the files of the <em>enabled</em> modules, so a broken or
 * unparsable file in a disabled module would go unnoticed until somebody enabled it. This walks the
 * modules folder on the classpath directly and checks every file it finds.</p>
 */
@Test(groups = "rules")
public class ModuleXmlFilesTest {

    /** The known rulebook files: the root tag, the element type it holds, and its per element tag. */
    private static final Map<String, RulebookFile> FILES = new LinkedHashMap<>();

    static {
        FILES.put("categories.xml", new RulebookFile("categories", "category", Category.class));
        FILES.put("cultures.xml", new RulebookFile("cultures", "culture", Culture.class));
        FILES.put("perks.xml", new RulebookFile("perks", "perk", Perk.class));
        FILES.put("professions.xml", new RulebookFile("professions", "profession", Profession.class));
        FILES.put("races.xml", new RulebookFile("races", "race", Race.class));
        FILES.put("skills.xml", new RulebookFile("skills", "skill", Skill.class));
        FILES.put("spells.xml", new RulebookFile("spells", "spellList", MagicSpellList.class));
        FILES.put("trainings.xml", new RulebookFile("trainings", "training", Training.class));
        FILES.put("weapons.xml", new RulebookFile("weapons", "weapon", Weapon.class));
    }

    private record RulebookFile(String root, String tag, Class<? extends Element> type) {
    }

    @Test
    public void everyModuleXmlIsWellFormedAndFullyDeserializable() throws Exception {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        // These files are data, not documents: never resolve anything they may point at.
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

        // Resolved through a known module, because a plain "modules/" lookup may hit the test
        // resources folder (which holds the module fixtures of XmlFactoryTest) instead of the data.
        final String corePath = classpathFileOrNull(PathManager.getModulePath(ModuleManager.CORE));
        Assert.assertNotNull(corePath, "the core module is not on the classpath");
        final File modulesFolder = new File(corePath).getParentFile();
        Assert.assertTrue(modulesFolder.isDirectory(), "modules folder not found at " + modulesFolder);

        final Set<String> walkedModules = new TreeSet<>();
        final List<String> absentModules = new ArrayList<>();
        final Set<String> seenFiles = new TreeSet<>();
        int checkedElements = 0;
        for (final String module : ModuleManager.getAllModules()) {
            final File moduleFolder = new File(modulesFolder, module);
            final File[] files = moduleFolder.isDirectory()
                    ? moduleFolder.listFiles((dir, name) -> name.endsWith(".xml"))
                    : null;
            if (files == null) {
                // A module may be declared without shipping a folder (nothing to validate).
                absentModules.add(module);
                continue;
            }
            walkedModules.add(module);
            for (final File file : files) {
                final RulebookFile rulebook = FILES.get(file.getName());
                Assert.assertNotNull(rulebook, "unknown rulebook file '" + file.getName() + "' in module " + module);
                // Well formed: a real XML parser has to accept the whole document.
                final Document document = factory.newDocumentBuilder().parse(file);
                Assert.assertEquals(document.getDocumentElement().getNodeName(), rulebook.root(),
                        "unexpected root tag in " + file);
                // Loadable: every element has to be read, with no field silently dropped. The
                // production mapper tolerates unknown fields on purpose, so the check is stricter.
                final List<Element> elements = strictMapper().readerForListOf(rulebook.type()).readValue(file);
                final Set<String> inFile = idsOf(document);
                final Set<String> loaded = elements.stream().map(Element::getId)
                        .collect(Collectors.toCollection(TreeSet::new));
                Assert.assertEquals(loaded, inFile, "elements lost while reading " + file);
                Assert.assertEquals(elements.size(), inFile.size(), "element count mismatch in " + file);
                seenFiles.add(file.getName());
                checkedElements += elements.size();
            }
        }
        // A folder that exists but is not declared would have its data silently ignored.
        final Set<String> declared = new TreeSet<>(ModuleManager.getAllModules());
        for (final String folder : listFolders(modulesFolder)) {
            Assert.assertTrue(declared.contains(folder),
                    "module folder '" + folder + "' is not declared in ModuleManager, so its data is ignored");
        }
        // Nothing that ships data may be skipped, and every kind of file must be covered.
        Assert.assertEquals(walkedModules, new TreeSet<>(listFolders(modulesFolder)),
                "some module folders were not walked");
        Assert.assertEquals(seenFiles, FILES.keySet(), "some rulebook files were not checked");
        Assert.assertTrue(checkedElements > 0, "no rulebook element was checked");
        Assert.assertFalse(absentModules.contains(ModuleManager.CORE), "the core module folder is missing");
    }

    /** A copy of the production mapper that refuses unknown fields, to catch mistyped data tags. */
    private static com.fasterxml.jackson.databind.ObjectMapper strictMapper() {
        return ObjectMapperFactory.getXmlObjectMapper().copy()
                .enable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    private static List<String> listFolders(File modulesFolder) {
        final String[] names = modulesFolder.list((dir, name) -> new File(dir, name).isDirectory());
        return names == null ? List.of() : Arrays.asList(names);
    }

    private static Set<String> idsOf(Document document) {
        final var nodes = document.getElementsByTagName("id");
        final Set<String> ids = new TreeSet<>();
        final List<String> duplicates = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            final String id = nodes.item(i).getTextContent().strip();
            if (!ids.add(id)) {
                duplicates.add(id);
            }
            Assert.assertFalse(id.isBlank(), "blank id in " + document.getDocumentElement().getNodeName());
        }
        Assert.assertTrue(duplicates.isEmpty(), "duplicated ids: " + duplicates);
        return ids;
    }

    /** Turns a classpath resource path into a filesystem path, {@code null} when it is absent. */
    private static String classpathFileOrNull(String resourcePath) {
        final var url = ModuleXmlFilesTest.class.getClassLoader()
                .getResource(resourcePath.replace('/', File.separatorChar));
        return url == null ? null : new File(url.getFile()).getAbsolutePath();
    }
}
