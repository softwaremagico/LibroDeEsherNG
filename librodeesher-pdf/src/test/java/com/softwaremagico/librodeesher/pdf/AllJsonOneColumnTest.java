package com.softwaremagico.librodeesher.pdf;

import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.persistence.CharacterData;
import com.softwaremagico.librodeesher.persistence.CharacterDataMapper;
import com.softwaremagico.librodeesher.persistence.LegacyCharacterJsonImporter;
import com.lowagie.text.pdf.PdfReader;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Test(groups = "pdf")
public class AllJsonOneColumnTest {
    private static final Path RES = Path.of(
            "/workspace/LibroDeEsherWorkspace/LibroDeEsherNG/librodeesher-pdf/src/test/resources");
    private static final Path OUT = Path.of("/tmp/RolemasterPDF");

    @Test
    public void test() throws Exception {
        Files.createDirectories(OUT);
        List<Path> files = new ArrayList<>();
        for (File f : RES.toFile().listFiles()) {
            if (!f.isFile()) {
                continue;
            }
            String name = f.getName().toLowerCase();
            if (name.endsWith(".json")) {
                files.add(f.toPath());
                continue;
            }
            if (name.endsWith(".rlm")) {
                byte[] b = Files.readAllBytes(f.toPath());
                if (b.length > 0 && b[0] == '{') {
                    files.add(f.toPath());
                }
            }
        }
        int ok = 0;
        for (Path f : files) {
            String content = Files.readString(f);
            final CharacterData data = LegacyCharacterJsonImporter.importLegacyJson(content);
            final CharacterPlayer character = CharacterDataMapper.toCharacter(data);
            final byte[] pdf = new LegacyCombinedOneColumnCharacterSheet().generate(character);
            PdfReader reader = new PdfReader(new java.io.ByteArrayInputStream(pdf));
            int pages = reader.getNumberOfPages();
            reader.close();
            String base = f.getFileName().toString();
            base = base.substring(0, base.lastIndexOf('.'));
            Path out = OUT.resolve(base + "_1col.pdf");
            Files.write(out, pdf);
            System.out.println("Wrote " + out + " (" + pages + " pages)");
            ok++;
        }
        System.out.println("OK: " + ok + "/" + files.size());
    }
}
