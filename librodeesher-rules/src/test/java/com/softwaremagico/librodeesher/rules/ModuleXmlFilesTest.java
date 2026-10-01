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
import java.util.TreeMap;
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

    /**
     * Every skill a category or a profession points at must exist in some module's {@code skills.xml}.
     *
     * <p>A dangling reference is invisible at load time (an id is just a string), yet it silently drops
     * that skill from the character's sheet and from the development-point cost. This is exactly how 56
     * category references broke: {@code Category#namesFromRaw} used to keep a raw token's
     * <code>{...}</code> enable-skills block or its "(R)" suffix attached, so the whole-phrase lookup in
     * {@code Translations} missed and produced per-word ids no skill has
     * ("chiPowerContactoContinuousR" instead of "chiPowerContinuousContact").</p>
     */
    @Test
    public void everySkillReferencedByACategoryOrProfessionExists() throws Exception {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

        final String corePath = classpathFileOrNull(PathManager.getModulePath(ModuleManager.CORE));
        Assert.assertNotNull(corePath, "the core module is not on the classpath");
        final File modulesFolder = new File(corePath).getParentFile();

        final Set<String> skillIds = new TreeSet<>();
        final Map<String, Set<String>> referencesByFile = new TreeMap<>();
        for (final String module : ModuleManager.getAllModules()) {
            final File moduleFolder = new File(modulesFolder, module);
            final File[] files = moduleFolder.isDirectory()
                    ? moduleFolder.listFiles((dir, name) -> name.endsWith(".xml"))
                    : null;
            if (files == null) {
                continue;
            }
            for (final File file : files) {
                final Document document = factory.newDocumentBuilder().parse(file);
                if ("skills.xml".equals(file.getName())) {
                    collect(document, "id", skillIds);
                } else if ("categories.xml".equals(file.getName())) {
                    collect(document, "skill", referencesByFile.computeIfAbsent(file.getPath(), key -> new TreeSet<>()));
                } else if ("professions.xml".equals(file.getName())) {
                    collect(document, "restrictedSkillId",
                            referencesByFile.computeIfAbsent(file.getPath(), key -> new TreeSet<>()));
                }
            }
        }
        Assert.assertFalse(skillIds.isEmpty(), "no skill id was found at all");
        Assert.assertFalse(referencesByFile.isEmpty(), "no category/profession skill reference was found at all");

        final List<String> dangling = new ArrayList<>();
        referencesByFile.forEach((file, referenced) -> {
            for (final String id : referenced) {
                if (!skillIds.contains(id)) {
                    dangling.add(file.substring(modulesFolder.getPath().length() + 1) + " -> " + id);
                }
            }
        });
        Assert.assertEquals(dangling, List.of(), "skill references that no module's skills.xml defines");
    }

    /**
     * A training grants skills and spell lists, and both must exist in the catalogs: the legacy parser
     * could only produce skill slots, so every "Listas Básicas de Hechizos" entry was auto-created as a
     * standalone {@code Skill} that no {@code skills.xml} defines. Those ranks were therefore invisible
     * to {@link com.softwaremagico.librodeesher.level.LevelUp#addSpellListRanks} and to the
     * "more than 5/10 spell lists per level" cost multiplier.
     */
    @Test
    public void everySkillAndSpellListReferencedByATrainingExists() throws Exception {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

        final String corePath = classpathFileOrNull(PathManager.getModulePath(ModuleManager.CORE));
        Assert.assertNotNull(corePath, "the core module is not on the classpath");
        final File modulesFolder = new File(corePath).getParentFile();

        final Set<String> skillIds = new TreeSet<>();
        final Set<String> spellListIds = new TreeSet<>();
        final Map<String, Set<String>> skillReferences = new TreeMap<>();
        final Map<String, Set<String>> spellListReferences = new TreeMap<>();
        for (final String module : ModuleManager.getAllModules()) {
            final File moduleFolder = new File(modulesFolder, module);
            final File[] files = moduleFolder.isDirectory()
                    ? moduleFolder.listFiles((dir, name) -> name.endsWith(".xml"))
                    : null;
            if (files == null) {
                continue;
            }
            for (final File file : files) {
                final Document document = factory.newDocumentBuilder().parse(file);
                if ("skills.xml".equals(file.getName())) {
                    collect(document, "id", skillIds);
                } else if ("spells.xml".equals(file.getName())) {
                    collect(document, "id", spellListIds);
                } else if ("trainings.xml".equals(file.getName())) {
                    collect(document, "skill", skillReferences.computeIfAbsent(file.getPath(), key -> new TreeSet<>()));
                    collect(document, "spellList",
                            spellListReferences.computeIfAbsent(file.getPath(), key -> new TreeSet<>()));
                }
            }
        }
        Assert.assertFalse(skillIds.isEmpty(), "no skill id was found at all");
        Assert.assertFalse(spellListIds.isEmpty(), "no spell list id was found at all");
        Assert.assertFalse(spellListReferences.isEmpty(), "no training spell list reference was found at all");

        final List<String> danglingSkills = new ArrayList<>();
        skillReferences.forEach((file, referenced) -> {
            for (final String id : referenced) {
                if (!skillIds.contains(id) && !KNOWN_LEGACY_TRAINING_SKILL_TOKENS.contains(id)) {
                    danglingSkills.add(file.substring(modulesFolder.getPath().length() + 1) + " -> " + id);
                }
            }
        });
        Assert.assertEquals(danglingSkills, List.of(), "training skill references no module's skills.xml defines");

        final List<String> danglingSpellLists = new ArrayList<>();
        spellListReferences.forEach((file, referenced) -> {
            for (final String id : referenced) {
                if (!spellListIds.contains(id)) {
                    danglingSpellLists.add(file.substring(modulesFolder.getPath().length() + 1) + " -> " + id);
                }
            }
        });
        Assert.assertEquals(danglingSpellLists, List.of(),
                "training spell list references no module's spells.xml defines");
    }

    /**
     * Training skill references that resolve to nothing on purpose: the martial-arts grant of
     * "ManualPersonajes" names two skills the legacy application auto-created at runtime
     * ({@code SkillFactory}) without ever adding them to a {@code categorias.txt} file.
     */
    private static final Set<String> KNOWN_LEGACY_TRAINING_SKILL_TOKENS = Set.of(
            "inmovilizaciones", "strikesNerviosos");

    /**
     * A culture hobby may legally name either a skill ("Cantar") or a weapon/armour type category
     * ("Armas·Filo", "Armadura·Ligera"), because the rulebook lets a culture spend hobby points on
     * gear as well as on skills. Anything else is a dangling reference.
     *
     * <p>This guards a failure the previous test could not see: {@code CultureMigrationTool} used to
     * translate every hobby with {@code Translations.toEnglishId} directly instead of resolving it
     * against the skill catalog, so both "Trepar" and "Escalar" collapsed onto the single id
     * {@code climbing} (silently duplicating a hobby and losing "Escalar"), while real
     * singular/gender variants ("Supervivencia (Bosque)" vs. the catalog's "Supervivencia (Bosques)")
     * produced ids no skill has.</p>
     */
    @Test
    public void everyCultureHobbyResolvesToASkillOrAGearCategory() throws Exception {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

        final String corePath = classpathFileOrNull(PathManager.getModulePath(ModuleManager.CORE));
        Assert.assertNotNull(corePath, "the core module is not on classpath");
        final File modulesFolder = new File(corePath).getParentFile();

        final Set<String> resolvable = new TreeSet<>();
        final Map<String, Set<String>> hobbiesByFile = new TreeMap<>();
        for (final String module : ModuleManager.getAllModules()) {
            final File moduleFolder = new File(modulesFolder, module);
            final File[] files = moduleFolder.isDirectory()
                    ? moduleFolder.listFiles((dir, name) -> name.endsWith(".xml"))
                    : null;
            if (files == null) {
                continue;
            }
            for (final File file : files) {
                final Document document = factory.newDocumentBuilder().parse(file);
                if ("skills.xml".equals(file.getName()) || "categories.xml".equals(file.getName())) {
                    collect(document, "id", resolvable);
                } else if ("cultures.xml".equals(file.getName())) {
                    final Set<String> hobbies = hobbiesByFile.computeIfAbsent(file.getPath(), key -> new TreeSet<>());
                    collect(document, "hobbyId", hobbies);
                    collect(document, "excludedHobbyId", hobbies);
                }
            }
        }
        Assert.assertFalse(resolvable.isEmpty(), "no skill/category id was found at all");
        Assert.assertFalse(hobbiesByFile.isEmpty(), "no culture hobby reference was found at all");

        final List<String> dangling = new ArrayList<>();
        hobbiesByFile.forEach((file, hobbies) -> {
            for (final String id : hobbies) {
                if (!resolvable.contains(id) && !KNOWN_LEGACY_CULTURE_TOKENS.contains(id)) {
                    dangling.add(file.substring(modulesFolder.getPath().length() + 1) + " -> " + id);
                }
            }
        });
        Assert.assertEquals(dangling, List.of(), "culture hobbies that are neither a skill nor a gear category");
    }

    /**
     * Culture hobbies that resolve to nothing on purpose. {@code all}/{@code weapon}/{@code armor} are
     * the legacy "any hobby"/"any weapon"/"any armour" markers and {@code listOfSpells} is the
     * "Lista de Hechizos" slot. The rest are hobby skills the legacy {@code SkillFactory} silently
     * auto-created at runtime and that were never added to any {@code categorias.txt}, so no catalog
     * entry exists for them yet.
     */
    private static final Set<String> KNOWN_LEGACY_CULTURE_TOKENS = Set.of(
            "all", "weapon", "armor", "listOfSpells",
            "coser", "loreOfFaunaAquatic", "loreOfFaunaArctic", "loreOfFloraAquatic",
            "loreRegionalArctic", "supervivenciaAquatic");

    /** Collects the text of every {@code tag} element of {@code document} into {@code into}. */
    private static void collect(Document document, String tag, Set<String> into) {
        final var nodes = document.getElementsByTagName(tag);
        for (int i = 0; i < nodes.getLength(); i++) {
            into.add(nodes.item(i).getTextContent().strip());
        }
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
