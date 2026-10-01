package com.softwaremagico.librodeesher.pdf.legacy;

import com.lowagie.text.BadElementException;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Loads the hand-drawn character sheet assets of the legacy application ({@code
 * com.softwaremagico.librodeesher.pj.export.pdf.PdfStandardSheet}) from this jar instead of from an
 * external Rolemaster installation folder ({@code RolemasterFolderStructure.getSheetFolder()} in
 * the legacy code, where the PNGs shipped inside the Rolemaster data directory rather than the
 * application itself).
 *
 * <p>Assets, copied from {@code LibroDeEsher/rolemaster/fiches/}:</p>
 * <ul>
 *     <li>{@code /sheets/RMHP1.png}: the main/characteristics page.</li>
 *     <li>{@code /sheets/RMHP2.png}: the categories page.</li>
 *     <li>{@code /sheets/RMHP3.png}: the skills page.</li>
 *     <li>{@code /sheets/RMHPComb.png}: the combined-sheets page (used by the combined sheets).</li>
 *     <li>{@code /sheets/cuadros/cuadros0..3.png}: the hand-drawn rank boxes ({@code cuadros0} is
 *     the empty box, {@code cuadros1..3} the one/two/three-new-ranks boxes).</li>
 *     <li>{@code /ArchitectsDaughter.ttf}: the handwriting font used for the character-filled
 *     values ({@code FONT_NAME} in the legacy class).</li>
 * </ul>
 */
public final class LegacySheetAssets {

    /** The version printed in the legacy footer ("...herramienta para Rolemaster V2.0.3"). */
    public static final String VERSION = "2.0.3";

    private static final String SHEETS = "/sheets/";
    private static final String CUADROS = SHEETS + "cuadros/";
    private static final String HANDWRITING_FONT = "/ArchitectsDaughter.ttf";

    private LegacySheetAssets() {
        // Utility class.
    }

    /** The A4 background of the main/characteristics page. */
    public static Image getMainPage() throws BadElementException, MalformedURLException, IOException {
        return getBackground(SHEETS + "RMHP1.png");
    }

    /** The A4 background of the categories page. */
    public static Image getCategoriesPage() throws BadElementException, MalformedURLException, IOException {
        return getBackground(SHEETS + "RMHP2.png");
    }

    /** The A4 background of the skills page. */
    public static Image getSkillsPage() throws BadElementException, MalformedURLException, IOException {
        return getBackground(SHEETS + "RMHP3.png");
    }

    /** The A4 background shared by the combined sheets. */
    public static Image getCombinedPage() throws BadElementException, MalformedURLException, IOException {
        return getBackground(SHEETS + "RMHPComb.png");
    }

    /**
     * The rank box for a number of ranks bought in the current level, as in the legacy
     * {@code getNewRanksImage}: any value other than 1, 2 or 3 (including 0) draws the empty box.
     */
    public static Image getRanksBox(int ranks) throws BadElementException, MalformedURLException, IOException {
        final String name = switch (ranks) {
            case 1 -> "cuadros1.png";
            case 2 -> "cuadros2.png";
            case 3 -> "cuadros3.png";
            default -> "cuadros0.png";
        };
        return getBackground(CUADROS + name);
    }

    /** The handwriting font, embedded like the legacy {@code FontFactory} call did. */
    public static Font getHandwritingFont() throws com.lowagie.text.DocumentException, IOException {
        return FontFactory.getFont(resourceUrl(HANDWRITING_FONT).toExternalForm(), BaseFont.IDENTITY_H,
                BaseFont.EMBEDDED, 0.8f);
    }

    /**
     * The classpath URL of an asset. Assets are packaged inside this jar, so they must be resolved
     * through the class loader: passing the bare path instead would make OpenPDF look for a file
     * relative to the working directory.
     */
    private static URL resourceUrl(String resource) {
        return LegacySheetAssets.class.getResource(resource);
    }

    private static Image getBackground(String resource) throws MalformedURLException, IOException {
        final URL url = resourceUrl(resource);
        if (url == null) {
            throw new FileNotFoundException("Missing sheet asset on the classpath: " + resource);
        }
        final Image image = Image.getInstance(url);
        // Same scaling as the legacy createBackgroundImage: fit within 760x760 so the full-page
        // drawing keeps its proportions inside the A4 frame.
        image.scaleToFit(760, 760);
        return image;
    }
}
