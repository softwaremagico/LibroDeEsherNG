package com.softwaremagico.librodeesher.pdf.legacy;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import com.lowagie.text.BadElementException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.softwaremagico.librodeesher.category.Category;
import com.softwaremagico.librodeesher.magic.MagicListType;
import com.softwaremagico.librodeesher.category.CategoryType;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.equipment.BonusType;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.skill.Skill;

/**
 * A port of the legacy {@code com.softwaremagico.librodeesher.pj.export.pdf.PdfCombinedSheet} (the
 * two-columns "Hoja Combinada de Habilidades" export) onto OpenPDF and the NG model.
 *
 * <p>Kept as a mechanical translation of the legacy class, like {@link LegacyCharacterSheet}: the
 * two pages it shares with the standard sheet are drawn by it, and every combined page is first
 * given the {@code RMHPComb.png} drawing as an UNDERLYING background image, then a two-column
 * {@link PdfPTable} with an absolute total width is painted over it with {@code writeSelectedRows}.</p>
 *
 * <p>The categories are walked in catalog order and packed into columns of at most
 * {@link #getMaxLinesPerColumn()} abstract lines (a category block counts as {@value #CATEGORY_SIZE}
 * lines plus one line per interesting skill and one per specialization, plus
 * {@link #getMinEmptySkillsPerCategory()} spare line); the columns that make a page are given by
 * {@link #getColumnsPerPage()} and the surplus lines of a column are handed out to its categories as
 * blank skill rows. That arithmetic is what decided the page count and the filler rows of the printed
 * sheets, so it is reproduced exactly. Every layout number lives behind a protected getter, and the
 * one-column variant {@link LegacyCombinedOneColumnSheet} only overrides those.</p>
 */
public class LegacyCombinedSheet extends LegacyCharacterSheet {

	private final static int DEFAULT_MAX_LINES_PER_COLUMN = 71;
	private final static int CATEGORY_SIZE = 4;
	private final static int SKILL_SIZE = 1;
	private final static int DEFAULT_COLUMNS_PER_PAGE = 2;
	private final static int BORDER = 0;
	private final static int DEFAULT_SKILL_NAME_LENGTH = 30;
	private final static int DEFAULT_CATEGORY_NAME_LENGTH = 33;
	private final static int DEFAULT_MIN_EMPTY_SKILLS_PER_CATEGORY = 1;

	private final static int DEFAULT_FONT_SIZE = 6;

	private static final float VERY_LIGHT_GRAY = 215f / 255f;

	private final CharacterPlayer characterPlayer;
	private int page = 0;
	private PdfPTable tableColumn;
	private PdfPTable table;
	protected List<List<Category>> columns;

	public LegacyCombinedSheet(CharacterPlayer characterPlayer) {
		super(characterPlayer, false);
		this.characterPlayer = characterPlayer;
	}

	@Override
	public void render(Document document, PdfWriter writer) throws BadElementException, DocumentException,
			InvalidXmlElementException {
		try {
			writePages(document, writer);
		} catch (final IOException e) {
			// The only IO left in here is reading the bundled background images, so a failure means
			// the JAR is broken rather than anything the caller could retry.
			throw new DocumentException(new java.io.IOException("Cannot read the sheet background images.", e));
		}
	}

	private void writePages(Document document, PdfWriter writer) throws BadElementException, DocumentException,
			IOException, InvalidXmlElementException {
		// The combined sheet had no second face of its own: it printed the talentos page only when
		// the character had something to write on it.
		twoFaced = (characterPlayer.getSelectedPerks().size() > 0
				|| !LegacyValues.raceSpecials(characterPlayer).isEmpty()
				|| !characterPlayer.getAllNotMagicEquipment().isEmpty() || !characterPlayer.getAllMagicItems().isEmpty());

		characteristicsPage(document, writer);
		if (twoFaced) {
			equipmentPage(document, writer);
		}
		combinedPage(document, writer);
	}

	private void combinedPage(Document document, PdfWriter writer) throws BadElementException, MalformedURLException,
			DocumentException, IOException, InvalidXmlElementException {
		createMainTable(document);
		document.newPage();
		createBackgroundImage(document, LegacySheetAssets.getCombinedPage());
		combinedSkillsPage(document, writer);
	}

