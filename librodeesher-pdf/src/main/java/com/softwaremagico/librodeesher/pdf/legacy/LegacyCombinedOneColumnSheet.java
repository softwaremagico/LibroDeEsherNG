package com.softwaremagico.librodeesher.pdf.legacy;

import java.util.ArrayList;
import java.util.List;

import com.lowagie.text.BadElementException;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.RGBColor;
import com.softwaremagico.librodeesher.category.Category;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;

/**
 * A port of the legacy {@code PdfCombinedSheet1Column}, the one-column "Hoja de Habilidades"
 * export, onto {@link LegacyCombinedSheet}.
 *
 * <p>Like the legacy class it extends the two-columns sheet and changes the numbers of the layout:
 * one column per page instead of two, 69 abstract lines per column instead of 71, a body font of 8
 * points instead of 6, 33 characters for both the skill and the category names, and its own column
 * widths (a wide name column and seven narrow value columns inside a main table that spans 90% of
 * the page instead of two halves). Because the first column is wider, the row of column titles
 * spells the same columns with longer words.</p>
 *
 * <p>Unlike the two-columns sheet, which follows the 2.2.1 base, the one-column sheet only ever
 * existed in the 2016 class, written against a base that had already drifted from 2.2.1. So the
 * pieces that differ are reproduced from that base: the page is titled {@code Hoja de Habilidades}
 * on a black bar, the columns are packed with its recursive "move categories to the last column"
 * rule, and its blank-line arithmetic (subtract a line for short columns, round the spread up, and
 * put the spare rows of a category back into the count) and its wider filler placeholders are
 * used.</p>
 */
public class LegacyCombinedOneColumnSheet extends LegacyCombinedSheet {

	private static final int MAX_LINES_PER_COLUMN = 69;
	private static final int COLUMNS_PER_PAGE = 1;
	private static final int SKILL_NAME_LENGTH = 33;
	private static final int CATEGORY_NAME_LENGTH = 33;
	private static final int MIN_CATEGORIES_PER_COLUMN = 4;
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

	/**
	 * The one-column sheet only existed in the 2.3.0 application, whose footer advertised that
	 * version (unlike the two-columns sheet, which is a 2.2.1 export).
	 */
	@Override
	protected String footerVersion() {
		return "2.3.0";
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

	@Override
	protected String sheetTitle() {
		return "Hoja de Habilidades";
	}

	@Override
	protected void applyTitleBackground(PdfPCell cell) {
		cell.setBackgroundColor(new RGBColor(0, 0, 0));
	}

	/**
	 * The legacy 2016 {@code getCategoriesPerColumn}: as soon as a column would overflow, a new one
	 * is started, and afterwards, if the last page would be left half empty or with too few
	 * categories, the packing is retried with one more spare line per category.
	 */
	@Override
	protected void obtainCategoriesPerColumn() throws InvalidXmlElementException {
		columns = packCategories(getMinEmptySkillsPerCategory());
	}

	private List<List<Category>> packCategories(int emptyLinesPerCategory) throws InvalidXmlElementException {
		List<List<Category>> packed = new ArrayList<>();
		List<Category> currentColumn = new ArrayList<>();
		int currentColumnLines = 0;

		for (final Category category : availableCategories()) {
			int linesByCategory = getCategoryLines(category, emptyLinesPerCategory);

			// If the category does not fit in the current column, start another one.
			if (currentColumnLines + linesByCategory > getMaxLinesPerColumn() && !currentColumn.isEmpty()) {
				packed.add(currentColumn);
				currentColumn = new ArrayList<>();
				currentColumnLines = 0;
			}

			currentColumn.add(category);
			currentColumnLines += linesByCategory;
		}
		packed.add(currentColumn);

		// If the last page would be left half empty, or its column with too few categories, move
		// categories to it by making every category need one more spare line.
		if (packed.size() % getColumnsPerPage() == 1
				|| packed.get(packed.size() - 1).size() < MIN_CATEGORIES_PER_COLUMN) {
			return packCategories(emptyLinesPerCategory + 1);
		}

		return packed;
	}

	@Override
	protected int getColumnEmptyLines(List<Category> column) throws InvalidXmlElementException {
		int emptyLines = super.getColumnEmptyLines(column);
		// Category rows are not exactly a multiple of skill rows, so short columns need one less.
		if (column.size() < 7) {
			emptyLines--;
		}
		return emptyLines;
	}

	/**
	 * The 2016 base already reserves one spare skill line per category when measuring a column, an
	 * extra line the 2.2.1 base (and so {@link LegacyCombinedSheet}) does not count. Without it the
	 * one-column sheet over-estimates the blank space to fill and prints too many filler rows.
	 */
	@Override
	protected int getMinNeededColumnLines(List<Category> column) throws InvalidXmlElementException {
		return super.getMinNeededColumnLines(column) + column.size() * getMinEmptySkillsPerCategory();
	}

	@Override
	protected int getCategoryEmptyLines(int emptyLinesToAdd, List<Category> column, int index) {
		return (int) Math.ceil(Math.max(0, emptyLinesToAdd) / (double) (column.size() - index))
				+ getMinEmptySkillsPerCategory();
	}

	@Override
	protected int getReaddedEmptySkills() {
		return getMinEmptySkillsPerCategory();
	}

	@Override
	protected String getEmptySkillName() {
		return "_________________________________";
	}

	@Override
	protected String getEmptySkillPreviousRanks() {
		return "______";
	}

	@Override
	protected String getEmptySkillValue() {
		return "______";
	}
}
