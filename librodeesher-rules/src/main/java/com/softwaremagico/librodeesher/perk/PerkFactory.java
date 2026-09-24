package com.softwaremagico.librodeesher.perk;

import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import com.softwaremagico.librodeesher.xml.XmlFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Reads {@code talentos.xml} from every enabled rulebook module.
 *
 * <p>Also offers the random pickers the legacy {@code PerkFactory} exposed for the "suggest a random
 * perk/weakness" feature: perks cost background points ({@link Perk#getCost()} &gt; 0), weaknesses
 * grant them back ({@code getCost()} &lt; 0), and non-official perks ({@link PerkType#OTHER}) are
 * never picked at random, matching the legacy classification. Unlike the legacy recursion-based
 * implementation (which could overflow the stack when no perk of a requested grade existed), the
 * candidates are filtered first and {@code null} is returned when none remain.</p>
 */
public class PerkFactory extends XmlFactory<Perk> {

    private static final String XML_FILE = "perks.xml";

    private static final Random RANDOM = new Random();

    private static final class PerkFactoryHolder {
        private static final PerkFactory INSTANCE = new PerkFactory();
    }

    public static PerkFactory getInstance() {
        return PerkFactoryHolder.INSTANCE;
    }

    @Override
    public String getXmlFile() {
        return XML_FILE;
    }

    @Override
    public List<Perk> getElements() throws InvalidXmlElementException {
        return readXml(Perk.class);
    }

    /**
     * A random perk (positive background-point cost, non-official ones excluded). A {@code null}
     * {@code grade} selects among every eligible perk; {@code null} if no perk of that grade exists.
     */
    public Perk getRandomPerk(PerkGrade grade) throws InvalidXmlElementException {
        return getRandomPerk(grade, RANDOM);
    }

    /** Same as {@link #getRandomPerk(PerkGrade)}, using the provided generator (seedable for tests). */
    Perk getRandomPerk(PerkGrade grade, Random random) throws InvalidXmlElementException {
        final List<Perk> candidates = getRandomizablePerks(grade);
        return candidates.isEmpty() ? null : candidates.get(random.nextInt(candidates.size()));
    }

    /**
     * A random weakness (negative background-point cost, non-official ones excluded). A {@code null}
     * {@code grade} selects among every eligible weakness; {@code null} if none remains.
     */
    public Perk getRandomWeakness(PerkGrade grade) throws InvalidXmlElementException {
        return getRandomWeakness(grade, null, RANDOM);
    }

    /**
     * A random weakness of the given {@code type}, mirroring the legacy behaviour: when the requested
     * type defines no weakness, or none of the requested grade, any other type is used instead.
     */
    public Perk getRandomWeakness(PerkGrade grade, PerkType type) throws InvalidXmlElementException {
        return getRandomWeakness(grade, type, RANDOM);
    }

    /** Same as {@link #getRandomWeakness(PerkGrade, PerkType)}, using the provided generator (seedable for tests). */
    Perk getRandomWeakness(PerkGrade grade, PerkType type, Random random) throws InvalidXmlElementException {
        List<Perk> candidates = getWeaknesses(grade, type);
        if (candidates.isEmpty() && type != null) {
            candidates = getWeaknesses(grade, null);
        }
        return candidates.isEmpty() ? null : candidates.get(random.nextInt(candidates.size()));
    }

    private List<Perk> getRandomizablePerks(PerkGrade grade) throws InvalidXmlElementException {
        final List<Perk> result = new ArrayList<>();
        for (final Perk perk : getElements()) {
            if (isRandomizable(perk)
                    && (grade == null || perk.getGrade() == grade)) {
                result.add(perk);
            }
        }
        return result;
    }

    private boolean isRandomizable(Perk perk) {
        return perk != null && perk.getCost() != null && perk.getCost() > 0
                && perk.getType() != PerkType.OTHER;
    }

    private List<Perk> getWeaknesses(PerkGrade grade, PerkType type) throws InvalidXmlElementException {
        final List<Perk> result = new ArrayList<>();
        for (final Perk perk : getElements()) {
            if (perk == null || perk.getCost() == null || perk.getCost() >= 0
                    || perk.getType() == PerkType.OTHER) {
                continue;
            }
            if (type != null && perk.getType() != type) {
                continue;
            }
            if (grade != null && perk.getGrade() != grade) {
                continue;
            }
            result.add(perk);
        }
        return result;
    }
}