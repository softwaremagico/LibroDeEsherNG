package com.softwaremagico.librodeesher.race;

import com.softwaremagico.librodeesher.characteristic.CharacteristicAbbreviation;
import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.language.LanguageSlot;
import com.softwaremagico.librodeesher.magic.RealmOfMagic;
import com.softwaremagico.librodeesher.perk.Perk;
import com.softwaremagico.librodeesher.resistance.ResistanceType;
import com.softwaremagico.librodeesher.rules.RulesCatalog;

/**
 * An alternate, house-rule perk point budget derived from how "powerful" a race's own stat block is
 * (e.g. a race with strong characteristic bonuses and few restrictions gets fewer perk points to
 * spend than a plain one), matching the legacy {@code PerkPointsCalculator} exactly: every one of its
 * ~15 lookup tables is ported verbatim, including their own quirks (e.g. {@link #getRaceTypeRacePoints}'s
 * dead {@code -0} case).
 *
 * <p>This is a completely separate mechanic from the normal, profession-driven background point
 * budget ({@code CharacterPlayer#getTotalBackgroundPoints}): it only existed as an optional,
 * off-by-default toggle in the legacy application, but is still exercised by real tests against real
 * race data (see the legacy {@code PerksTests}), so it is ported here for parity.</p>
 *
 * <p><strong>Known pre-existing data quirk (not a migration bug):</strong> "grayOrc"'s own source
 * file lists 6 numbers for its Psionic power point progression ({@code "0/6/5/4/3/2"}) instead of
 * the usual 5 every other race and every lookup table entry here (matching the legacy {@code
 * PerkPointsCalculator}'s own table) expects; {@link #getPerkPoints()} throws {@link
 * IllegalStateException} for it, the same as the legacy calculator would.</p>
 */
public final class RacePerkPointsCalculator {

    private final Race race;

    public RacePerkPointsCalculator(Race race) {
        this.race = race;
    }

    /** The final perk point budget (0 to 65, in steps of 5), derived from {@link #getRacePoints()}. */
    public int getPerkPoints() throws InvalidXmlElementException {
        final int racePoints = getRacePoints();
        if (racePoints < -75) {
            return 65;
        } else if (racePoints <= 25) {
            return 60;
        } else if (racePoints <= 75) {
            return 55;
        } else if (racePoints <= 100) {
            return 50;
        } else if (racePoints <= 125) {
            return 45;
        } else if (racePoints <= 150) {
            return 40;
        } else if (racePoints <= 175) {
            return 35;
        } else if (racePoints <= 200) {
            return 30;
        } else if (racePoints <= 225) {
            return 25;
        } else if (racePoints <= 250) {
            return 20;
        } else if (racePoints <= 275) {
            return 15;
        } else if (racePoints <= 300) {
            return 10;
        } else if (racePoints <= 325) {
            return 5;
        } else {
            return 0;
        }
    }

    /** The race's own "power level" score, the sum of every sub-score below. */
    private int getRacePoints() throws InvalidXmlElementException {
        int racePoints = 0;
        racePoints += getPhysicalDevelopmentRacePoints();
        for (final RealmOfMagic realm : RealmOfMagic.values()) {
            racePoints += getPowerPointsRacePoints(realm);
        }
        racePoints += getLifeExpectationRacePoints();
        for (final ResistanceType resistance : ResistanceType.values()) {
            racePoints += getResistanceRacePoints(resistance);
        }
        racePoints += getSoulDepartTimeRacePoints();
        racePoints += getStartingLanguagesRacePoints();
        for (final CharacteristicAbbreviation characteristic : CHARACTERISTICS) {
            racePoints += getCharacteristicBonusRacePoints(characteristic);
        }
        racePoints += getSizeRacePoints();
        racePoints += getRecoveryRacePoints();
        racePoints += getRaceTypeRacePoints();
        racePoints += getCommonSkillsRacePoints();
        racePoints += getRestrictedSkillsRacePoints();
        racePoints += getSpecialRacePoints();
        return racePoints;
    }

    /** Every characteristic {@link #getCharacteristicBonusRacePoints} is applied to (every real one, excluding appearance). */
    private static final CharacteristicAbbreviation[] CHARACTERISTICS = {
            CharacteristicAbbreviation.AGILITY, CharacteristicAbbreviation.CONSTITUTION,
            CharacteristicAbbreviation.MEMORY, CharacteristicAbbreviation.REASONING,
            CharacteristicAbbreviation.SELF_DISCIPLINE, CharacteristicAbbreviation.EMPATHY,
            CharacteristicAbbreviation.INTUITION, CharacteristicAbbreviation.PRESENCE,
            CharacteristicAbbreviation.QUICKNESS, CharacteristicAbbreviation.STRENGTH,
    };

