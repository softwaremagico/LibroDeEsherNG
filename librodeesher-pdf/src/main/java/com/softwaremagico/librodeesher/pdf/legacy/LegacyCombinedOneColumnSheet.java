package com.softwaremagico.librodeesher.pdf.legacy;

import com.lowagie.text.BadElementException;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.softwaremagico.librodeesher.character.CharacterPlayer;

/**
 * A port of the legacy {@code PdfCombinedSheet1Column}, the one-column "Hoja Combinada de
 * Habilidades" export, onto {@link LegacyCombinedSheet}.
 *
 * <p>Like the legacy class it extends the two-columns sheet and changes nothing but the numbers of
 * the layout: one column per page instead of two, 69 abstract lines per column instead of 71, a
 * body font of 8 points instead of 6, 33 characters for both the skill and the category names, and
 * its own column widths (a wide name column and seven narrow value columns inside a main table that
 * spans 90% of the page instead of two halves). Because the first column is wider, the row of
 * column titles spells the same columns with longer words.</p>
 *
 * <p>The rendering itself stays the one already verified for the two-columns sheet: the legacy
 * 2016 class was written against a base that had already drifted from the 2.2.1 sheet this port
 * follows (split label/value paragraphs, another page title, another blank-line arithmetic), so only
 * the one-column specifics are taken from it.</p>
 */
public class LegacyCombinedOneColumnSheet extends LegacyCombinedSheet {

	private static final int MAX_LINES_PER_COLUMN = 69;
	private static final int COLUMNS_PER_PAGE = 1;
	private static final int SKILL_NAME_LENGTH = 33;
	private static final int CATEGORY_NAME_LENGTH = 33;
	private static final int FONT_SIZE = 8;

	public LegacyCombinedOneColumnSheet(CharacterPlayer characterPlayer) {
		super(characterPlayer);
	}

	@Override
	protected int getMaxLinesPerColumn() {
		return MAX_LINES_PER_COLUMN;
	}

	@Override
	protected int getColumnsPerPage() {
		return COLUMNS_PER_PAGE;
	}

	@Override
	protected int getSkillNameLength() {
		return SKILL_NAME_LENGTH;
	}

	@Override
	protected int getCategoryNameLength() {
		return CATEGORY_NAME_LENGTH;
	}

	@Override
	protected int getFontSize() {
		return FONT_SIZE;
	}

	@Override
	protected float[] getMainTableWidths() {
		float[] mainWidths = { 0.90f };
		return mainWidths;
	}

	@Override
	protected float[] getSkillLineWidths() {
		float[] widths = { 0.32f, 0.10f, 0.10f, 0.10f, 0.10f, 0.10f, 0.10f, 0.10f };
		return widths;
	}

	@Override
	protected PdfPCell skillTitleCell() throws BadElementException {
		final Font titleFont = new Font(getDefaultFont(), getFontSize() - 1, Font.BOLD);
		return skillLine("Habilidad:", "Rangos", null, "B. Rangos:", "Categoría", "Objeto:", "Especial:",
				"Total:", titleFont, titleFont, getSkillLineWidths());
	}
}