	/** The legacy main table: two equal columns drawn with an absolute width on top of the page. */
	private void createMainTable(Document document) {
		setMainTable(new PdfPTable(getMainTableWidths()));
		getMainTable().flushContent();
		getMainTable().getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
		getMainTable().setTotalWidth(document.getPageSize().getWidth() - 60);
	}

	/**
	 * The legacy {@code PdfCombinedSheet#combinedSkillsPage}: the header, then every column of
	 * categories, painting a blank skill row per spare line of the column.
	 */
	private void combinedSkillsPage(Document document, PdfWriter writer) throws MalformedURLException,
			BadElementException, IOException, DocumentException, InvalidXmlElementException {
		PdfPCell cell;

		cell = new PdfPCell(createHeader());
		cell.setColspan(getColumnsPerPage());
		getMainTable().addCell(cell);

		setColumnTable(new PdfPTable(1));

		int columnIndex = 1;
		for (List<Category> column : columns) {
			int emptyLinesToAdd = getColumnEmptyLines(column);
			for (int index = 0; index < column.size(); index++) {
				final Category category = column.get(index);

				// Add the category title.
				getColumnTable().addCell(categoryCell(category));

				// Add the title for the skill list.
				getColumnTable().addCell(skillTitleCell());

				// Skills list.
				boolean isSpellListCat = MagicListType.fromCategoryId(category.getId()) != null;
				for (Skill skill : LegacyValues.skillsOf(characterPlayer, category)) {
					boolean interesting = characterPlayer.isSkillInteresting(skill);
					if (!interesting) {
						// For spell lists, check spell list ranks
						if (isSpellListCat) {
							interesting = characterPlayer.getSpellListTotalRanks(skill.getId()) > 0;
						}
					}
					if (!interesting) {
						continue;
					}
					int currentRanks = isSpellListCat ? characterPlayer.getCurrentLevel().getSpellListRanks(skill.getId())
							: characterPlayer.getCurrentLevel().getSkillRanks(skill.getId());
					int prevRanks = isSpellListCat ? characterPlayer.getSpellListTotalRanks(skill.getId())
							: LegacyValues.previousSkillRanks(characterPlayer, skill);
					int skillRankValue = isSpellListCat ? characterPlayer.getSkillRankValue(category, characterPlayer.getSpellListTotalRanks(skill.getId()))
							: LegacyValues.skillRanksValue(characterPlayer, skill, category);
					PdfPCell skillLineCell = skillLine(skillName(skill), prevRanks + "",
							getNewRanksImage(currentRanks),
							skillRankValue + "",
							LegacyValues.categoryTotalValue(characterPlayer, skill, category) + "",
							characterPlayer.getItemBonus(BonusType.SKILL, skill.getId()) + "", getBonusValue(skill),
							isSpellListCat ? getTotalValue(skill, spellListTotalValue(skill, category, skillRankValue))
									: getTotalValue(skill),
							new Font(getHandWrittingFont(), getFontSize() - 1),
							new Font(getHandWrittingFont(), getFontSize() - 1), getSkillLineWidths());
					getColumnTable().addCell(skillLineCell);

					// Add the specializations.
					if (!isSpellListCat) {
						for (String specialization : characterPlayer.getSkillSpecializations(skill.getId())) {
							PdfPCell skillSpecializedLineCell = skillLine("  " + specialization, "", null,
									characterPlayer.getSpecializedSkillRanks(skill) + "",
									LegacyValues.categoryTotalValue(characterPlayer, skill, category) + "",
									characterPlayer.getItemBonus(BonusType.SKILL, skill.getId()) + "", getBonusValue(skill),
									getTotalSpecializedValue(skill), new Font(getHandWrittingFont(), getFontSize() - 1),
									new Font(getHandWrittingFont(), getFontSize() - 1), getSkillLineWidths());
							getColumnTable().addCell(skillSpecializedLineCell);
						}
					}
				}

				// Empty skill lines to fill gaps:
				int addEmptyLines = getCategoryEmptyLines(emptyLinesToAdd, column, index);

				for (int i = 0; i < addEmptyLines; i++) {
					PdfPCell emptySkillLines = skillLine(getEmptySkillName(), getEmptySkillPreviousRanks(),
							getNewRanksImage(0), getEmptySkillValue(),
							LegacyValues.categoryTotalValue(characterPlayer, category) + "", getEmptySkillValue(),
							getEmptySkillValue(), getEmptySkillValue(),
							new Font(getDefaultFont(), getFontSize() - 1), new Font(getHandWrittingFont(), getFontSize() - 1),
							getSkillLineWidths());
					getColumnTable().addCell(emptySkillLines);
					emptyLinesToAdd--;
				}
				emptyLinesToAdd += getReaddedEmptySkills();
			}

			addColumn(document);

			if (columnIndex % getColumnsPerPage() == 0 && columnIndex < columns.size()) {
				addNewSkillPage(document, writer);
			}
			columnIndex++;
		}

		closePage(document, writer);
	}

