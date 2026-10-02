package com.softwaremagico.librodeesher.pdf.legacy;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.lowagie.text.BadElementException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.softwaremagico.librodeesher.category.Category;
import com.softwaremagico.librodeesher.category.CategoryFactory;
import com.softwaremagico.librodeesher.category.CategoryType;
import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.characteristic.Characteristic;
import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.characteristic.Characteristics;
import com.softwaremagico.librodeesher.equipment.BonusType;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.level.Experience;
import com.softwaremagico.librodeesher.magic.RealmOfMagic;
import com.softwaremagico.librodeesher.race.Race;
import com.softwaremagico.librodeesher.resistance.ResistanceType;
import com.softwaremagico.librodeesher.rules.RulesCatalog;
import com.softwaremagico.librodeesher.skill.Skill;
import com.softwaremagico.librodeesher.skill.SkillFactory;

/**
 * A port of the legacy {@code com.softwaremagico.librodeesher.pj.export.pdf.PdfStandardSheet} onto
 * OpenPDF and the NG model.
 *
 * <p>Kept as a mechanical translation of the legacy class: every page first gets its full-page
 * drawing as an UNDERLYING background image, then a {@link PdfPTable} with an absolute total width is
 * painted over it at fixed page coordinates with {@code writeSelectedRows}, so the generated sheet
 * matches the printed one instead of being a generic flow of tables. Only the API calls changed
 * (iText to OpenPDF, legacy {@code CharacterPlayer} accessors to their documented NG equivalents,
 * file-system image paths to the JAR resources in {@link LegacySheetAssets}).</p>
 *
 * <p>Pages are emitted in the legacy order: characteristics, equipment (only when {@code twoFaced}),
 * categories, a blank filler, skills, and a second blank filler.</p>
 */
public class LegacyCharacterSheet {
	/**
	 * iText/OpenPDF's own default page margin. Every page position in this class is an absolute
	 * coordinate measured against it, so it is part of the layout, not a cosmetic setting.
	 */
	public final static float MARGIN = 36;

	private final static String EMPTY_VALUE = "_____";
	private final static String FONT_NAME = "ArchitectsDaughter.ttf";
	private final static int BORDER = 0;
	public final static int MOST_USED_SKILLS_LINES = 16;
	public final static int MOST_USED_ATTACKS_LINES = 6;
	private final static int MAX_FAVOURITE_SKILL_NAME_LENGTH = 27;
	private final static int MAX_FAVOURITE_WEAPON_NAME_LENGTH = 25;

	private CharacterPlayer characterPlayer;
	protected boolean twoFaced;
	private boolean sortedSkills;
	private BaseFont handWrittingFont;
	private BaseFont baseFont;

	public LegacyCharacterSheet(CharacterPlayer characterPlayer, boolean sortedSkills) {
		this.characterPlayer = characterPlayer;
		this.sortedSkills = sortedSkills;
	}

	/**
	 * The font used for every value the player would fill in by hand. The legacy sheet only used the
	 * handwriting font when the character had opted into it, otherwise it fell back to Helvetica.
	 */
	protected BaseFont getHandWrittingFont() throws MalformedURLException, IOException, InvalidXmlElementException {
		if (characterPlayer != null && characterPlayer.isHandWritingFont()) {
			if (handWrittingFont == null) {
				Font font = LegacySheetAssets.getHandwritingFont();
				handWrittingFont = font.getBaseFont();
			}
			return handWrittingFont;
		} else {
			return getDefaultFont();
		}
	}

	protected BaseFont getDefaultFont() {
		if (baseFont == null) {
			String categoriesfont = com.lowagie.text.FontFactory.HELVETICA;
			baseFont = com.lowagie.text.FontFactory.getFont(categoriesfont).getBaseFont();
		}
		return baseFont;
	}

	/**
	 * Writes every page of the sheet onto an already-open {@code document}.
	 *
	 * <p>A null {@code characterPlayer} is not handled here: {@code PdfDocument} renders the blank
	 * version of the same sheet by itself, so this method is only called with a real character.</p>
	 */
	public void render(Document document, PdfWriter writer) throws BadElementException,
			DocumentException, InvalidXmlElementException {
		try {
			writePages(document, writer);
		} catch (final IOException e) {
			// The only IO left in here is reading the bundled background images, so a failure means
			// the JAR is broken rather than anything the caller could retry.
			throw new DocumentException(
					new java.io.IOException("Cannot read the sheet background images.", e));
		}
	}

	private void writePages(Document document, PdfWriter writer) throws BadElementException,
			DocumentException, IOException, InvalidXmlElementException {
		twoFaced = ((characterPlayer.getHistoryText() != null && characterPlayer.getHistoryText().length() > 10)
				|| characterPlayer.getSelectedPerks().size() > 0 || !LegacyValues.raceSpecials(characterPlayer).isEmpty()
				|| !characterPlayer.getAllNotMagicEquipment().isEmpty() || !characterPlayer.getAllMagicItems().isEmpty());

		characteristicsPage(document, writer);
		if (twoFaced) {
			equipmentPage(document, writer);
		}

		categoriesPage(document, writer);
		if (twoFaced) {
			whitePage(document, writer);
		}
		skillPage(document, writer);
		if (twoFaced) {
			whitePage(document, writer);
		}
	}

	/**
	 * The legacy page metadata, kept verbatim so a generated sheet identifies itself exactly as the
	 * printed one did.
	 */
	public Document DocumentData(Document document, PdfWriter writer) throws MalformedURLException, IOException, InvalidXmlElementException {
		document.addTitle("Ficha Personaje Rolemaster");
		document.addAuthor("Software Magico");
		document.addCreator("Libro de Esher - Generador de PJs y PNJs para Rolemaster");
		document.addSubject("Pagina de PJ para Rolemaster");
		document.addKeywords("Rolemaster, PJ, PNJ, Libro de Esher");
		document.addCreationDate();

		return document;
	}

	protected void createBackgroundImage(Document document, Image png) throws MalformedURLException, IOException, InvalidXmlElementException, BadElementException, DocumentException {
		png.setAlignment(Image.MIDDLE | Image.UNDERLYING);
		png.scaleToFit((float) 760, (float) 760);
		document.add(png);
	}

	private void addCategoryTable(Document document, PdfWriter writer) throws MalformedURLException, IOException, InvalidXmlElementException {
		PdfPTable table = new PdfPTable(1);
		table.getDefaultCell().setBorderWidth(BORDER);
		table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
		int fontSize = 8;
		table.setTotalWidth(document.getPageSize().getWidth() - 500);

		Paragraph p;
		if (characterPlayer != null && characterPlayer.getName() != null) {
			final String name = characterPlayer.getName();
			p = new Paragraph(name.substring(0, Math.min(name.length(), 20)), new Font(getHandWrittingFont(),
					fontSize));
		} else {
			p = new Paragraph("", new Font(getDefaultFont(), fontSize + 2));
		}
		PdfPCell cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		table.addCell(cell);

		table.writeSelectedRows(0, -1, 3 * document.getPageSize().getWidth() / 4 + 20, document.getPageSize().getHeight() - 45, writer.getDirectContent());
		table.flushContent();
	}

