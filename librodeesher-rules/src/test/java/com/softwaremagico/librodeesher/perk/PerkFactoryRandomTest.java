package com.softwaremagico.librodeesher.perk;

import com.softwaremagico.librodeesher.exceptions.InvalidXmlElementException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/** Verifies the random perk/weakness pickers of {@link PerkFactory}. */
@Test(groups = "perkFactory")
public class PerkFactoryRandomTest {

    @Test
    public void randomPerkIsAlwaysAPositiveCostPerk() throws InvalidXmlElementException {
        final Random random = new Random(1);
        for (int i = 0; i < 100; i++) {
            final Perk perk = PerkFactory.getInstance().getRandomPerk(null, random);
            Assert.assertNotNull(perk);
            Assert.assertTrue(perk.getCost() > 0, "perk must cost points, was " + perk.getCost());
            Assert.assertFalse(perk.isWeakness());
        }
    }

    @Test
    public void randomPerkNeverReturnsNonOfficialPerks() throws InvalidXmlElementException {
        final Random random = new Random(2);
        for (int i = 0; i < 100; i++) {
            final Perk perk = PerkFactory.getInstance().getRandomPerk(null, random);
            Assert.assertNotNull(perk);
            Assert.assertNotEquals(perk.getType(), PerkType.OTHER);
        }
    }

    @Test
    public void randomPerkMatchesRequestedGradeWhenPresent() throws InvalidXmlElementException {
        final Random random = new Random(3);
        Perk minimumPerk = null;
        for (int i = 0; i < 30 && minimumPerk == null; i++) {
            minimumPerk = PerkFactory.getInstance().getRandomPerk(PerkGrade.MINIMUM, random);
        }
        Assert.assertNotNull(minimumPerk, "the real data must contain at least one MINIMUM perk");
        Assert.assertEquals(minimumPerk.getGrade(), PerkGrade.MINIMUM);
    }

    @Test
    public void randomWeaknessHasNegativeCost() throws InvalidXmlElementException {
        final Random random = new Random(4);
        for (int i = 0; i < 100; i++) {
            final Perk weakness = PerkFactory.getInstance().getRandomWeakness(null, null, random);
            Assert.assertNotNull(weakness);
            Assert.assertTrue(weakness.isWeakness());
            Assert.assertFalse(weakness.getCost() > 0);
            Assert.assertNotEquals(weakness.getType(), PerkType.OTHER);
        }
    }

    @Test
    public void randomWeaknessOfTypeFallsBackToAnyTypeWhenTypeHasNone() throws InvalidXmlElementException {
        // MAGICAL weaknesses may or may not exist depending on module set; whatever the result, it
        // must still be a weakness (never a perk, never null when any weakness is available) and the
        // type filter must be honoured when the requested type has candidates.
        final Random random = new Random(5);
        final Perk candidate = PerkFactory.getInstance().getRandomWeakness(null, PerkType.TRAINING, random);
        if (hasWeaknessOfType(PerkType.TRAINING)) {
            Assert.assertNotNull(candidate);
            Assert.assertEquals(candidate.getType(), PerkType.TRAINING);
        } else {
            Assert.assertNotNull(candidate);
            Assert.assertTrue(candidate.isWeakness());
        }
    }

    @Test
    public void randomPickersAreDeterministicGivenTheSameSeed() throws InvalidXmlElementException {
        final Perk factoryPerk = PerkFactory.getInstance().getRandomPerk(null, new Random(42));
        final Perk factoryPerkAgain = PerkFactory.getInstance().getRandomPerk(null, new Random(42));
        Assert.assertEquals(factoryPerk, factoryPerkAgain);

        final Perk firstWeakness = PerkFactory.getInstance().getRandomWeakness(null, null, new Random(7));
        final Perk secondWeakness = PerkFactory.getInstance().getRandomWeakness(null, null, new Random(7));
        Assert.assertEquals(firstWeakness, secondWeakness);
    }

    @Test
    public void randomPickersCoverSeveralDistinctPerks() throws InvalidXmlElementException {
        final Set<String> perks = new HashSet<>();
        final Set<String> weaknesses = new HashSet<>();
        final Random random = new Random(8);
        for (int i = 0; i < 50; i++) {
            perks.add(PerkFactory.getInstance().getRandomPerk(null, random).getId());
            weaknesses.add(PerkFactory.getInstance().getRandomWeakness(null, null, random).getId());
        }
        Assert.assertTrue(perks.size() > 1);
        Assert.assertTrue(weaknesses.size() > 1);
    }

    private boolean hasWeaknessOfType(PerkType type) throws InvalidXmlElementException {
        for (final Perk perk : PerkFactory.getInstance().getElements()) {
            if (perk.isWeakness() && perk.getType() == type && perk.getType() != PerkType.OTHER) {
                return true;
            }
        }
        return false;
    }
}