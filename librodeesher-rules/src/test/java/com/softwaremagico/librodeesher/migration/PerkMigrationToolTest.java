package com.softwaremagico.librodeesher.migration;

import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.perk.Perk;
import com.softwaremagico.librodeesher.perk.PerkBonus;
import com.softwaremagico.librodeesher.perk.PerkBonusKind;
import com.softwaremagico.librodeesher.perk.PerkChoiceGrant;
import com.softwaremagico.librodeesher.perk.PerkChoiceScope;
import com.softwaremagico.librodeesher.perk.PerkGrade;
import com.softwaremagico.librodeesher.perk.PerkType;
import com.softwaremagico.librodeesher.resistance.ResistanceType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Verifies {@link PerkMigrationTool} against a small fixture file reproducing the tricky bits of the
 * real data: a weakness (negative cost), a swapped grade/type column pair (a real data typo present
 * in "ManualPersonajes"), and a row whose description was accidentally split by a stray tab.
 */
@Test(groups = "migration")
public class PerkMigrationToolTest {

    @Test
    public void migratesPerksForEveryModuleThatHasThem() throws IOException {
        final Path sourceRoot = Files.createTempDirectory("librodeesher-perk-migration-source");
        final Path targetRoot = Files.createTempDirectory("librodeesher-perk-migration-target");
        try {
            final Path perksDir = sourceRoot.resolve("rolemaster").resolve("modulos")
                    .resolve("ManualPersonajes").resolve("talentos");
            Files.createDirectories(perksDir);
            Files.writeString(perksDir.resolve("talentos.txt"), String.join("\n",
                    "# Nombre\tCoste\tPermitido\tGrado\tTipo\tBonus\tDescripcion",
                    "#########\t#########\t#########\t#######\t#########\t#########\t#########",
                    "Suerte\t30\tTodos\tMáximo\tCapacidad Especial\tNinguno\tTiradas abiertas a 93-100.",
                    "Adicción Ligera\t-20\tTodos\tMáximo\tMental\tNinguno\tConsulta la página 90.",
                    // Swapped grade/type columns, exactly as found in the real data.
                    "Promesa (Máximo)\t-20\tTodos\tMental\tMáximo\tNinguno\tUna promesa.",
                    // Description accidentally split across two tab-separated columns.
                    "Duplicación\t30\tTodos\tMáximo\tCapacidad Especial\tNinguno\tGenera una copia.\tConsulta la página 70.",
                    ""), StandardCharsets.UTF_8);

            final int written = PerkMigrationTool.migrate(sourceRoot, targetRoot);
            Assert.assertEquals(written, 1);

            final List<Perk> perks = readGeneratedFile(targetRoot.resolve("CharacterLaw/perks.xml"));
            Assert.assertEquals(perks.size(), 4);

            final Perk suerte = findById(perks, "luck");
            Assert.assertEquals(suerte.getCost(), Integer.valueOf(30));
            Assert.assertFalse(suerte.isWeakness());
            Assert.assertTrue(suerte.isAvailableToEveryone());
            Assert.assertEquals(suerte.getGrade(), PerkGrade.MAXIMUM);
            Assert.assertEquals(suerte.getType(), PerkType.SPECIAL);
            Assert.assertTrue(suerte.getBonuses().isEmpty());
            Assert.assertTrue(suerte.getChoiceGrants().isEmpty());

            final Perk addiction = findById(perks, "minorAddiction");
            Assert.assertTrue(addiction.isWeakness());
            Assert.assertEquals(addiction.getType(), PerkType.MENTAL);

            // Legacy behaviour preserved: an unrecognized grade tag ("Mental") falls back to MAXIMUM,
            // and an unrecognized type tag ("Máximo") falls back to OTHER.
            final Perk swappedColumns = findById(perks, "vowMaximum");
            Assert.assertEquals(swappedColumns.getGrade(), PerkGrade.MAXIMUM);
            Assert.assertEquals(swappedColumns.getType(), PerkType.OTHER);

            final Perk duplication = findById(perks, "duplication");
            Assert.assertEquals(duplication.getDescription().getSpanish(), "Genera una copia. Consulta la página 70.");
            Assert.assertFalse(duplication.getDescription().getEnglish().isBlank());
        } finally {
            deleteRecursively(sourceRoot);
            deleteRecursively(targetRoot);
        }
    }