    /**
     * Every {@link RaceSpecial}'s own point value (see its javadoc), plus the background point cost
     * of every real {@link Perk} one of them grants for free (matching the legacy {@code
     * Race#getRacePerks()} - the one {@code PerkPointsCalculator} actually calls, as opposed to the
     * separate, stricter matching {@code Race#setOtherSpecials} does into its own unused {@code
     * racePerks} field - exactly: a special's own points, from its trailing {@code "[N]"}, are always
     * added regardless of whether it also names a real perk before its first colon; two specials
     * naming the same perk (e.g. two different "Arma Natural: ..." lines) each add its cost again).
     */
    private int getSpecialRacePoints() throws InvalidXmlElementException {
        int specialPoints = 0;
        for (final RaceSpecial special : race.getSpecials()) {
            if (special.getPoints() != null) {
                specialPoints += special.getPoints();
            }
            final Perk perk = findGrantedPerk(special);
            if (perk != null) {
                specialPoints += perk.getCost();
            }
        }
        return specialPoints;
    }

    /**
     * The real {@link Perk} a race special line grants for free, if the text before its first colon
     * is a real perk's Spanish name (matching the legacy {@code Race#getRacePerks()}'s {@code
     * special.split(":")[0]} exactly); {@code null} if there is no colon, or nothing before it
     * matches a real perk.
     */
    private Perk findGrantedPerk(RaceSpecial special) throws InvalidXmlElementException {
        final String text = special.getText().getSpanish();
        final int colonIndex = text.indexOf(':');
        if (colonIndex < 0) {
            return null;
        }
        final String perkName = text.substring(0, colonIndex).trim();
        for (final Perk perk : RulesCatalog.getInstance().getPerks()) {
            if (perk.getName().getSpanish().equalsIgnoreCase(perkName)) {
                return perk;
            }
        }
        return null;
    }

    private int getRestrictedSkillsRacePoints() throws InvalidXmlElementException {
        final int restrictedSkills = getTotalSkills(race.getRestrictedSkillIds(), race.getRestrictedCategoryIds());
        if (restrictedSkills == 0) {
            return 0;
        } else if (restrictedSkills <= 5) {
            return restrictedSkills * -2;
        } else if (restrictedSkills <= 10) {
            return ((restrictedSkills - 5) * -1) - 10;
        } else {
            return -15;
        }
    }

    private int getCommonSkillsRacePoints() throws InvalidXmlElementException {
        final int commonSkills = getTotalSkills(race.getCommonSkillIds(), race.getCommonCategoryIds());
        if (commonSkills == 0) {
            return 0;
        }
        return commonSkills * (((commonSkills - 1) / 10) + 1);
    }

    /** {@code skillIds.size()} plus every {@code categoryIds} category's own skill count, matching the legacy {@code getTotalCommonSkills}/{@code getTotalRestrictedSkills}. */
    private static int getTotalSkills(java.util.List<String> skillIds, java.util.List<String> categoryIds) throws InvalidXmlElementException {
        int total = skillIds.size();
        for (final String categoryId : categoryIds) {
            final com.softwaremagico.librodeesher.category.Category category = RulesCatalog.getInstance().getCategory(categoryId);
            if (category.hasDynamicSkills()) {
                // Weapon categories list their skills in the weapon files, not the category itself
                // (see Category#hasDynamicSkills's javadoc); count real weapons of that category
                // instead, matching the legacy Category (which stored weapons as plain skills).
                for (final com.softwaremagico.librodeesher.weapon.Weapon weapon : RulesCatalog.getInstance().getWeapons()) {
                    if (categoryId.equals(weapon.getCategoryId())) {
                        total++;
                    }
                }
            } else {
                total += category.getSkills().size();
            }
        }
        return total;
    }

    private int getRaceTypeRacePoints() {
        switch (race.getRaceType()) {
            case 1:
                return 10;
            case 2:
                return -5;
            case 3:
                return -0;
            case 4:
                return -5;
            case 5:
                return -10;
            default:
                throw new IllegalStateException(
                        "Unknown race type '" + race.getRaceType() + "' for race '" + race.getId() + "'.");
        }
    }

    private int getSizeRacePoints() {
        switch (race.getSize()) {
            case XXS:
                return 15;
            case XS:
                return 10;
            case S:
                return 5;
            case M:
                return 0;
            case L:
                return 10;
            case XL:
                return 15;
            case XXL:
                return 25;
            default:
                return 0;
        }
    }

