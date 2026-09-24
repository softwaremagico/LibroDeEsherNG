package com.softwaremagico.librodeesher.pdf.details;

import com.softwaremagico.librodeesher.character.CharacterPlayer;
import com.softwaremagico.librodeesher.equipment.Equipment;
import com.softwaremagico.librodeesher.language.TranslatedText;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Verifies the detail appendix accepts every legacy text-backed detail source. */
@Test(groups = "pdf")
public class CharacterDetailsTableFactoryTest {

    @Test
    public void detailsTableIncludesPerksRaceSpecialsAndEquipment() throws Exception {
        final CharacterPlayer character = new CharacterPlayer();
        character.setRaceId("horseCentaur");
        character.setProfessionId("fighter");
        character.addPerk("acrobat");
        character.getCurrentLevel().addTraining("soldier");
        character.addStandardEquipment(new Equipment(new TranslatedText("Cuerda", "Rope"),
                new TranslatedText("Escalada", "Climbing")));

        final var table = CharacterDetailsTableFactory.getCharacterDetailsTable(character);

        Assert.assertTrue(table.size() > 1);
    }
}