	/**
	 * Left-over lines of a column, which the blank skill rows must fill. The one-column sheet
	 * subtracts one because its category rows are not an exact multiple of its skill rows.
	 */
	protected int getColumnEmptyLines(List<Category> column) throws InvalidXmlElementException {
		return getMaxLinesPerColumn() - getMinNeededColumnLines(column);
	}

	/**
	 * The legacy {@code (emptyLinesToAdd / (column.size() - index)) + MIN_EMPTY_SKILLS_PER_CATEGORY}:
	 * the blank rows a category prints, spreading the column surplus over the categories left.
	 */
	protected int getCategoryEmptyLines(int emptyLinesToAdd, List<Category> column, int index) {
		return (emptyLinesToAdd / (column.size() - index)) + getMinEmptySkillsPerCategory();
	}

	/**
	 * The legacy 2016 arithmetic put the spare blank rows of a category back into the running count;
	 * the 2.2.1 sheet this port follows does not.
	 */
	protected int getReaddedEmptySkills() {
		return 0;
	}

	/** The name printed on a blank skill row. */
	protected String getEmptySkillName() {
		return "________________________";
	}

	/** The previous-ranks placeholder printed on a blank skill row. */
	protected String getEmptySkillPreviousRanks() {
		return "__";
	}

	/** The value placeholder printed on a blank skill row. */
	protected String getEmptySkillValue() {
		return "___";
	}

	/**
	 * The legacy {@code PdfCombinedSheet#getBonusValue(Skill)}: the flat bonus of the skill with the
	 * letters that say where it comes from.
	 */
	private String getBonusValue(Skill skill) throws InvalidXmlElementException {
		String bonusText = LegacyValues.skillSimpleBonus(characterPlayer, skill) + "";

		String letter = "";
		if (LegacyValues.backgroundSkillBonus(characterPlayer, skill) > 0) {
			letter += "H";
		}
		if (characterPlayer.getPerkSkillBonus(skill.getId()) != 0
				|| characterPlayer.getPerkSkillConditionalBonus(skill.getId()) != 0) {
			letter += "T";
			if (characterPlayer.getPerkSkillConditionalBonus(skill.getId()) != 0) {
				letter += "*";
			}
		}
		if (!letter.equals("")) {
			bonusText += " (" + letter + ")";
		}
		return bonusText;
	}

	private String getTotalValue(Skill skill) throws InvalidXmlElementException {
		return getTotalValue(skill, characterPlayer.getSkillTotalValue(skill));
	}

	/**
	 * {@link #getTotalValue(Skill)} over a total supplied by the caller, used by spell-list rows
	 * (whose ranks are not those of a catalog skill, see
	 * {@link #spellListTotalValue(Skill, Category, int)}).
	 */
	private String getTotalValue(Skill skill, int total) throws InvalidXmlElementException {
		if (characterPlayer.getItemBonus(BonusType.SKILL, skill.getId()) > 0
				|| characterPlayer.getPerkSkillConditionalBonus(skill.getId()) != 0) {
			return total - characterPlayer.getItemBonus(BonusType.SKILL, skill.getId())
					- characterPlayer.getPerkSkillConditionalBonus(skill.getId()) + "/" + total + "";
		}
		return total + "";
	}