    private int getRecoveryRacePoints() {
        final double restorationTime = race.getRestorationTime();
        if (restorationTime >= 3) {
            return -45;
        } else if (restorationTime >= 2) {
            return -25;
        } else if (restorationTime >= 1.5) {
            return -10;
        } else if (restorationTime >= 1.2) {
            return -8;
        } else if (restorationTime >= 1) {
            return 0;
        } else if (restorationTime >= 0.9) {
            return 3;
        } else if (restorationTime >= 0.75) {
            return 5;
        } else if (restorationTime >= 0.6) {
            return 7;
        } else if (restorationTime >= 0.5) {
            return 10;
        } else if (restorationTime >= 0.2) {
            return 25;
        } else if (restorationTime >= 0.1) {
            return 45;
        } else {
            throw new IllegalStateException(
                    "Unknown recovery bonus '" + restorationTime + "' for race '" + race.getId() + "'.");
        }
    }

    private int getCharacteristicBonusRacePoints(CharacteristicAbbreviation characteristic) {
        final int bonus = race.getCharacteristicBonuses().getOrDefault(characteristic.name(), 0);
        switch (bonus) {
            case -15:
                return -80;
            case -14:
                return -70;
            case -13:
                return -65;
            case -12:
                return -55;
            case -11:
                return -50;
            case -10:
                return -45;
            case -9:
                return -35;
            case -8:
                return -30;
            case -7:
                return -25;
            case -6:
                return -20;
            case -5:
                return -13;
            case -4:
                return -10;
            case -3:
                return -7;
            case -2:
                return -5;
            case -1:
                return -3;
            case 0:
                return 0;
            case 1:
                return 3;
            case 2:
                return 5;
            case 3:
                return 7;
            case 4:
                return 10;
            case 5:
                return 25;
            case 6:
                return 45;
            case 7:
                return 55;
            case 8:
                return 65;
            case 9:
                return 73;
            case 10:
                return 80;
            case 11:
                return 100;
            case 12:
                return 120;
            case 13:
                return 125;
            case 14:
                return 150;
            case 15:
                return 170;
            default:
                throw new IllegalStateException("Unknown characteristic bonus '" + bonus + "' for characteristic '"
                        + characteristic + "' for race '" + race.getId() + "'.");
        }
    }

    /**
     * The first entry of {@link Race#getRaceLanguages()} is the race's own native language, matching
     * the legacy {@code Race#setLanguages}' own "first language is main language" comment exactly.
     */
    private int getStartingLanguagesRacePoints() {
        int languageCost = 0;
        int languages = 0;
        final String mainLanguageId = race.getRaceLanguages().isEmpty() ? null
                : race.getRaceLanguages().get(0).getLanguageId();

        for (final RaceLanguage language : race.getRaceLanguages()) {
            languages++;
            final boolean isMainLanguage = language.getLanguageId().equals(mainLanguageId);
            languageCost += language.getStartingSpeakingRanks() - (isMainLanguage ? 8 : 5);
            languageCost += language.getStartingWritingRanks() - (isMainLanguage ? 4 : 0);
        }

        for (final LanguageSlot language : race.getOptionalRaceLanguages()) {
            languageCost += language.getStartingSpeakingRanks() - 5;
            languageCost += language.getStartingWritingRanks();
        }

        switch (languages + race.getOptionalRaceLanguages().size()) {
            case 1:
                languageCost += -5;
                break;
            case 2:
                languageCost += 0;
                break;
            case 3:
                languageCost += -5;
                break;
            case 4:
                languageCost += 7;
                break;
            case 5:
                languageCost += 10;
                break;
            default:
                languageCost += (languages - 5) * 5 + 10;
        }

        languageCost += race.getLanguagePoints();
        return languageCost;
    }

    private int getSoulDepartTimeRacePoints() {
        final int soulDepartTime = race.getSoulDepartTime();
        switch (soulDepartTime) {
            case 1:
                return -25;
            case 2:
                return -20;
            case 3:
                return -15;
            case 4:
                return -11;
            case 5:
                return -9;
            case 6:
                return -7;
            case 7:
                return -5;
            case 8:
                return -4;
            case 9:
                return -3;
            case 10:
                return -2;
            case 11:
                return -1;
            case 12:
                return 0;
            case 13:
                return 2;
            case 14:
                return 4;
            case 15:
                return 6;
            case 16:
                return 8;
            case 17:
                return 10;
            case 18:
                return 15;
            default:
                return ((soulDepartTime - 18) * 5) + 15;
        }
    }