    @Test
    public void migratesEveryBonusFlavor() throws IOException {
        final Path sourceRoot = Files.createTempDirectory("librodeesher-perk-bonus-source");
        final Path targetRoot = Files.createTempDirectory("librodeesher-perk-bonus-target");
        try {
            Files.createDirectories(sourceRoot.resolve("rolemaster"));
            Files.writeString(sourceRoot.resolve("rolemaster/categorias.txt"), String.join("\n",
                    "Atletismo·Gimnasia(AtlGim)\tAg/Fu/Ag\tEstándar\tAcrobacia, Trepar",
                    "Autocontrol(Auto)\tAd/Ad/Ad\tEstándar\tResistir Dolor",
                    ""), StandardCharsets.UTF_8);

            final Path perksDir = sourceRoot.resolve("rolemaster").resolve("modulos")
                    .resolve("ManualPersonajes").resolve("talentos");
            Files.createDirectories(perksDir);
            Files.writeString(perksDir.resolve("talentos.txt"), String.join("\n",
                    "# Nombre\tCoste\tPermitido\tGrado\tTipo\tBonus\tDescripcion",
                    "#########\t#########\t#########\t#######\t#########\t#########\t#########",
                    "Trepador\t10\tTodos\tMenor\tAdiestramiento Especial\tTrepar (20), Ag (5), TR Esencia (25), Apariencia (-10), TA (4), Movimiento (10), Atletismo·Gimnasia (Común), Trepar (10*), Autocontrol (+4/r), TR Reino (50)\tDescripcion.",
                    "Todoterreno\t15\tTodos\tMenor\tAdiestramiento Especial\t{Cualquier Categoría} (10)[1], {Atletismo·Gimnasia} (5)[2]\tDescripcion.",
                    ""), StandardCharsets.UTF_8);

            final int written = PerkMigrationTool.migrate(sourceRoot, targetRoot);
            Assert.assertEquals(written, 1);
            final List<Perk> perks = readGeneratedFile(targetRoot.resolve("CharacterLaw/perks.xml"));

            final Perk climber = findById(perks, "trepador");
            final List<PerkBonus> bonuses = climber.getBonuses();
            Assert.assertEquals(bonuses.size(), 10);

            Assert.assertEquals(bonuses.get(0).getSkillId(), "climbing");
            Assert.assertEquals(bonuses.get(0).getKind(), PerkBonusKind.FLAT);
            Assert.assertEquals(bonuses.get(0).getValue(), Integer.valueOf(20));

            Assert.assertEquals(bonuses.get(1).getCharacteristic(), CharacteristicAbbreviation.AGILITY);
            Assert.assertEquals(bonuses.get(1).getValue(), Integer.valueOf(5));

            Assert.assertEquals(bonuses.get(2).getResistanceType(), ResistanceType.ESSENCE);
            Assert.assertEquals(bonuses.get(2).getValue(), Integer.valueOf(25));

            Assert.assertTrue(bonuses.get(3).isAppearance());
            Assert.assertEquals(bonuses.get(3).getValue(), Integer.valueOf(-10));

            Assert.assertTrue(bonuses.get(4).isArmor());
            Assert.assertEquals(bonuses.get(4).getValue(), Integer.valueOf(4));

            Assert.assertTrue(bonuses.get(5).isMovement());
            Assert.assertEquals(bonuses.get(5).getValue(), Integer.valueOf(10));

            Assert.assertEquals(bonuses.get(6).getCategoryId(), "athleticsGymnastics");
            Assert.assertEquals(bonuses.get(6).getKind(), PerkBonusKind.MAKES_COMMON);
            Assert.assertNull(bonuses.get(6).getValue());

            Assert.assertEquals(bonuses.get(7).getSkillId(), "climbing");
            Assert.assertEquals(bonuses.get(7).getKind(), PerkBonusKind.CONDITIONAL);
            Assert.assertEquals(bonuses.get(7).getValue(), Integer.valueOf(10));

            Assert.assertEquals(bonuses.get(8).getCategoryId(), "selfControl");
            Assert.assertEquals(bonuses.get(8).getKind(), PerkBonusKind.PER_RANK);
            Assert.assertEquals(bonuses.get(8).getValue(), Integer.valueOf(4));

            // "TR Reino" is a dynamic, per-character resistance not modeled yet.
            Assert.assertNull(bonuses.get(9).getResistanceType());
            Assert.assertEquals(bonuses.get(9).getUnresolvedTargetId(), "realm");
            Assert.assertEquals(bonuses.get(9).getValue(), Integer.valueOf(50));

            final Perk allRounder = findById(perks, "todoterreno");
            Assert.assertEquals(allRounder.getChoiceGrants().size(), 2);
            Assert.assertEquals(allRounder.getChoiceGrants().get(0).getScope(), PerkChoiceScope.ANY_CATEGORY);
            Assert.assertEquals(allRounder.getChoiceGrants().get(0).getOptionsToChoose(), Integer.valueOf(1));
            Assert.assertEquals(allRounder.getChoiceGrants().get(0).getValue(), Integer.valueOf(10));
            Assert.assertEquals(allRounder.getChoiceGrants().get(1).getCategoryId(), "athleticsGymnastics");
            Assert.assertNull(allRounder.getChoiceGrants().get(1).getScope());
            Assert.assertEquals(allRounder.getChoiceGrants().get(1).getOptionsToChoose(), Integer.valueOf(2));
        } finally {
            deleteRecursively(sourceRoot);
            deleteRecursively(targetRoot);
        }
    }

    private static List<Perk> readGeneratedFile(Path file) throws IOException {
        Assert.assertTrue(Files.isRegularFile(file), "expected generated file at " + file);
        try (var inputStream = Files.newInputStream(file)) {
            return com.softwaremagico.librodeesher.ObjectMapperFactory.getXmlObjectMapper()
                    .readerForListOf(Perk.class).readValue(inputStream);
        }
    }

    private static Perk findById(List<Perk> perks, String id) {
        return perks.stream().filter(perk -> perk.getId().equals(id)).findFirst()
                .orElseThrow(() -> new AssertionError("Perk '" + id + "' not found"));
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (var walk = Files.walk(path)) {
            walk.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (final IOException e) {
                    // Best effort cleanup of a temporary test directory.
                }
            });
        }
    }
}
