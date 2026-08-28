package com.softwaremagico.librodeesher.decision;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/** Verifies {@link Decision}'s validation rules. */
@Test(groups = "decision")
public class DecisionTest {

    @Test
    public void selectValidatesTheChosenOptionIsOffered() {
        final Decision decision = Decision.select(List.of("sword", "axe"), "axe");
        Assert.assertEquals(decision.getSelectedOption(), "axe");
        Assert.assertEquals(decision.getOfferedOptions(), List.of("sword", "axe"));
        Assert.assertTrue(decision.isChoice());
    }

    @Test(expectedExceptions = InvalidDecisionException.class)
    public void selectRejectsAnOptionThatWasNotOffered() {
        Decision.select(List.of("sword", "axe"), "bow");
    }

    @Test(expectedExceptions = InvalidDecisionException.class)
    public void selectRejectsANullSelection() {
        Decision.select(List.of("sword", "axe"), null);
    }

    @Test(expectedExceptions = InvalidDecisionException.class)
    public void selectRejectsEmptyOfferedOptions() {
        Decision.select(List.of(), "sword");
    }

    @Test
    public void fixedAutoSelectsTheSingleOption() {
        final Decision decision = Decision.fixed(List.of("sword"));
        Assert.assertEquals(decision.getSelectedOption(), "sword");
        Assert.assertFalse(decision.isChoice());
    }

    @Test(expectedExceptions = InvalidDecisionException.class)
    public void fixedRejectsMoreThanOneOption() {
        Decision.fixed(List.of("sword", "axe"));
    }

    @Test(expectedExceptions = InvalidDecisionException.class)
    public void fixedRejectsNoOptions() {
        Decision.fixed(List.of());
    }
}
