package com.softwaremagico.librodeesher.decision;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/** Verifies {@link Decisions}' bookkeeping. */
@Test(groups = "decision")
public class DecisionsTest {

    @Test
    public void undecidedKeyResolvesToNull() {
        final Decisions decisions = new Decisions();
        final DecisionKey key = DecisionKey.characterWide(DecisionKind.PROFESSION_REALM, "soldier", 0);
        Assert.assertFalse(decisions.isDecided(key));
        Assert.assertNull(decisions.get(key));
        Assert.assertNull(decisions.getSelectedOption(key));
    }

    @Test
    public void setRecordsAndOverwritesADecision() {
        final Decisions decisions = new Decisions();
        final DecisionKey key = DecisionKey.characterWide(DecisionKind.PROFESSION_REALM, "soldier", 0);
        decisions.set(key, Decision.select(List.of("sword", "axe"), "axe"));

        Assert.assertTrue(decisions.isDecided(key));
        Assert.assertEquals(decisions.getSelectedOption(key), "axe");

        decisions.set(key, Decision.select(List.of("sword", "axe"), "sword"));
        Assert.assertEquals(decisions.getSelectedOption(key), "sword");
    }

    @Test
    public void removeForgetsADecision() {
        final Decisions decisions = new Decisions();
        final DecisionKey key = DecisionKey.characterWide(DecisionKind.PROFESSION_REALM, "soldier", 0);
        decisions.set(key, Decision.fixed(List.of("sword")));
        decisions.remove(key);
        Assert.assertFalse(decisions.isDecided(key));
    }
}