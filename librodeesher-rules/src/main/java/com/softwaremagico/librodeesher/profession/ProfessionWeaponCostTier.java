package com.softwaremagico.librodeesher.profession;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.Collections;
import java.util.List;

/**
 * One "Armas·CategoríaN" cost tier from a profession's "HABILIDADES Y CATEGORÍAS DE HABILIDADES"
 * section (see {@link Profession#getWeaponCategoryCostTiers()}); a thin wrapper around a rank-cost
 * list (1 to 3 numbers, same format as {@link ProfessionCategoryCost}) so it can be XML-serialized as
 * a list of lists.
 */
public class ProfessionWeaponCostTier {

    @JacksonXmlElementWrapper(localName = "rankCosts")
    @JacksonXmlProperty(localName = "rankCost")
    private List<Integer> rankCosts;

    public ProfessionWeaponCostTier() {
        // Required by Jackson.
    }

    public ProfessionWeaponCostTier(List<Integer> rankCosts) {
        this.rankCosts = rankCosts;
    }

    public List<Integer> getRankCosts() {
        return rankCosts == null ? Collections.emptyList() : rankCosts;
    }

    public void setRankCosts(List<Integer> rankCosts) {
        this.rankCosts = rankCosts;
    }
}
