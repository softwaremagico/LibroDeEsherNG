package com.softwaremagico.librodeesher.profession;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.Collections;
import java.util.List;

/**
 * One line of a profession's "HABILIDADES Y CATEGORÍAS DE HABILIDADES" section: how many background
 * points it costs to buy each rank of {@link #getCategoryId()} (1 to 3 numbers, e.g. {@code "2/2/2"}
 * or {@code "3/6"}; a rank beyond the list costs as much as the last one, per the legacy {@code
 * CategoryCost}).
 */
public class ProfessionCategoryCost {

    @JsonProperty("categoryId")
    private String categoryId;

    @JacksonXmlElementWrapper(localName = "rankCosts")
    @JacksonXmlProperty(localName = "rankCost")
    private List<Integer> rankCosts;

    public ProfessionCategoryCost() {
        // Required by Jackson.
    }

    public ProfessionCategoryCost(String categoryId, List<Integer> rankCosts) {
        this.categoryId = categoryId;
        this.rankCosts = rankCosts;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public List<Integer> getRankCosts() {
        return rankCosts == null ? Collections.emptyList() : rankCosts;
    }

    public void setRankCosts(List<Integer> rankCosts) {
        this.rankCosts = rankCosts;
    }

    /**
     * The background point cost of the {@code rankIndex}-th rank bought this level (0-based, so
     * {@code rankIndex=0} is the first rank bought at that level), or {@code null} if {@code
     * rankIndex} is beyond {@link #getRankCosts()}'s size (this profession does not allow buying that
     * many ranks of this category in a single level), matching the legacy {@code
     * CategoryCost#getRankCost} exactly.
     */
    public Integer getRankCost(int rankIndex) {
        final List<Integer> costs = getRankCosts();
        if (rankIndex < 0 || rankIndex >= costs.size()) {
            return null;
        }
        return costs.get(rankIndex);
    }
}