	private void addCategoryValuesTable(Document document, PdfWriter writer) throws InvalidXmlElementException, DocumentException, MalformedURLException, IOException {
		float[] widths = { 0.23f, 0.09f, 0.07f, 0.07f, 0.085f, 0.065f, 0.065f, 0.065f, 0.065f, 0.065f, 0.065f };
		PdfPTable table = new PdfPTable(widths);
		table.getDefaultCell().setBorderWidth(BORDER);
		table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
		table.setTotalWidth(document.getPageSize().getWidth() - 65);
		int fontSize = 6;
		String text;
		PdfPCell cell;
		Category category = null;
		int omitidas = 0;

		// Add categories and spaces for a new ones.
		for (int i = 0; i < 60 + omitidas; i++) {
			if (i < LegacyValues.availableCategories().size()) {
				category = LegacyValues.availableCategories().get(i);
			}

			if (characterPlayer != null) {
				if (characterPlayer.isCategoryEnabledByOptions(category) || i >= LegacyValues.availableCategories().size()) {

					// Add a category row
					Paragraph p;
					if (i < LegacyValues.availableCategories().size()) {
						text = LegacyTextBlocks.name(category.getName());
						p = new Paragraph(text, new Font(getDefaultFont(), fontSize));
					} else {
						text = "_______________________";
						p = new Paragraph(text, new Font(getDefaultFont(), fontSize));
					}
					cell = new PdfPCell(p);
					cell.setMinimumHeight(11 + (i - omitidas) % 2);
					cell.setBorderWidth(BORDER);
					cell.setHorizontalAlignment(Element.ALIGN_LEFT);
					cell.setPaddingLeft(5f);
					table.addCell(cell);

					if (i < LegacyValues.availableCategories().size()) {
						text = LegacyTextBlocks.characteristicTags(category.getCharacteristics());
						p = new Paragraph(text, new Font(getDefaultFont(), fontSize));
					} else {
						text = "_______";
						p = new Paragraph(text, new Font(getDefaultFont(), fontSize));
					}
					cell = new PdfPCell(p);
					cell.setBorderWidth(BORDER);
					cell.setHorizontalAlignment(Element.ALIGN_CENTER);
					table.addCell(cell);

					if (characterPlayer != null && i < LegacyValues.availableCategories().size()) {
						text = LegacyValues.categoryCostTag(characterPlayer, category);
						p = new Paragraph(text, new Font(getHandWrittingFont(), fontSize));
					} else {
						text = "_________";
						p = new Paragraph(text, new Font(getDefaultFont(), fontSize));
					}

					cell = new PdfPCell(p);
					cell.setBorderWidth(BORDER);
					cell.setHorizontalAlignment(Element.ALIGN_CENTER);
					table.addCell(cell);

					if (i < LegacyValues.availableCategories().size()) {
						if (category.getType().equals(CategoryType.STANDARD)) {
							if (characterPlayer != null) {
								text = LegacyValues.previousCategoryRanks(characterPlayer, category) + "";
								p = new Paragraph(text, new Font(getHandWrittingFont(), fontSize));
							} else {
								text = EMPTY_VALUE;
								p = new Paragraph(text, new Font(getDefaultFont(), fontSize));
							}
						} else {
							text = "na";
							p = new Paragraph(text, new Font(getDefaultFont(), fontSize));
						}
					} else {
						text = EMPTY_VALUE;
						p = new Paragraph(text, new Font(getDefaultFont(), fontSize));
					}
					cell = new PdfPCell(p);
					cell.setBorderWidth(BORDER);
					cell.setHorizontalAlignment(Element.ALIGN_CENTER);
					table.addCell(cell);

					if (i < LegacyValues.availableCategories().size()) {
						if (category.getType().equals(CategoryType.STANDARD)) {
							Image image;
							if (characterPlayer == null) {
								image = LegacySheetAssets.getRanksBox(0);
								image.scalePercent(28);
							} else {
								image = getNewRanksImage(characterPlayer.getCurrentLevel().getCategoryRanks(category.getId()));
							}
							cell = new PdfPCell(image);
						}

						if (category.getType().equals(CategoryType.COMBINED)) {
							text = "*";
							p = new Paragraph(text, new Font(getDefaultFont(), fontSize));
							cell = new PdfPCell(p);
						}
						if (category.getType().equals(CategoryType.LIMITED) || category.getType().equals(CategoryType.SPECIAL)
								|| category.getType().equals(CategoryType.PPD) || category.getType().equals(CategoryType.PD)) {
							text = "+";
							p = new Paragraph(text, new Font(getDefaultFont(), fontSize));
							cell = new PdfPCell(p);
						}
					} else {
						Image image;
						image = LegacySheetAssets.getRanksBox(0);
						image.scalePercent(28);
						cell = new PdfPCell(image);
					}
					cell.setBorderWidth(BORDER);
					cell.setHorizontalAlignment(Element.ALIGN_CENTER);
					table.addCell(cell);

					if (characterPlayer != null && i < LegacyValues.availableCategories().size()) {
						text = LegacyValues.categoryRanksValue(characterPlayer, category) + "";
						p = new Paragraph(text, new Font(getHandWrittingFont(), fontSize));
					} else {
						text = EMPTY_VALUE;
						p = new Paragraph(text, new Font(getDefaultFont(), fontSize));
					}
					cell = new PdfPCell(p);
					cell.setBorderWidth(BORDER);
					cell.setHorizontalAlignment(Element.ALIGN_CENTER);
					table.addCell(cell);

					if (characterPlayer != null && i < LegacyValues.availableCategories().size()) {
						text = characterPlayer.getCategoryCharacteristicBonus(category) + "";
						p = new Paragraph(text, new Font(getHandWrittingFont(), fontSize));
					} else {
						text = EMPTY_VALUE;
						p = new Paragraph(text, new Font(getDefaultFont(), fontSize));
					}
					cell = new PdfPCell(p);
					cell.setBorderWidth(BORDER);
					cell.setHorizontalAlignment(Element.ALIGN_CENTER);
					table.addCell(cell);

					if (characterPlayer != null && i < LegacyValues.availableCategories().size()) {
						text = LegacyValues.categoryBonus(category, characterPlayer) + "";
						p = new Paragraph(text, new Font(getHandWrittingFont(), fontSize));
					} else {
						text = EMPTY_VALUE;
						p = new Paragraph(text, new Font(getDefaultFont(), fontSize));
					}
					cell = new PdfPCell(p);
					cell.setBorderWidth(BORDER);
					cell.setHorizontalAlignment(Element.ALIGN_CENTER);
					table.addCell(cell);

					if (characterPlayer != null && i < LegacyValues.availableCategories().size()) {
						text = (LegacyValues.backgroundCategoryBonus(characterPlayer, category) + characterPlayer.getPerkCategoryBonus(category.getId())) + "";
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
						p = new Paragraph(text, new Font(getHandWrittingFont(), fontSize));
					} else {
						text = EMPTY_VALUE;
						p = new Paragraph(text, new Font(getDefaultFont(), fontSize));
					}

					cell = new PdfPCell(p);
					cell.setBorderWidth(BORDER);
					cell.setHorizontalAlignment(Element.ALIGN_CENTER);
					table.addCell(cell);

					// Magic Items
					if (characterPlayer != null && i < LegacyValues.availableCategories().size()) {
						text = characterPlayer.getItemBonus(BonusType.CATEGORY, category.getId()) + "";
						p = new Paragraph(text, new Font(getHandWrittingFont(), fontSize));
					} else {
						text = EMPTY_VALUE;
						p = new Paragraph(text, new Font(getDefaultFont(), fontSize));
					}
					cell = new PdfPCell(p);
					cell.setBorderWidth(BORDER);
					cell.setHorizontalAlignment(Element.ALIGN_CENTER);
					table.addCell(cell);

					if (characterPlayer != null && i < LegacyValues.availableCategories().size()) {
						text = LegacyValues.categoryTotalValue(characterPlayer, category) + "";
						p = new Paragraph(text, new Font(getHandWrittingFont(), fontSize));
					} else {
						text = EMPTY_VALUE;
						p = new Paragraph(text, new Font(getDefaultFont(), fontSize));
					}
					cell = new PdfPCell(p);
					cell.setBorderWidth(BORDER);
					cell.setHorizontalAlignment(Element.ALIGN_CENTER);
					table.addCell(cell);
				} else {
					omitidas++;
				}
			}
		}

		cell = new PdfPCell(createFooter(fontSize + 1));
		cell.setBorderWidth(BORDER);
		cell.setColspan(11);
		cell.setMinimumHeight(20);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_BOTTOM);
		table.addCell(cell);