    private int getResistanceRacePoints(ResistanceType resistance) {
        final int bonus = race.getResistanceBonuses().getOrDefault(resistance.name(), 0);
        if (bonus < -10) {
            return ((bonus - 10) * 2) - 10;
        }
        switch (bonus) {
            case -10:
                return -10;
            case -5:
                return -5;
            case 0:
                return 0;
            case 5:
                return 3;
            case 10:
                return 5;
            case 15:
                return 7;
            case 20:
                return 10;
            case 25:
                return 11;
            case 30:
                return 13;
            case 35:
                return 14;
            case 40:
                return 16;
            case 50:
                return 20;
            case 60:
                return 25;
            case 70:
                return 35;
            case 80:
                return 50;
            case 90:
                return 70;
            case 100:
                return 95;
            case 150:
                return 150;
            default:
                throw new IllegalStateException(
                        "Unknown resistance value '" + bonus + "' for race '" + race.getId() + "'.");
        }
    }

    private int getLifeExpectationRacePoints() {
        final int lifeYears = race.getExpectedLifeYears();
        if (lifeYears <= 80) {
            return -5;
        } else if (lifeYears <= 100) {
            return 0;
        } else if (lifeYears <= 300) {
            return 3;
        } else if (lifeYears <= 500) {
            return 5;
        } else if (lifeYears <= 800) {
            return 7;
        } else if (lifeYears <= 1200) {
            return 9;
        } else if (lifeYears <= 1700) {
            return 11;
        } else if (lifeYears <= 2300) {
            return 13;
        } else {
            return 15;
        }
    }

    /** The realm-specific key {@link Race#getProgressionRankValues()} uses for {@code realm}'s power point progression, or {@code null} for realms with none (e.g. {@link RealmOfMagic#RACE}). */
    private static String progressionKeyFor(RealmOfMagic realm) {
        return switch (realm) {
            case ARCHANUM -> "ppArcane";
            case CANALIZATION -> "ppChanneling";
            case ESSENCE -> "ppEssence";
            case MENTALISM -> "ppMentalism";
            case PSIONIC -> "ppPsionic";
            case RACE -> null;
        };
    }

    private int getPowerPointsRacePoints(RealmOfMagic realm) {
        final String key = progressionKeyFor(realm);
        final String cost = key == null ? null : race.getProgressionRankValues().get(key);
        if (cost == null) {
            return 0;
        }
        return switch (cost) {
            case "0/2/1/1/1" -> -25;
            case "0/3/2/1/1" -> -20;
            case "0/4/3/2/1" -> -10;
            case "0/5/3/2/1" -> -1;
            case "0/5/2/2/2" -> -1;
            case "0/5/3/2/2" -> 0;
            case "0/5/4/3/2" -> 3;
            case "0/6/4/3/1" -> 4;
            case "0/6/4/3/2" -> 5;
            case "0/6/5/4/3" -> 10;
            case "0/6/6/4/3" -> 13;
            case "0/7/5/4/3" -> 14;
            case "0/7/6/5/4" -> 15;
            default -> throw new IllegalStateException("Unknown cost '" + cost
                    + "' for power point progression of realm '" + realm + "' for race '" + race.getId() + "'.");
        };
    }

    private int getPhysicalDevelopmentRacePoints() {
        final String cost = race.getProgressionRankValues().get("physicalDevelopment");
        return switch (cost) {
            case "0/2/1/1/1" -> -50;
            case "0/5/2/2/1" -> -30;
            case "0/5/3/2/1" -> -20;
            case "0/6/2/2/1" -> -17;
            case "0/6/3/1/1" -> -15;
            case "0/5/4/2/1" -> -11;
            case "0/5/4/3/2" -> -10;
            case "0/6/3/2/1" -> -10;
            case "0/6/3/2/2" -> -9;
            case "0/6/4/2/1" -> 0;
            case "0/7/3/2/1" -> 5;
            case "0/6/5/2/1" -> 10;
            case "0/7/4/2/1" -> 13;
            case "0/6/5/4/3" -> 14;
            case "0/7/5/3/1" -> 15;
            case "0/7/6/5/2" -> 19;
            case "0/8/6/4/2" -> 20;
            case "0/9/6/5/3" -> 24;
            case "0/9/6/5/4" -> 25;
            case "0/11/9/7/5" -> 50;
            case "0/15/13/11/9" -> 100;
            default -> throw new IllegalStateException(
                    "Unknown physical development cost '" + cost + "' value for race '" + race.getId() + "'.");
        };
    }
}
