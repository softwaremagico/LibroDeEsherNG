package com.softwaremagico.librodeesher.training;

import com.softwaremagico.librodeesher.decision.Decision;
import com.softwaremagico.librodeesher.decision.InvalidDecisionException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/** Verifies {@link ChoiceGroup#resolve}, {@link TrainingCategoryGrant#resolve} and {@link TrainingSkillGrant#resolve}. */
@Test(groups = "training")
public class GrantDecisionTest {

    @Test
    public void fixedChoiceGroupIgnoresTheSelection() {
        final ChoiceGroup group = new ChoiceGroup(List.of("hunt"));
        Assert.assertEquals(group.resolve(null).getSelectedOption(), "hunt");
        Assert.assertEquals(group.resolve("whateverIsIgnored").getSelectedOption(), "hunt");
    }

    @Test
    public void choiceGroupRequiresAValidSelection() {
        final ChoiceGroup group = new ChoiceGroup(List.of("stalking", "hunting"));
        Assert.assertEquals(group.resolve("hunting").getSelectedOption(), "hunting");
        Assert.assertThrows(InvalidDecisionException.class, () -> group.resolve("swimming"));
    }

    @Test
    public void trainingCategoryGrantResolvesAChoiceOfCategories() {
        final TrainingCategoryGrant grant = new TrainingCategoryGrant();
        grant.setCategoryOptions(List.of("weaponsTwoHanded", "weaponsEdged"));

        final Decision decision = grant.resolve("weaponsEdged");
        Assert.assertEquals(decision.getSelectedOption(), "weaponsEdged");
        Assert.assertTrue(decision.isChoice());
    }

    @Test
    public void trainingCategoryGrantAutoResolvesAFixedCategory() {
        final TrainingCategoryGrant grant = new TrainingCategoryGrant();
        grant.setCategoryOptions(List.of("outdoorEnvironment"));

        Assert.assertEquals(grant.resolve(null).getSelectedOption(), "outdoorEnvironment");
    }

    @Test
    public void trainingSkillGrantResolvesAChoiceOfSkills() {
        final TrainingSkillGrant grant = new TrainingSkillGrant(List.of("stalking", "hunting"), 3);
        Assert.assertEquals(grant.resolve("stalking").getSelectedOption(), "stalking");
        Assert.assertThrows(InvalidDecisionException.class, () -> grant.resolve("swimming"));
    }
}