		table.writeSelectedRows(0, -1, 34, document.getPageSize().getHeight() - 97, writer.getDirectContent());
		table.flushContent();
	}

	private void addSkillNameTable(Document document, PdfWriter writer) throws MalformedURLException, IOException, InvalidXmlElementException {
		PdfPTable table = new PdfPTable(1);
		table.getDefaultCell().setBorderWidth(BORDER);
		table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
		int fontSize = 8;
		table.setTotalWidth(document.getPageSize().getWidth() - 500);

		Paragraph p;
		if (characterPlayer != null) {
			p = new Paragraph(characterPlayer.getName(), new Font(getHandWrittingFont(), fontSize));
		} else {
			p = new Paragraph("", new Font(getDefaultFont(), fontSize));
		}
		PdfPCell cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		table.addCell(cell);

		if (characterPlayer != null) {
			p = new Paragraph(characterPlayer.getLevel() + "", new Font(getHandWrittingFont(), fontSize));
		} else {
			p = new Paragraph("", new Font(getDefaultFont(), fontSize));
		}
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		table.addCell(cell);

		table.writeSelectedRows(0, -1, 3 * document.getPageSize().getWidth() / 4 - 80, document.getPageSize().getHeight() - 65, writer.getDirectContent());
	}

	private void addSkillLine(Skill skill, int fontSize, PdfPTable table, int line) throws InvalidXmlElementException, BadElementException, MalformedURLException, IOException {
		String text;
		PdfPCell cell;

		Paragraph p;
		if (characterPlayer != null) {
			text = (LegacyValues.skillNameWithSufix(characterPlayer, skill)).trim();
			p = new Paragraph(text, new Font(getHandWrittingFont(), fontSize));
		} else {
			text = "___________________________________________";
			p = new Paragraph(text, new Font(getDefaultFont(), fontSize));
		}
		cell = new PdfPCell(p);
		cell.setMinimumHeight(11 + line % 2);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_LEFT);
		cell.setPaddingLeft(5f);
		table.addCell(cell);

		if (characterPlayer != null) {
			text = LegacyValues.previousSkillRanks(characterPlayer, skill) + "";
			p = new Paragraph(text, new Font(getHandWrittingFont(), fontSize));
		} else {
			text = "__";
			p = new Paragraph(text, new Font(getDefaultFont(), fontSize));
		}
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(cell);

		if (characterPlayer != null) {
			cell = new PdfPCell(getNewRanksImage(characterPlayer.getCurrentLevel().getSkillRanks(skill.getId())));
		} else {
			cell = new PdfPCell(getNewRanksImage(0));
		}
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(cell);

		if (characterPlayer != null) {
			text = "  " + LegacyValues.skillDevelopmentBonus(characterPlayer, skill) + "";
			p = new Paragraph(text, new Font(getHandWrittingFont(), fontSize));
		} else {
			text = "   __";
			p = new Paragraph(text, new Font(getDefaultFont(), fontSize));
		}
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(cell);

		if (characterPlayer != null) {
			text = characterPlayer.getSkillTotalValue(skill) + "";
			p = new Paragraph(text, new Font(getHandWrittingFont(), fontSize));
		} else {
			text = "__";
			p = new Paragraph(text, new Font(getDefaultFont(), fontSize));
		}
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(cell);

		if (characterPlayer != null && characterPlayer.getItemBonus(BonusType.SKILL, skill.getId()) != 0) {
			text = characterPlayer.getItemBonus(BonusType.SKILL, skill.getId()) + "";
			p = new Paragraph(text, new Font(getHandWrittingFont(), fontSize));
		} else {
			text = EMPTY_VALUE;
			p = new Paragraph(text, new Font(getDefaultFont(), fontSize));
		}
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(cell);

		if (characterPlayer != null) {
			text = LegacyValues.skillSimpleBonus(characterPlayer, skill) + "";
			String letter = "";
			if (characterPlayer != null && LegacyValues.backgroundSkillBonus(characterPlayer, skill) > 0) {
				letter += "H";
			}

			if (characterPlayer != null && (characterPlayer.getPerkSkillBonus(skill.getId()) != 0 || characterPlayer.getPerkSkillConditionalBonus(skill.getId()) != 0)) {
				letter += "T";
				if (characterPlayer.getPerkSkillConditionalBonus(skill.getId()) != 0) {
					letter += "*";
				}
			}
			if (!letter.equals("")) {
				text += " (" + letter + ")";
			}
			p = new Paragraph(text, new Font(getHandWrittingFont(), fontSize));
		} else {
			text = "__";
			p = new Paragraph(text, new Font(getDefaultFont(), fontSize));
		}

		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(cell);

		p = new Paragraph(EMPTY_VALUE, new Font(getDefaultFont(), fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(cell);

		if (characterPlayer != null) {
			if (characterPlayer.getItemBonus(BonusType.SKILL, skill.getId()) > 0 || characterPlayer.getPerkSkillConditionalBonus(skill.getId()) > 0) {
				text = characterPlayer.getSkillTotalValue(skill) - characterPlayer.getItemBonus(BonusType.SKILL, skill.getId()) - characterPlayer.getPerkSkillConditionalBonus(skill.getId()) + "/"
						+ characterPlayer.getSkillTotalValue(skill) + "";
			} else {
				text = characterPlayer.getSkillTotalValue(skill) + "";
			}
			p = new Paragraph(text, new Font(getHandWrittingFont(), fontSize));
		} else {
			text = EMPTY_VALUE;
			p = new Paragraph(text, new Font(getDefaultFont(), fontSize));
		}
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(cell);
	}

	private void addSpecializedSkillLine(Skill skill, int fontSize, PdfPTable table, int i, int specializedIndex) throws InvalidXmlElementException, BadElementException, MalformedURLException, IOException {
		String text;
		PdfPCell cell;
		Paragraph p;

		if (characterPlayer != null) {
			text = "  " + LegacyTextBlocks.name(characterPlayer.getSkillSpecializations(skill.getId()).get(specializedIndex));
			p = new Paragraph(text, new Font(getHandWrittingFont(), fontSize));
		} else {
			text = "___________________________________________";
			p = new Paragraph(text, new Font(getDefaultFont(), fontSize));
		}
		cell = new PdfPCell(p);
		cell.setMinimumHeight(11 + i % 2);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_LEFT);
		cell.setPaddingLeft(5f);
		table.addCell(cell);

		p = new Paragraph("", new Font(getHandWrittingFont(), fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(cell);

		cell = new PdfPCell();
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(cell);

		if (characterPlayer != null) {
			text = "  " + characterPlayer.getSpecializedSkillRanks(skill) + "";
			p = new Paragraph(text, new Font(getHandWrittingFont(), fontSize));
		} else {
			text = "   __";
			p = new Paragraph(text, new Font(getDefaultFont(), fontSize));
		}
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(cell);

		if (characterPlayer != null) {
			text = characterPlayer.getSkillTotalValue(skill) + "";
			p = new Paragraph(text, new Font(getHandWrittingFont(), fontSize));
		} else {
			text = "__";
			p = new Paragraph(text, new Font(getDefaultFont(), fontSize));
		}
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(cell);

		if (characterPlayer != null) {
			text = characterPlayer.getItemBonus(BonusType.SKILL, skill.getId()) + "";
			p = new Paragraph(text, new Font(getHandWrittingFont(), fontSize));
		} else {
			text = EMPTY_VALUE;
			p = new Paragraph(text, new Font(getDefaultFont(), fontSize));
		}
		if (text.equals("0")) {
			text = EMPTY_VALUE;
			p = new Paragraph(text, new Font(getDefaultFont(), fontSize));
		}
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(cell);

		if (characterPlayer != null) {
			text = LegacyValues.professionSkillBonus(characterPlayer, skill) + LegacyValues.backgroundSkillBonus(characterPlayer, skill)
					+ characterPlayer.getPerkSkillBonus(skill.getId()) + "";
			String letra = "";
			if (characterPlayer != null && LegacyValues.backgroundSkillBonus(characterPlayer, skill) > 0) {
				letra += "H";
			}

			if (characterPlayer != null && characterPlayer.getPerkSkillBonus(skill.getId()) != 0) {
				letra += "T";
				if (characterPlayer.getPerkSkillConditionalBonus(skill.getId()) != 0) {
					letra += "*";
				}
			}
			if (!letra.equals("")) {
				text += "(" + letra + ")";
			}
			p = new Paragraph(text, new Font(getHandWrittingFont(), fontSize));
		} else {
			text = "__";
			p = new Paragraph(text, new Font(getDefaultFont(), fontSize));
		}
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(cell);

		p = new Paragraph(EMPTY_VALUE, new Font(getHandWrittingFont(), fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(cell);

		if (characterPlayer != null) {
			if (characterPlayer.getItemBonus(BonusType.SKILL, skill.getId()) > 0 || characterPlayer.getPerkSkillConditionalBonus(skill.getId()) > 0) {
				text = LegacyValues.specializedSkillTotalValue(characterPlayer, skill) - characterPlayer.getItemBonus(BonusType.SKILL, skill.getId()) - characterPlayer.getPerkSkillConditionalBonus(skill.getId())
						+ "/" + LegacyValues.specializedSkillTotalValue(characterPlayer, skill) + "";
			} else {
				text = LegacyValues.specializedSkillTotalValue(characterPlayer, skill) + "";
			}
			p = new Paragraph(text, new Font(getHandWrittingFont(), fontSize));
		} else {
			text = EMPTY_VALUE;
			p = new Paragraph(text, new Font(getDefaultFont(), fontSize));
		}
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(cell);
	}

	private void addEmptySkillLine(int fontSize, PdfPTable table, int i) throws InvalidXmlElementException, BadElementException, MalformedURLException, IOException {
		PdfPCell cell;
		Paragraph p = new Paragraph("___________________________________________", new Font(getDefaultFont(), fontSize));
		cell = new PdfPCell(p);
		cell.setMinimumHeight(11 + i % 2);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_LEFT);
		cell.setPaddingLeft(5f);
		table.addCell(cell);

		p = new Paragraph(EMPTY_VALUE, new Font(getDefaultFont(), fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(cell);

		cell = new PdfPCell(getNewRanksImage(0));
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(cell);

		p = new Paragraph("   " + EMPTY_VALUE, new Font(getDefaultFont(), fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(cell);

		p = new Paragraph("  " + EMPTY_VALUE, new Font(getDefaultFont(), fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(cell);

		p = new Paragraph("  " + EMPTY_VALUE, new Font(getDefaultFont(), fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(cell);

		p = new Paragraph(EMPTY_VALUE, new Font(getDefaultFont(), fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(cell);

		p = new Paragraph(EMPTY_VALUE, new Font(getDefaultFont(), fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(cell);

		p = new Paragraph(EMPTY_VALUE, new Font(getDefaultFont(), fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(cell);
	}

	protected void addNewSkillPage(PdfPTable table, Document document, PdfWriter writer, int fontSize) throws InvalidXmlElementException, BadElementException, DocumentException, MalformedURLException, IOException {
		PdfPCell cell;
		// Cerramos pagina anterior.
		cell = new PdfPCell(createFooter(fontSize));
		cell.setBorderWidth(BORDER);
		cell.setColspan(9);
		cell.setMinimumHeight(20);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_BOTTOM);
		table.addCell(cell);
		table.writeSelectedRows(0, -1, 34, document.getPageSize().getHeight() - 129, writer.getDirectContent());
		table.flushContent();
		// Generamos el reverso en blanco.
		if (twoFaced) {
			whitePage(document, writer);
		}
		// Generamos una nueva.
		document.newPage();
		createBackgroundImage(document, LegacySheetAssets.getSkillsPage());
	}

	private int newSkill(PdfPTable table, Document document, PdfWriter writer, int fontsize, float[] widths, List<Skill> skills, int alreadyAddedSkills) throws InvalidXmlElementException, DocumentException, MalformedURLException, IOException {

		for (int j = 0; j < skills.size(); j++) {
			Skill skill = skills.get(j);
			if (alreadyAddedSkills > 56) {
				addNewSkillPage(table, document, writer, fontsize);
				table.flushContent();
				table.getDefaultCell().setBorderWidth(BORDER);
				table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
				table.setTotalWidth(document.getPageSize().getWidth() - 65);
				alreadyAddedSkills = 0;
			}

			if (characterPlayer.isSkillInteresting(skill)) {
				alreadyAddedSkills++;
				addSkillLine(skill, fontsize, table, alreadyAddedSkills);
				for (int m = 0; m < characterPlayer.getSkillSpecializations(skill.getId()).size(); m++) {
					addSpecializedSkillLine(skill, fontsize, table, alreadyAddedSkills, m);
					if (alreadyAddedSkills > 56) {
						addNewSkillPage(table, document, writer, fontsize);
						table.flushContent();
						table.getDefaultCell().setBorderWidth(BORDER);
						table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
						table.setTotalWidth(document.getPageSize().getWidth() - 65);
						alreadyAddedSkills = 0;
					}
					alreadyAddedSkills++;
				}
			}
		}
		return alreadyAddedSkills;
	}

	private void addSkillTable(Document document, PdfWriter writer) throws InvalidXmlElementException, DocumentException, MalformedURLException, IOException {
		float[] widths = { 0.36f, 0.07f, 0.085f, 0.065f, 0.065f, 0.065f, 0.065f, 0.065f, 0.065f };
		PdfPTable table = new PdfPTable(widths);
		table.getDefaultCell().setBorderWidth(BORDER);
		table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
		// table.setWidthPercentage((float)102);
		table.setTotalWidth(document.getPageSize().getWidth() - 65);
		PdfPCell cell;
		int fontsize = 6;
		int skillLines = 0;

		if (characterPlayer != null) {
			// Add skills and add lines for new ones.
			if (!sortedSkills) {
				for (int i = 0; i < LegacyValues.availableCategories().size(); i++) {
					Category category = LegacyValues.availableCategories().get(i);
										skillLines = newSkill(table, document, writer, fontsize, widths, LegacyValues.skillsOf(category), skillLines);
				}
				// Add skills sorted
			} else {
				List<Skill> sortedSkills = SkillFactory.getInstance().getElements();
				skillLines = newSkill(table, document, writer, fontsize, widths, sortedSkills, skillLines);
			}
		}
		while (skillLines < 57) {
			skillLines++;
			addEmptySkillLine(fontsize, table, skillLines);
		}

		cell = new PdfPCell(createFooter(fontsize + 1));
		cell.setBorderWidth(BORDER);
		cell.setColspan(9);
		cell.setMinimumHeight(20);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_BOTTOM);
		table.addCell(cell);

		table.writeSelectedRows(0, -1, 34, document.getPageSize().getHeight() - 129, writer.getDirectContent());
		table.flushContent();
	}

	private void skillPage(Document document, PdfWriter writer) throws InvalidXmlElementException, BadElementException, MalformedURLException, DocumentException, IOException {
		document.newPage();
		createBackgroundImage(document, LegacySheetAssets.getSkillsPage());
		addSkillNameTable(document, writer);
		addSkillTable(document, writer);
	}

	private void categoriesPage(Document document, PdfWriter writer) throws InvalidXmlElementException, BadElementException, MalformedURLException, DocumentException, IOException {
		document.newPage();
		createBackgroundImage(document, LegacySheetAssets.getCategoriesPage());
		addCategoryTable(document, writer);
		addCategoryValuesTable(document, writer);
		// El reverso en blanco para no desentonar.
		// if(twoFaced) PersonajePaginaVacia(document, writer, font);
	}

	private PdfPTable createMainHeader(int fontSize) throws MalformedURLException, IOException, InvalidXmlElementException {
		PdfPCell cell;
		Paragraph p;
		float[] widths = { 0.26f, 0.23f, 0.51f };
		PdfPTable table = new PdfPTable(widths);

		if (characterPlayer != null) {
			p = new Paragraph(Experience.getMinimumExperienceForLevel(characterPlayer.getLevel()) + "", new Font(getHandWrittingFont(), fontSize + 2));
		} else {
			p = new Paragraph("", new Font(getHandWrittingFont(), fontSize + 2));
		}
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_BOTTOM);
		cell.setPaddingBottom(10f);
		table.addCell(cell);

		if (characterPlayer != null) {
			p = new Paragraph(characterPlayer.getLevel() + "", new Font(getHandWrittingFont(), fontSize + 2));
		} else {
			p = new Paragraph("", new Font(getHandWrittingFont(), fontSize + 2));
		}
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_BOTTOM);
		cell.setPaddingBottom(10f);
		table.addCell(cell);

		if (characterPlayer != null) {
			p = new Paragraph(characterPlayer.getName(), new Font(getHandWrittingFont(), fontSize));
		} else {
			p = new Paragraph("", new Font(getHandWrittingFont(), fontSize + 2));
		}
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		cell.setPaddingRight(15f);
		cell.setVerticalAlignment(Element.ALIGN_TOP);
		cell.setPaddingTop(5f);
		table.addCell(cell);

		return table;
	}

	private PdfPTable createResistenceTable(int fontSize, ResistanceType resistence, CharacteristicAbbreviation characteristicAbbreviature) throws MalformedURLException, IOException, InvalidXmlElementException {
		PdfPCell cell;
		Paragraph p;
		float[] widths = { 0.37f, 0.15f, 0.15f, 0.165f, 0.15f };
		PdfPTable tablaResistencia = new PdfPTable(widths);

		p = new Paragraph("", new Font(getHandWrittingFont(), fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tablaResistencia.addCell(cell);

		if (characterPlayer != null) {
			p = new Paragraph(LegacyValues.raceResistanceBonus(characterPlayer.getRace(), resistence) + "", new Font(getHandWrittingFont(), fontSize));
		} else {
			p = new Paragraph(" " + EMPTY_VALUE, new Font(getDefaultFont(), fontSize));
		}
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tablaResistencia.addCell(cell);

		if (characterPlayer != null) {
			p = new Paragraph(characterPlayer.getCharacteristicTotalBonus(characteristicAbbreviature) * 3 + "", new Font(getHandWrittingFont(), fontSize));
		} else {
			p = new Paragraph(" " + EMPTY_VALUE, new Font(getDefaultFont(), fontSize));
		}
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		tablaResistencia.addCell(cell);

		p = new Paragraph("", new Font(getDefaultFont(), fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tablaResistencia.addCell(cell);

		if (characterPlayer != null) {
			p = new Paragraph(characterPlayer.getResistanceTotalBonus(resistence) + "    ", new Font(getHandWrittingFont(), fontSize));
		} else {
			p = new Paragraph(EMPTY_VALUE, new Font(getDefaultFont(), fontSize));
		}
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tablaResistencia.addCell(cell);

		return tablaResistencia;
	}

	private PdfPTable createResistenceTable(int fontSize) throws MalformedURLException, IOException, InvalidXmlElementException {
		PdfPCell cell;
		Paragraph p;
		PdfPTable tablaResistencias = new PdfPTable(1);

		p = new Paragraph("", new Font(getHandWrittingFont(), fontSize));
		cell = new PdfPCell(p);
		cell.setColspan(4);
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(30);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tablaResistencias.addCell(cell);

		cell = new PdfPCell(createResistenceTable(fontSize, ResistanceType.CHANNELING, CharacteristicAbbreviation.INTUITION));

		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(10);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tablaResistencias.addCell(cell);

		cell = new PdfPCell(createResistenceTable(fontSize, ResistanceType.ESSENCE, CharacteristicAbbreviation.EMPATHY));

		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(11);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tablaResistencias.addCell(cell);

		cell = new PdfPCell(createResistenceTable(fontSize, ResistanceType.MENTALISM, CharacteristicAbbreviation.PRESENCE));
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(10);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tablaResistencias.addCell(cell);

		cell = new PdfPCell(createResistenceTable(fontSize, ResistanceType.DISEASE, CharacteristicAbbreviation.CONSTITUTION));
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(11);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tablaResistencias.addCell(cell);

		cell = new PdfPCell(createResistenceTable(fontSize, ResistanceType.POISON, CharacteristicAbbreviation.CONSTITUTION));
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(10);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tablaResistencias.addCell(cell);

		cell = new PdfPCell(createResistenceTable(fontSize, ResistanceType.FEAR, CharacteristicAbbreviation.SELF_DISCIPLINE));
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(11);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tablaResistencias.addCell(cell);

		return tablaResistencias;
	}

	private PdfPTable createBDTable(int fontSize) throws MalformedURLException, IOException, InvalidXmlElementException {
		PdfPCell cell;
		Paragraph p;
		PdfPTable tableDefenseive = new PdfPTable(1);

		if (characterPlayer != null) {
			p = new Paragraph((characterPlayer.getCharacteristicTotalBonus(CharacteristicAbbreviation.QUICKNESS) * 3) + "    ", new Font(getHandWrittingFont(),
					fontSize));
		} else {
			p = new Paragraph("_________", new Font(getDefaultFont(), fontSize));
		}
		cell = new PdfPCell(p);
		cell.setColspan(4);
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(30);
		cell.setPaddingRight(5);
		cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		tableDefenseive.addCell(cell);

		return tableDefenseive;
	}

	private PdfPTable createTableRace(int fontSize) throws MalformedURLException, IOException, InvalidXmlElementException {
		PdfPCell cell;
		Paragraph p;
		PdfPTable tablaRaza = new PdfPTable(1);

		p = new Paragraph("", new Font(getHandWrittingFont(), fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(14);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tablaRaza.addCell(cell);

		if (characterPlayer != null) {
			p = new Paragraph(LegacyValues.raceSoulDepartTime(characterPlayer) + "", new Font(getHandWrittingFont(), fontSize));
		} else {
			p = new Paragraph(EMPTY_VALUE, new Font(getDefaultFont(), fontSize));
		}
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(14);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_BOTTOM);
		tablaRaza.addCell(cell);

		if (characterPlayer != null) {
			p = new Paragraph(LegacyValues.raceRestorationTime(characterPlayer) + "   ", new Font(getHandWrittingFont(), fontSize));
		} else {
			p = new Paragraph(EMPTY_VALUE + "             ", new Font(getDefaultFont(), fontSize));
		}
		cell = new PdfPCell(p);
		cell.setColspan(2);
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(14);
		cell.setPaddingRight(10f);
		cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		tablaRaza.addCell(cell);

		if (characterPlayer != null) {
			p = new Paragraph(LegacyValues.progressionRankValues(characterPlayer.getRace(), "physicalDevelopment"), new Font(
					getHandWrittingFont(), fontSize + 2));
		} else {
			p = new Paragraph("   ", new Font(getHandWrittingFont(), fontSize + 2));
		}
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(23);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_BOTTOM);
		cell.setPaddingBottom(5);
		tablaRaza.addCell(cell);

		if (characterPlayer != null) {
			p = new Paragraph(LegacyValues.progressionValues(characterPlayer.getPowerPointsDevelopmentCost()), new Font(getHandWrittingFont(),
					fontSize + 2));
		} else {
			p = new Paragraph("   ", new Font(getHandWrittingFont(), fontSize + 2));
		}
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(23);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_BOTTOM);
		cell.setPaddingBottom(5);
		tablaRaza.addCell(cell);

		return tablaRaza;
	}

	private PdfPTable createTableInterpretation(int fontSize) throws MalformedURLException, IOException, InvalidXmlElementException {
		PdfPCell cell;
		Paragraph p;
		PdfPTable tabla = new PdfPTable(1);
		String text;
		String line;

		p = new Paragraph("", new Font(getHandWrittingFont(), fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(13);
		cell.setPaddingRight(5f);
		cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		tabla.addCell(cell);

		if (characterPlayer != null) {
			// Apparence line length depending on digits.
			if (characterPlayer.getAppearanceTotal() < 10) {
				line = "  ________________________";
			} else if (characterPlayer.getAppearanceTotal() < 100) {
				line = "  _______________________";
			} else {
				line = "  ______________________";
			}
			text = "(" + characterPlayer.getAppearanceTotal() + ")";
			Paragraph p1 = new Paragraph(line, new Font(getDefaultFont(), fontSize));
			Paragraph p2 = new Paragraph(text, new Font(getHandWrittingFont(), fontSize));
			p = new Paragraph();
			p.add(p1);
			p.add(p2);
		} else {
			text = "____________________________";
			p = new Paragraph(text, new Font(getDefaultFont(), fontSize));
		}
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(11);
		cell.setPaddingRight(6f);
		cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		tabla.addCell(cell);

		p = new Paragraph("", new Font(getHandWrittingFont(), fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(11);
		cell.setPaddingRight(5f);
		cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		tabla.addCell(cell);

		p = new Paragraph(characterPlayer.getCurrentAge() + "/" + LegacyValues.raceExpectedLifeYears(characterPlayer), new Font(getHandWrittingFont(), fontSize - 1));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(11);
		cell.setPaddingRight(5f);
		cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		tabla.addCell(cell);

		if (characterPlayer != null) {
			text = "   " + characterPlayer.getSex().getTag();
			p = new Paragraph(text, new Font(getHandWrittingFont(), fontSize));
		} else {
			text = "_____________";
			p = new Paragraph(text, new Font(getDefaultFont(), fontSize));
		}
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(12);
		cell.setPaddingLeft(26f);
		cell.setHorizontalAlignment(Element.ALIGN_LEFT);
		tabla.addCell(cell);

		return tabla;
	}

	private PdfPTable createHistoryTable(int fontSize) throws MalformedURLException, IOException, InvalidXmlElementException {
		PdfPCell cell;
		Paragraph p;
		PdfPTable tabla = new PdfPTable(1);
		String text;

		p = new Paragraph("", new Font(getHandWrittingFont(), fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(16);
		cell.setPaddingRight(5f);
		cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		tabla.addCell(cell);

		for (int i = 0; i < 8; i++) {
			p = new Paragraph("", new Font(getHandWrittingFont(), fontSize));
			cell = new PdfPCell(p);
			cell.setBorderWidth(BORDER);
			cell.setMinimumHeight(11);
			cell.setPaddingRight(5f);
			cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
			tabla.addCell(cell);
		}

		String line = "______________________________";
		if (characterPlayer != null) {
			text = LegacyValues.cultureName(characterPlayer);
			p = new Paragraph(text, new Font(getHandWrittingFont(), fontSize));
		} else {
			text = line;
			p = new Paragraph(text, new Font(getDefaultFont(), fontSize));
		}
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(11);
		cell.setPaddingRight(6f);
		cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		tabla.addCell(cell);

		return tabla;
	}

	private PdfPTable createCharacterDataTable(int fontSize) throws MalformedURLException, IOException, InvalidXmlElementException {
		PdfPCell cell;
		Paragraph p;
		PdfPTable tabla = new PdfPTable(1);
		String texto;

		if (characterPlayer != null) {
			texto = LegacyValues.raceName(characterPlayer);
		} else {
			texto = "";
		}
		p = new Paragraph(texto, new Font(getHandWrittingFont(), fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(12);
		cell.setPaddingRight(5f);
		cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		tabla.addCell(cell);

		if (characterPlayer != null) {
			texto = LegacyValues.professionName(characterPlayer);
		} else {
			texto = "";
		}
		p = new Paragraph(texto, new Font(getHandWrittingFont(), fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(12);
		cell.setPaddingRight(5f);
		cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		tabla.addCell(cell);

		p = new Paragraph("", new Font(getHandWrittingFont(), fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(12);
		cell.setPaddingRight(5f);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tabla.addCell(cell);

		if (characterPlayer != null) {
			texto = "";
			List<String> trainings = LegacyValues.selectedTrainings(characterPlayer);
			for (int i = 0; i < trainings.size(); i++) {
				texto += trainings.get(i);
				if (i < trainings.size() - 1) {
					texto += ", ";
				}
			}
		} else {
			texto = "";
		}
		p = new Paragraph(texto, new Font(getHandWrittingFont(), fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(12);
		cell.setPaddingRight(5f);
		cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		tabla.addCell(cell);

		if (characterPlayer != null) {
			texto = "";
			for (RealmOfMagic realm : characterPlayer.getRealmsOfMagic()) {
				if (texto.length() > 0) {
					texto += "/";
				}
				texto += realm.getTag();
			}
		} else {
			texto = "";
		}
		p = new Paragraph(texto, new Font(getHandWrittingFont(), fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(12);
		cell.setPaddingRight(5f);
		cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		tabla.addCell(cell);

		return tabla;
	}

	private PdfPTable createArmourTable(int fontSize) throws MalformedURLException, IOException, InvalidXmlElementException {
		PdfPCell cell;
		Paragraph p;
		PdfPTable tabla = new PdfPTable(1);
		String text;

		text = "";
		if (LegacyValues.raceNaturalArmorType(characterPlayer) != 1) {
			text = "(" + LegacyValues.raceNaturalArmorType(characterPlayer) + ")";
			String line = " ______________________";
			Paragraph p1 = new Paragraph(text, new Font(getHandWrittingFont(), fontSize));
			Paragraph p2 = new Paragraph(line, new Font(getDefaultFont(), fontSize));
			p = new Paragraph();
			p.add(p2);
			p.add(p1);
		} else {
			text = "_________________________";
			p = new Paragraph(text, new Font(getDefaultFont(), fontSize));
		}
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(13);
		cell.setPaddingRight(5f);
		cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		tabla.addCell(cell);

		text = "";
		p = new Paragraph(text, new Font(getDefaultFont(), fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(11);
		cell.setPaddingRight(5f);
		cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		tabla.addCell(cell);

		if (characterPlayer != null) {
			text = characterPlayer.getMovementCapacity() + "";
		} else {
			text = "";
		}
		p = new Paragraph(text, new Font(getHandWrittingFont(), fontSize + 1));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(12);
		cell.setPaddingRight(5f);
		cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		tabla.addCell(cell);

		return tabla;
	}

	private PdfPTable createMainPageLeftFrame(int fontSize) throws MalformedURLException, IOException, InvalidXmlElementException {
		PdfPCell cell;
		PdfPTable tablaIzquierda = new PdfPTable(1);

		cell = new PdfPCell(createCharacterDataTable(fontSize));
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(60);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tablaIzquierda.addCell(cell);

		cell = new PdfPCell(createArmourTable(fontSize));
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(65);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tablaIzquierda.addCell(cell);

		cell = new PdfPCell(createBDTable(fontSize + 1));
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(75);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tablaIzquierda.addCell(cell);

		cell = new PdfPCell(createResistenceTable(fontSize - 1));
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(130);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tablaIzquierda.addCell(cell);

		cell = new PdfPCell(createTableRace(fontSize + 1));
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(86);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tablaIzquierda.addCell(cell);

		cell = new PdfPCell(createTableInterpretation(fontSize + 1));
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(138);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tablaIzquierda.addCell(cell);

		cell = new PdfPCell(createHistoryTable(fontSize + 1));
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(100);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tablaIzquierda.addCell(cell);

		return tablaIzquierda;
	}

	private PdfPTable createRuneTable(int fontSize) throws MalformedURLException, IOException, InvalidXmlElementException {
		float[] widths = { 0.79f, 0.21f };
		PdfPTable tableFrame = new PdfPTable(widths);
		Paragraph p;
		PdfPCell cell;

		cell = new PdfPCell(createCharacteristicsTable(fontSize));
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tableFrame.addCell(cell);

		p = new Paragraph("", new Font(getHandWrittingFont(), fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tableFrame.addCell(cell);

		return tableFrame;
	}

	private PdfPTable createMostUsedAttacksTable(List<Skill> favouriteAttacks, int fontSize) throws MalformedURLException, IOException, InvalidXmlElementException {
		PdfPTable tableFrame = new PdfPTable(1);
		Paragraph p;
		PdfPCell cell;

		// Header
		p = new Paragraph("", new Font(getHandWrittingFont(), fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(25);
		cell.setColspan(2);
		tableFrame.addCell(cell);

		int favouriteSkillsNumber = 0;
		for (int i = 0; i < (favouriteAttacks.size() < MOST_USED_SKILLS_LINES ? favouriteAttacks.size() : MOST_USED_SKILLS_LINES); i++) {
			cell = new PdfPCell(createMostUsedAttackLine(
					" " + LegacyTextBlocks.getWeaponNameOfLength(LegacyTextBlocks.name(favouriteAttacks.get(i).getName())),
					characterPlayer.getSkillTotalRanks(favouriteAttacks.get(i).getId()) + "", characterPlayer.getSkillTotalValue(favouriteAttacks.get(i)) + "",
					getHandWrittingFont(), fontSize));
			cell.setBorderWidth(BORDER);
			cell.setMinimumHeight((float) 8);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			tableFrame.addCell(cell);
			favouriteSkillsNumber++;
		}

		for (int i = favouriteSkillsNumber; i < MOST_USED_ATTACKS_LINES; i++) {
			cell = new PdfPCell(createMostUsedAttackLine("_______________________", "______", "_______", getDefaultFont(), fontSize));
			cell.setBorderWidth(BORDER);
			cell.setMinimumHeight((float) 9);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			tableFrame.addCell(cell);
		}

		return tableFrame;
	}

	private PdfPTable createMostUsedAttackLine(String skillName, String skillRanks, String skillTotal, BaseFont font, int fontSize) throws MalformedURLException, IOException, InvalidXmlElementException {
		float[] widths = { 3.1f, 1f, 1f, 1f, 5f };
		PdfPTable tableFrame = new PdfPTable(widths);

		// Name
		Paragraph p = new Paragraph(skillName, new Font(font, fontSize));
		PdfPCell cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight((float) 9);
		cell.setHorizontalAlignment(Element.ALIGN_LEFT);
		tableFrame.addCell(cell);

		// Ranks
		p = new Paragraph(skillRanks, new Font(font, fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight((float) 9);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tableFrame.addCell(cell);

		// Total
		p = new Paragraph(skillTotal, new Font(font, fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight((float) 9);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tableFrame.addCell(cell);

		// CriticalFailure
		p = new Paragraph("______", new Font(getDefaultFont(), fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight((float) 9);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tableFrame.addCell(cell);

		// Range
		p = new Paragraph("_______________________________________", new Font(getDefaultFont(), fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight((float) 9);
		cell.setHorizontalAlignment(Element.ALIGN_LEFT);
		tableFrame.addCell(cell);

		return tableFrame;
	}

	private PdfPTable createMostUsedSkillsTable(int fontSize) throws MalformedURLException, IOException, InvalidXmlElementException {
		float[] widths = { 0.49f, 0.51f };
		PdfPTable tableFrame = new PdfPTable(widths);
		Paragraph p;
		PdfPCell cell;

		// Header
		p = new Paragraph("", new Font(getHandWrittingFont(), fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(25);
		cell.setColspan(2);
		tableFrame.addCell(cell);

		List<Skill> skillsToAdd = new ArrayList<>();
		skillsToAdd.addAll(characterPlayer.getFavouriteNoOffensiveSkills());
		cell = new PdfPCell(createMostUsedSkillsColumn(skillsToAdd, fontSize));
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tableFrame.addCell(cell);

		cell = new PdfPCell(createMostUsedSkillsColumn(skillsToAdd, fontSize));
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tableFrame.addCell(cell);

		return tableFrame;
	}

	private PdfPTable createMostUsedSkillsColumn(List<Skill> favouriteSkills, int fontSize) throws MalformedURLException, IOException, InvalidXmlElementException {
		PdfPTable tableFrame = new PdfPTable(1);

		int skillsShowed = 0;
		List<Skill> skillsToAdd = new ArrayList<>(favouriteSkills);
		for (int i = 0; i < (skillsToAdd.size() < MOST_USED_SKILLS_LINES ? skillsToAdd.size() : MOST_USED_SKILLS_LINES); i++) {
			PdfPCell cell = new PdfPCell(createMostUsedSkillLine(
					" " + LegacyTextBlocks.getFavouriteSkillNameOfLength(LegacyTextBlocks.name(skillsToAdd.get(i).getName())),
					characterPlayer.getSkillTotalRanks(skillsToAdd.get(i).getId()) + "", characterPlayer.getSkillTotalValue(skillsToAdd.get(i)) + "", getHandWrittingFont(),
					fontSize));
			cell.setBorderWidth(BORDER);
			cell.setMinimumHeight((float) 8);
			tableFrame.addCell(cell);
			favouriteSkills.remove(skillsToAdd.get(i));
			skillsShowed++;
		}

		for (int i = skillsShowed; i < MOST_USED_SKILLS_LINES; i++) {
			PdfPCell cell = new PdfPCell(createMostUsedSkillLine(" ____________________________", "_____", "_____", getDefaultFont(), fontSize));
			cell.setBorderWidth(BORDER);
			cell.setMinimumHeight((float) 8);
			tableFrame.addCell(cell);
		}

		return tableFrame;
	}

	private PdfPTable createMostUsedSkillLine(String skillName, String skillRanks, String skillTotal, BaseFont font, int fontSize) throws MalformedURLException, IOException, InvalidXmlElementException {
		float[] widths = { 4f, 1f, 1f };
		PdfPTable tableFrame = new PdfPTable(widths);

		// Name
		Paragraph p = new Paragraph(skillName, new Font(font, fontSize));
		PdfPCell cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight((float) 8);
		cell.setHorizontalAlignment(Element.ALIGN_LEFT);
		tableFrame.addCell(cell);

		// Ranks
		p = new Paragraph(skillRanks, new Font(font, fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight((float) 8);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tableFrame.addCell(cell);

		// Total
		p = new Paragraph(skillTotal, new Font(font, fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight((float) 8);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tableFrame.addCell(cell);

		return tableFrame;
	}

	private PdfPTable createCharacteristicsTable(int fontSize) throws MalformedURLException, IOException, InvalidXmlElementException {
		float[] widths = { 0.30f, 0.10f, 0.10f, 0.10f, 0.10f, 0.10f, 0.20f };
		PdfPTable tablaCaracteristicas = new PdfPTable(widths);
		Paragraph p;
		PdfPCell cell;

		p = new Paragraph("", new Font(getHandWrittingFont(), fontSize));
		cell = new PdfPCell(p);
		cell.setMinimumHeight((float) 20);
		cell.setColspan(10);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tablaCaracteristicas.addCell(cell);

		int i = 0;
		for (Characteristic characteristic : Characteristics.getCharacteristics()) {
			p = new Paragraph("", new Font(getHandWrittingFont(), fontSize));
			cell = new PdfPCell(p);
			cell.setBorderWidth(BORDER);
			cell.setMinimumHeight((float) 13.5);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			tablaCaracteristicas.addCell(cell);

			if (characterPlayer != null) {
				p = new Paragraph(characterPlayer.getCharacteristicTemporalValue(characteristic.getAbbreviation()) + "", new Font(getHandWrittingFont(),
						fontSize));
			} else {
				p = new Paragraph("  " + EMPTY_VALUE, new Font(getDefaultFont(), fontSize));
			}
			cell = new PdfPCell(p);
			cell.setBorderWidth(BORDER);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			tablaCaracteristicas.addCell(cell);

			if (characterPlayer != null) {
				p = new Paragraph(characterPlayer.getCharacteristicPotentialValue(characteristic.getAbbreviation()) + "", new Font(getHandWrittingFont(),
						fontSize));
			} else {
				p = new Paragraph("  " + EMPTY_VALUE, new Font(getDefaultFont(), fontSize));
			}
			cell = new PdfPCell(p);
			cell.setBorderWidth(BORDER);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			tablaCaracteristicas.addCell(cell);

			if (characterPlayer != null) {
				p = new Paragraph(characterPlayer.getCharacteristicTemporalBonus(characteristic.getAbbreviation()) + "", new Font(getHandWrittingFont(),
						fontSize));
			} else {
				p = new Paragraph("  " + EMPTY_VALUE, new Font(getDefaultFont(), fontSize));
			}
			cell = new PdfPCell(p);
			cell.setBorderWidth(BORDER);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			tablaCaracteristicas.addCell(cell);

			if (characterPlayer != null) {
				p = new Paragraph(characterPlayer.getCharacteristicRaceBonus(characteristic.getAbbreviation()) + "", new Font(getHandWrittingFont(), fontSize));
			} else {
				p = new Paragraph("  " + EMPTY_VALUE, new Font(getDefaultFont(), fontSize));
			}
			cell = new PdfPCell(p);
			cell.setBorderWidth(BORDER);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			tablaCaracteristicas.addCell(cell);

			if (characterPlayer != null) {
				p = new Paragraph("    " + characterPlayer.getPerkCharacteristicBonus(characteristic.getAbbreviation()), new Font(getHandWrittingFont(),
						fontSize));
			} else {
				p = new Paragraph("    " + EMPTY_VALUE, new Font(getDefaultFont(), fontSize));
			}
			cell = new PdfPCell(p);
			cell.setBorderWidth(BORDER);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			tablaCaracteristicas.addCell(cell);

			if (characterPlayer != null) {
				p = new Paragraph(characterPlayer.getCharacteristicTotalBonus(characteristic.getAbbreviation()) + "", new Font(getHandWrittingFont(), fontSize));
			} else {
				p = new Paragraph("", new Font(getHandWrittingFont(), fontSize));
			}
			cell = new PdfPCell(p);
			cell.setBorderWidth(BORDER);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			tablaCaracteristicas.addCell(cell);

			// El separador de caracteristicas
			if (i == 4) {
				p = new Paragraph("", new Font(getHandWrittingFont(), fontSize));
				cell = new PdfPCell(p);
				cell.setBorderWidth(BORDER);
				cell.setMinimumHeight((float) 6);
				cell.setColspan(10);
				cell.setHorizontalAlignment(Element.ALIGN_CENTER);
				tablaCaracteristicas.addCell(cell);
			}
			i++;
		}
		return tablaCaracteristicas;
	}

	private PdfPTable createPointsTable(int fontSize) throws MalformedURLException, IOException, InvalidXmlElementException {
		float[] widths = { 0.32f, 0.32f, 0.35f };
		PdfPTable tabla = new PdfPTable(widths);
		Paragraph p;
		PdfPCell cell;

		p = new Paragraph(" ", new Font(getHandWrittingFont(), fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(30);
		cell.setColspan(3);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tabla.addCell(cell);

		if (characterPlayer != null) {
			p = new Paragraph(characterPlayer.getSkillTotalValue(LegacyValues.physicalDevelopmentSkill()) + "", new Font(getHandWrittingFont(),
					fontSize + 3));
		} else {
			p = new Paragraph("", new Font(getHandWrittingFont(), fontSize + 3));
		}
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(30);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tabla.addCell(cell);

		p = new Paragraph("", new Font(getHandWrittingFont(), fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tabla.addCell(cell);
		if (characterPlayer != null) {
			p = new Paragraph(Math.max(characterPlayer.getPowerPoints(), 0) + "", new Font(getHandWrittingFont(), fontSize + 3));
		} else {
			p = new Paragraph("", new Font(getHandWrittingFont(), fontSize + 3));
		}
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tabla.addCell(cell);

		p = new Paragraph(" ", new Font(getHandWrittingFont(), fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(10);
		cell.setColspan(3);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tabla.addCell(cell);

		if (characterPlayer != null) {
			int value = characterPlayer.getCharacteristicTotalBonus(CharacteristicAbbreviation.CONSTITUTION) / 2;
			if (value < 0) {
				value = 0;
			}
			p = new Paragraph(value + "", new Font(getHandWrittingFont(), fontSize));
		} else {
			p = new Paragraph(EMPTY_VALUE, new Font(getDefaultFont(), fontSize));
		}
		cell = new PdfPCell(p);
		cell.setMinimumHeight(25);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_LEFT);
		cell.setPaddingLeft(30f);
		tabla.addCell(cell);

		p = new Paragraph("", new Font(getHandWrittingFont(), fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		cell.setPaddingRight(30f);
		tabla.addCell(cell);

		if (characterPlayer != null) {
			p = new Paragraph(Math.max(characterPlayer.getBonusCharacteristicOfRealmOfMagic() / 2, 1) + "", new Font(getHandWrittingFont(), fontSize));
		} else {
			p = new Paragraph("__", new Font(getDefaultFont(), fontSize));
		}
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_LEFT);
		cell.setPaddingLeft(30f);
		tabla.addCell(cell);

		if (characterPlayer != null) {
			int puntos = Math.min(characterPlayer.getCharacteristicTotalBonus(CharacteristicAbbreviation.CONSTITUTION) * 2,
					characterPlayer.getSkillTotalValue(LegacyValues.physicalDevelopmentSkill()));
			if (puntos < 1) {
				puntos = 1;
			}
			p = new Paragraph(puntos + "", new Font(getHandWrittingFont(), fontSize));
		} else {
			p = new Paragraph("__", new Font(getDefaultFont(), fontSize));
		}
		cell = new PdfPCell(p);
		cell.setMinimumHeight(20);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_LEFT);
		cell.setPaddingLeft(30f);
		tabla.addCell(cell);

		p = new Paragraph("", new Font(getHandWrittingFont(), fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		cell.setPaddingRight(30f);
		tabla.addCell(cell);

		if (characterPlayer != null) {
			p = new Paragraph(Math.max(characterPlayer.getPowerPoints() / 2, 1) + "", new Font(getHandWrittingFont(), fontSize));
		} else {
			p = new Paragraph("__", new Font(getDefaultFont(), fontSize));
		}
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_LEFT);
		cell.setPaddingLeft(30f);
		tabla.addCell(cell);

		return tabla;
	}

	PdfPTable createFooter(int fontSize) {
		PdfPTable tabla = new PdfPTable(1);
		Paragraph p;
		PdfPCell cell;

		p = new Paragraph("Generado con El Libro de Esher NG, herramienta para Rolemaster V" + LegacySheetAssets.VERSION, new Font(getDefaultFont(), fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setPaddingLeft(30f);
		tabla.addCell(cell);

		return tabla;
	}

	private PdfPTable createMainPageRightFrame(int fontSize) throws MalformedURLException, IOException, InvalidXmlElementException {
		PdfPCell cell;
		Paragraph p;
		PdfPTable tablaDerecha = new PdfPTable(1);

		cell = new PdfPCell(createRuneTable(fontSize));
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(165);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tablaDerecha.addCell(cell);

		cell = new PdfPCell(createMostUsedSkillsTable(fontSize));
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(208);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tablaDerecha.addCell(cell);

		cell = new PdfPCell(createMostUsedAttacksTable(characterPlayer.getFavouriteOffensiveSkills(), fontSize));
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(100);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tablaDerecha.addCell(cell);

		p = new Paragraph("", new Font(getHandWrittingFont(), fontSize));
		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(100);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tablaDerecha.addCell(cell);

		cell = new PdfPCell(createPointsTable(fontSize + 2));
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(125);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tablaDerecha.addCell(cell);

		return tablaDerecha;
	}

	private void addMainTable(Document document, PdfWriter writer, int fontSize) throws MalformedURLException, IOException, InvalidXmlElementException {
		PdfPCell cell;
		float[] widths = { 0.295f, 0.605f };
		PdfPTable table = new PdfPTable(widths);
		table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
		table.setTotalWidth(document.getPageSize().getWidth() - 60);

		cell = new PdfPCell(createMainHeader(fontSize));
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(58);
		cell.setColspan(2);
		cell.setHorizontalAlignment(Element.ALIGN_LEFT);
		table.addCell(cell);

		cell = new PdfPCell(createMainPageLeftFrame(fontSize));
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(document.getPageSize().getHeight() - 145);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(cell);

		cell = new PdfPCell(createMainPageRightFrame(fontSize));
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(document.getPageSize().getHeight() - 145);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(cell);

		cell = new PdfPCell(createFooter(fontSize));
		cell.setBorderWidth(BORDER);
		cell.setColspan(2);
		cell.setMinimumHeight(30);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(cell);

		// PdfPCell celda = new PdfPCell();
		table.writeSelectedRows(0, -1, 30, document.getPageSize().getHeight() - 37, writer.getDirectContent());
		table.flushContent();
	}

	void characteristicsPage(Document document, PdfWriter writer) throws BadElementException,
			MalformedURLException, DocumentException, IOException, InvalidXmlElementException {
		int fontSize = 7;
		createBackgroundImage(document, LegacySheetAssets.getMainPage());
		addMainTable(document, writer, fontSize);
	}

	public String exportSpecials() throws InvalidXmlElementException {
		return LegacyTextBlocks.exportSpecials(characterPlayer);
	}

	public String exportPerks() throws InvalidXmlElementException {
		return LegacyTextBlocks.exportPerks(characterPlayer);
	}

	public String exportItems() throws InvalidXmlElementException {
		return LegacyTextBlocks.exportItems(characterPlayer);
	}

	public String exportHistory() throws InvalidXmlElementException {
		return LegacyTextBlocks.exportHistory(characterPlayer);
	}

	private void addSpecialText(Document document, PdfWriter writer, int fontSize) throws MalformedURLException, IOException, InvalidXmlElementException {
		PdfPCell cell;
		Paragraph p;

		float[] widths = { 1 };
		PdfPTable tablaPagina = new PdfPTable(widths);
		tablaPagina.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
		tablaPagina.setTotalWidth(document.getPageSize().getWidth() - 60);

		String texto = exportHistory() + "\n\n" + exportPerks() + "\n\n" + exportSpecials() + "\n" + exportItems();
		p = new Paragraph(texto, new Font(getHandWrittingFont(), fontSize - 1));

		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_LEFT);
		cell.setPaddingLeft(30f);
		cell.setMinimumHeight(document.getPageSize().getHeight() - 90);
		tablaPagina.addCell(cell);

		cell = new PdfPCell(createFooter(fontSize - 2));
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(30);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tablaPagina.addCell(cell);

		tablaPagina.writeSelectedRows(0, -1, 30, document.getPageSize().getHeight() - 37, writer.getDirectContent());
		tablaPagina.flushContent();
	}

	private void addEmptyText(Document document, PdfWriter writer, int fontSize) throws MalformedURLException, IOException, InvalidXmlElementException {
		PdfPCell cell;
		Paragraph p;

		float[] widths = { 1 };
		PdfPTable tablaPagina = new PdfPTable(widths);
		tablaPagina.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
		tablaPagina.setTotalWidth(document.getPageSize().getWidth() - 60);

		p = new Paragraph("", new Font(getHandWrittingFont(), fontSize));

		cell = new PdfPCell(p);
		cell.setBorderWidth(BORDER);
		cell.setHorizontalAlignment(Element.ALIGN_LEFT);
		cell.setPaddingLeft(30f);
		cell.setMinimumHeight(document.getPageSize().getHeight() - 90);
		tablaPagina.addCell(cell);

		cell = new PdfPCell(createFooter(fontSize - 2));
		cell.setBorderWidth(BORDER);
		cell.setMinimumHeight(30);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		tablaPagina.addCell(cell);

		tablaPagina.writeSelectedRows(0, -1, 30, document.getPageSize().getHeight() - 37, writer.getDirectContent());
		tablaPagina.flushContent();
	}

	void equipmentPage(Document document, PdfWriter writer) throws MalformedURLException, IOException, InvalidXmlElementException {
		int fontSize = 9;
		document.newPage();
		addSpecialText(document, writer, fontSize);
	}

	private void whitePage(Document document, PdfWriter writer) throws MalformedURLException, IOException, InvalidXmlElementException {
		int fontSize = 9;
		document.newPage();
		addEmptyText(document, writer, fontSize);
	}

	protected Image getNewRanksImage(int ranks) throws InvalidXmlElementException, BadElementException, MalformedURLException, IOException {
		Image image;
		switch (ranks) {
		case 1:
			image = LegacySheetAssets.getRanksBox(1);
			break;
		case 2:
			image = LegacySheetAssets.getRanksBox(2);
			break;
		case 3:
			image = LegacySheetAssets.getRanksBox(3);
			break;
		default:
			image = LegacySheetAssets.getRanksBox(0);
		}
		image.scalePercent(28);

		return image;
	}

	public CharacterPlayer getCharacterPlayer() {
		return characterPlayer;
	}
}