	/**
	 * The legacy {@code getTotalValue(Skill)} for a spell list: the flat bonus and item bonus of the
	 * list plus its own rank value applied to the list's real ranks (the magic list categories have
	 * no per-rank development term in {@code getSkillTotalValue}, whose ranks resolve to 0 for a
	 * synthetic list skill, so it is added here from {@code spellListRankValue})
	 */
	private int spellListTotalValue(Skill skill, Category category, int spellListRankValue)
			throws InvalidXmlElementException {
		return characterPlayer.getSkillTotalValue(skill)
				- characterPlayer.getSkillRankValue(category, characterPlayer.getSkillRealRanks(skill))
				+ spellListRankValue;
	}

	private String getTotalSpecializedValue(Skill skill) throws InvalidXmlElementException {
		if (characterPlayer.getItemBonus(BonusType.SKILL, skill.getId()) > 0
				|| characterPlayer.getPerkSkillConditionalBonus(skill.getId()) != 0) {
			return LegacyValues.specializedSkillTotalValue(characterPlayer, skill)
					- characterPlayer.getItemBonus(BonusType.SKILL, skill.getId())
					- characterPlayer.getPerkSkillConditionalBonus(skill.getId()) + "/"
					+ LegacyValues.specializedSkillTotalValue(characterPlayer, skill) + "";
		}
		return LegacyValues.specializedSkillTotalValue(characterPlayer, skill) + "";
	}

	/**
	 * The legacy {@code TxtSheet#getNameSpecificLength} of the 2015 sheet: the name is cut so that
	 * it and its suffix fit the column, and the suffix that says how the character may use the skill
	 * is appended. A generalized skill is printed under its group name, without the specialization.
	 * The legacy padded the result with spaces up to the column width, which changes nothing on the
	 * page, so only the cut is kept.
	 */
	private String skillName(Skill skill) throws InvalidXmlElementException {
		String name = LegacyTextBlocks.name(skill.getName());
		if (characterPlayer.isSkillGeneralized(skill.getId())) {
			final int colon = name.indexOf(':');
			if (colon > 0) {
				name = name.substring(0, colon);
			}
		}
		final String suffix;
		if (characterPlayer.isSkillRestricted(skill)) {
			suffix = " (r)";
		} else if (characterPlayer.isSkillProfessional(skill)) {
			suffix = " (p)" + (characterPlayer.isSkillGeneralized(skill.getId()) ? " (g)" : "");
		} else if (characterPlayer.isSkillCommon(skill)) {
			suffix = " (c)" + (characterPlayer.isSkillGeneralized(skill.getId()) ? " (g)" : "");
		} else {
			suffix = characterPlayer.isSkillGeneralized(skill.getId()) ? " (g)" : "";
		}
		if (getSkillNameLength() < name.length() + suffix.length()) {
			name = name.substring(0, getSkillNameLength() - suffix.length());
		}
		return name + suffix;
	}

	/** The category block: its title line and its bonuses line, as two rows of one small table. */
	private PdfPCell categoryCell(Category category) throws InvalidXmlElementException, MalformedURLException,
			BadElementException, IOException, DocumentException {
		float[] widths = { 0.23f, 0.18f, 0.19f, 0.11f, 0.13f, 0.16f };
		PdfPTable categoryTable = new PdfPTable(widths);

		Paragraph p = new Paragraph(categoryName(category), new Font(getDefaultFont(), getFontSize() - 1, Font.BOLD));
		PdfPCell cell = getCategoryCell(p);
		cell.setColspan(2);
		categoryTable.addCell(cell);

		p = new Paragraph(LegacyTextBlocks.characteristicTags(category.getCharacteristics()),
				new Font(getDefaultFont(), getFontSize()));
		categoryTable.addCell(getCategoryCell(p));

		p = new Paragraph(LegacyValues.categoryCostTag(characterPlayer, category), new Font(getDefaultFont(), getFontSize()));
		categoryTable.addCell(getCategoryCell(p));

		p = new Paragraph("Rang: " + LegacyValues.previousCategoryRanks(characterPlayer, category),
				new Font(getDefaultFont(), getFontSize()));
		categoryTable.addCell(getCategoryCell(p));

		if (category.getType().equals(CategoryType.STANDARD)) {
			categoryTable.addCell(
					getCategoryCell(getNewRanksImage(characterPlayer.getCurrentLevel().getCategoryRanks(category.getId()))));
		} else {
			if (category.getType().equals(CategoryType.COMBINED)) {
				p = new Paragraph("*", new Font(getDefaultFont(), getFontSize()));
				categoryTable.addCell(getCategoryCell(p));
			}
			if (category.getType().equals(CategoryType.LIMITED) || category.getType().equals(CategoryType.SPECIAL)
					|| category.getType().equals(CategoryType.PPD) || category.getType().equals(CategoryType.PD)) {
				p = new Paragraph("+", new Font(getDefaultFont(), getFontSize()));
				categoryTable.addCell(getCategoryCell(p));
			}
		}

		// Second line.
		p = new Paragraph("Bonif. Rango: " + LegacyValues.categoryRanksValue(characterPlayer, category),
				new Font(getDefaultFont(), getFontSize()));
		categoryTable.addCell(getCategoryCell(p));

		p = new Paragraph("Caract: " + characterPlayer.getCategoryCharacteristicBonus(category),
				new Font(getDefaultFont(), getFontSize()));
		categoryTable.addCell(getCategoryCell(p));

		String text = (LegacyValues.backgroundCategoryBonus(characterPlayer, category)
				+ characterPlayer.getPerkCategoryBonus(category.getId())) + "";
		String letter = "";
		if (LegacyValues.backgroundCategoryBonus(characterPlayer, category) > 0) {
			letter += "H";
		}
		if (characterPlayer.getPerkCategoryBonus(category.getId()) != 0) {
			letter += "T";
			if (characterPlayer.getPerkCategoryConditionalBonus(category.getId()) != 0) {
				letter += "*";
			}
		}
		if (!letter.equals("")) {
			text += " (" + letter + ")";
		}
		p = new Paragraph("Esp: " + text, new Font(getDefaultFont(), getFontSize()));
		categoryTable.addCell(getCategoryCell(p));

		p = new Paragraph("Prof: " + LegacyValues.categoryBonus(category, characterPlayer),
				new Font(getDefaultFont(), getFontSize()));
		categoryTable.addCell(getCategoryCell(p));

		p = new Paragraph("Obj: " + characterPlayer.getItemBonus(BonusType.CATEGORY, category.getId()),
				new Font(getDefaultFont(), getFontSize()));
		categoryTable.addCell(getCategoryCell(p));

		p = new Paragraph("Total: " + LegacyValues.categoryTotalValue(characterPlayer, category),
				new Font(getDefaultFont(), getFontSize()));
		categoryTable.addCell(getCategoryCell(p));

		cell = new PdfPCell(categoryTable);
		cell.setBorderWidth(1);
		cell.setBorderWidthTop(2);
		cell.setColspan(1);
		cell.setMinimumHeight(30);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setGrayFill(VERY_LIGHT_GRAY);

		return cell;
	}

	/** The legacy {@code Category#getName(int)}: the Spanish name cut to fit its column. */
	private String categoryName(Category category) {
		final String name = LegacyTextBlocks.name(category.getName());
		return name.substring(0, Math.min(getCategoryNameLength() - 1, name.length())).toUpperCase();
	}

	private void addColumn(Document document) {
		PdfPCell cell = new PdfPCell(getColumnTable());
		cell.setBorderWidth(1);
		cell.setColspan(1);
		cell.setMinimumHeight(document.getPageSize().getWidth() - 60);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		getMainTable().addCell(cell);

		setColumnTable(new PdfPTable(1));
		getColumnTable().flushContent();
	}

	private void closePage(Document document, PdfWriter writer) throws BadElementException {
		PdfPCell cell = new PdfPCell(createFooter(getFontSize()));
		cell.setBorderWidth(0);
		cell.setColspan(getColumnsPerPage());
		cell.setMinimumHeight(20);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_BOTTOM);
		getMainTable().addCell(cell);
		getMainTable().writeSelectedRows(0, -1, 30, document.getPageSize().getHeight() - 37, writer.getDirectContent());
	}

	/**
	 * The categories the sheet may print, in catalog order: those enabled by the character options
	 * and with a cost.
	 */
	protected List<Category> availableCategories() throws InvalidXmlElementException {
		List<Category> allCategories = new ArrayList<>();
		for (final Category category : LegacyValues.availableCategories()) {
			if (!characterPlayer.isCategoryUseful(category) || !LegacyValues.hasCategoryCost(characterPlayer, category)) {
				continue;
			}
			allCategories.add(category);
		}
		return allCategories;
	}

	/**
	 * The legacy {@code PdfCombinedSheet#obtainCategoriesPerColumn}: walk the categories in catalog
	 * order and start a new column as soon as the next one would not fit in
	 * {@link #getMaxLinesPerColumn()} lines.
	 */
	protected void obtainCategoriesPerColumn() throws InvalidXmlElementException {
		columns = new ArrayList<>();
		List<Category> allCategories = availableCategories();

		if (allCategories.isEmpty()) {
			return;
		}

		List<Category> col = new ArrayList<>();
		int colLines = 0;
		for (int i = 0; i < allCategories.size(); i++) {
			Category category = allCategories.get(i);
			int linesByCategory = getCategoryLines(category);
			if (colLines + linesByCategory > getMaxLinesPerColumn() && !col.isEmpty()) {
				columns.add(col);
				col = new ArrayList<>();
				colLines = 0;
			}
			col.add(category);
			colLines += linesByCategory;
		}
		if (!col.isEmpty()) {
			columns.add(col);
		}
	}

	private int getCategoryLines(Category category) throws InvalidXmlElementException {
		return getCategoryLines(category, getMinEmptySkillsPerCategory());
	}

	protected int getCategoryLines(Category category, int emptyLinesPerCategory) throws InvalidXmlElementException {
		int linesByCategory = CATEGORY_SIZE;
		boolean isSpellListCat = MagicListType.fromCategoryId(category.getId()) != null;
		for (final Skill skill : LegacyValues.skillsOf(characterPlayer, category)) {
			boolean interesting = characterPlayer.isSkillInteresting(skill);
			if (!interesting && isSpellListCat) {
				interesting = characterPlayer.getSpellListTotalRanks(skill.getId()) > 0;
			}
			if (interesting) {
				linesByCategory += SKILL_SIZE;
				if (!isSpellListCat) {
					linesByCategory += characterPlayer.getSkillSpecializations(skill.getId()).size() * SKILL_SIZE;
				}
			}
		}
		linesByCategory += emptyLinesPerCategory * SKILL_SIZE;
		return linesByCategory;
	}
	protected int getMinNeededColumnLines(List<Category> column) throws InvalidXmlElementException {
		int linesByCategory = 0;
		for (final Category category : column) {
			linesByCategory += CATEGORY_SIZE;
			boolean isSpellListCat = MagicListType.fromCategoryId(category.getId()) != null;
			for (final Skill skill : LegacyValues.skillsOf(characterPlayer, category)) {
				boolean interesting = characterPlayer.isSkillInteresting(skill);
				if (!interesting && isSpellListCat) {
					interesting = characterPlayer.getSpellListTotalRanks(skill.getId()) > 0;
				}
				if (interesting) {
					linesByCategory += SKILL_SIZE;
					if (!isSpellListCat) {
						linesByCategory += characterPlayer.getSkillSpecializations(skill.getId()).size() * SKILL_SIZE;
					}
				}
			}
		}
		return linesByCategory;
	}

	/**
	 * The row of titles printed above every skill list. The one-column sheet widens its first
	 * column, so it spells the same columns with longer words.
	 */
	protected PdfPCell skillTitleCell() throws BadElementException {
		final Font titleFont = new Font(getDefaultFont(), getFontSize() - 1, Font.BOLD);
		return skillLine("Habilidad:", "#", null, "Rng:", "Cat", "Obj:", "Esp:", "Total:", titleFont, titleFont,
				getSkillLineWidths());
	}

	protected static PdfPCell skillLine(String skillName, String previousSkillRanks, Image currentLevelRanks,
			String rankValue, String totalCategoryValue, String itemBonus, String totalBonus, String total, Font font,
			Font categoryFont, float[] widths) throws BadElementException {
		Paragraph p;
		PdfPCell cell;
		
		PdfPTable tableHab = new PdfPTable(widths);
		tableHab.flushContent();

		// Skill name.
		p = new Paragraph(skillName, font);
		cell = getSkillCell(p);
		cell.setBorderWidthRight(0);
		cell.setBorderWidthTop(0);
		cell.setBorderWidthBottom(0);
		tableHab.addCell(cell);

		// Previous ranks.
		p = new Paragraph(previousSkillRanks, font);
		tableHab.addCell(getSkillCell(p));

		// Current ranks.
		if (currentLevelRanks != null) {
			tableHab.addCell(getSkillCell(currentLevelRanks));
		} else {
			p = new Paragraph("", font);
			tableHab.addCell(getSkillCell(p));
		}

		// Rank value.
		p = new Paragraph(rankValue, font);
		tableHab.addCell(getSkillCell(p));

		// Category value.
		p = new Paragraph(totalCategoryValue, categoryFont);
		tableHab.addCell(getSkillCell(p));

		// Magic items.
		p = new Paragraph(itemBonus, font);
		tableHab.addCell(getSkillCell(p));

		// Bonus.
		p = new Paragraph(totalBonus, font);
		tableHab.addCell(getSkillCell(p));

		// Total.
		p = new Paragraph(total, font);
		cell = getSkillCell(p);
		cell.setBorderWidthLeft(0);
		cell.setBorderWidthTop(0);
		cell.setBorderWidthBottom(0);
		tableHab.addCell(cell);

		PdfPCell skillcell = new PdfPCell(tableHab);
		skillcell.setBorderWidth(0);

		return skillcell;
	}

	private static PdfPCell getSkillCell(Paragraph content) {
		PdfPCell cell = new PdfPCell(content);
		cell.setMinimumHeight(10);
		cell.setBorderWidth(BORDER);
		// The legacy sheet painted every skill cell opaque white over the RMHPComb.png drawing.
		cell.setGrayFill(1f);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setPaddingLeft(5f);

		return cell;
	}

	private static PdfPCell getSkillCell(Image content) {
		PdfPCell cell = new PdfPCell(content);
		cell.setMinimumHeight(10);
		cell.setBorderWidth(BORDER);
		cell.setGrayFill(1f);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setPaddingLeft(5f);

		return cell;
	}

	private PdfPCell getCategoryCell(Paragraph content) {
		PdfPCell cell = new PdfPCell(content);
		cell.setMinimumHeight(15);
		cell.setBorderWidth(0);
		cell.setGrayFill(VERY_LIGHT_GRAY);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		cell.setPaddingLeft(5f);

		return cell;
	}

	private PdfPCell getCategoryCell(Image content) {
		PdfPCell cell = new PdfPCell(content);
		cell.setMinimumHeight(15);
		cell.setBorderWidth(0);
		cell.setGrayFill(VERY_LIGHT_GRAY);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		cell.setPaddingLeft(5f);

		return cell;
	}

	/** The black title of the page on the left and the name of the character on the right. */
	private PdfPCell createHeader() throws BadElementException, InvalidXmlElementException {
		float[] widths = { 0.50f, 0.50f };
		PdfPTable tableC = new PdfPTable(widths);
		tableC.flushContent();
		PdfPCell cell;

		cell = new PdfPCell(leftHeader());
		cell.setBorderWidth(1);
		cell.setColspan(1);
		tableC.addCell(cell);

		cell = new PdfPCell(rightHeader());
		cell.setBorderWidth(1);
		cell.setColspan(1);
		tableC.addCell(cell);

		cell = new PdfPCell(tableC);
		return cell;
	}

	private PdfPCell leftHeader() throws InvalidXmlElementException {
		PdfPCell cell;
		Font f = FontFactory.getFont(FontFactory.TIMES_BOLDITALIC, getFontSize() + 9);
		f.setColor(255, 255, 255);

		obtainCategoriesPerColumn();

		Paragraph p = new Paragraph(sheetTitle() + " (" + (page + 1) + " de "
				+ ((columns.size() + getColumnsPerPage() - 1) / getColumnsPerPage()) + ")", f);
		cell = new PdfPCell(p);
		cell.setMinimumHeight(30);
		cell.setBorderWidth(0);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		cell.setPaddingLeft(5f);
		applyTitleBackground(cell);

		return cell;
	}

	/** The name of the sheet on the page title, before the page counter. */
	protected String sheetTitle() {
		return "Hoja Combinada de Habilidades";
	}

	/** The one-column legacy sheet painted its title white on a black bar. */
	protected void applyTitleBackground(PdfPCell cell) {
	}

	private PdfPCell rightHeader() {
		PdfPTable tableC = new PdfPTable(1);
		tableC.flushContent();
		PdfPCell cell;

		Paragraph p = new Paragraph("Personaje: " + characterPlayer.getName(),
				FontFactory.getFont(FontFactory.HELVETICA, getFontSize()));
		cell = new PdfPCell(p);
		cell.setMinimumHeight(15);
		cell.setBorderWidth(0);
		cell.setHorizontalAlignment(Element.ALIGN_LEFT);
		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		cell.setPaddingLeft(5f);
		tableC.addCell(cell);

		p = new Paragraph("Jugador: ___________________________________", FontFactory.getFont(FontFactory.HELVETICA, getFontSize()));
		cell = new PdfPCell(p);
		cell.setMinimumHeight(15);
		cell.setBorderWidth(0);
		cell.setHorizontalAlignment(Element.ALIGN_LEFT);
		cell.setPaddingLeft(5f);
		tableC.addCell(cell);

		cell = new PdfPCell(tableC);
		return cell;
	}

	private void addNewSkillPage(Document document, PdfWriter writer) throws BadElementException, MalformedURLException,
			IOException, DocumentException, InvalidXmlElementException {
		PdfPCell cell;

		// Close the previous page.
		closePage(document, writer);

		// Create a new one.
		createMainTable(document);

		document.newPage();
		page++;
		createBackgroundImage(document, LegacySheetAssets.getCombinedPage());

		cell = new PdfPCell(createHeader());
		cell.setColspan(getColumnsPerPage());
		getMainTable().addCell(cell);
	}

	private PdfPTable getMainTable() {
		return table;
	}

	private void setMainTable(PdfPTable table) {
		this.table = table;
	}

	private PdfPTable getColumnTable() {
		return tableColumn;
	}

	private void setColumnTable(PdfPTable table) {
		tableColumn = table;
	}

	protected float[] getMainTableWidths() {
		float[] mainWidths = { 0.45f, 0.45f };
		return mainWidths;
	}

	protected float[] getSkillLineWidths() {
		float[] widths = { 0.32f, 0.05f, 0.15f, 0.08f, 0.10f, 0.10f, 0.10f, 0.10f };
		return widths;
	}

	/** Abstract lines a column may hold, including the spare ones. */
	protected int getMaxLinesPerColumn() {
		return DEFAULT_MAX_LINES_PER_COLUMN;
	}

	/** Columns painted side by side on every combined page. */
	protected int getColumnsPerPage() {
		return DEFAULT_COLUMNS_PER_PAGE;
	}

	protected int getSkillNameLength() {
		return DEFAULT_SKILL_NAME_LENGTH;
	}

	protected int getCategoryNameLength() {
		return DEFAULT_CATEGORY_NAME_LENGTH;
	}

	/** Spare blank skill rows printed after every category. */
	protected int getMinEmptySkillsPerCategory() {
		return DEFAULT_MIN_EMPTY_SKILLS_PER_CATEGORY;
	}

	protected int getFontSize() {
		return DEFAULT_FONT_SIZE;
	}
}