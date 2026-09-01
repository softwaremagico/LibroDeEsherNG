package com.softwaremagico.librodeesher.profession;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.softwaremagico.librodeesher.magic.MagicLevelRange;
import com.softwaremagico.librodeesher.magic.MagicListType;

import java.util.Collections;
import java.util.List;

/**
 * One row of a profession's "DESARROLLO DE HECHIZOS" section: how many background points it costs to
 * buy each rank of a spell list classified as {@link #getListType()}, while it has between {@link
 * MagicLevelRange#forRanks}'s bracket bounds ranks (1 to 3 numbers, e.g. {@code "3/3/3"} or {@code
 * "100"|}; a rank beyond the list costs {@code null}, matching {@link ProfessionCategoryCost}'s own
 * "HABILIDADES Y CATEGORÍAS" rank-cost format exactly).
 */
public class ProfessionMagicCost {

    @JsonProperty("listType")
    private MagicListType listType;

    @JsonProperty("levelRange")
    private MagicLevelRange levelRange;

    @JacksonXmlElementWrapper(localName = "rankCosts")
    @JacksonXmlProperty(localName = "rankCost")
    private List<Integer> rankCosts;

    public ProfessionMagicCost() {
        // Required by Jackson.
    }

    public ProfessionMagicCost(MagicListType listType, MagicLevelRange levelRange, List<Integer> rankCosts) {
        this.listType = listType;
        this.levelRange = levelRange;
        this.rankCosts = rankCosts;
    }

    public MagicListType getListType() {
        return listType;
    }

    public void setListType(MagicListType listType) {
        this.listType = listType;
    }

    public MagicLevelRange getLevelRange() {
        return levelRange;
    }

    public void setLevelRange(MagicLevelRange levelRange) {
        this.levelRange = levelRange;
    }

    public List<Integer> getRankCosts() {
        return rankCosts == null ? Collections.emptyList() : rankCosts;
    }

    public void setRankCosts(List<Integer> rankCosts) {
        this.rankCosts = rankCosts;
    }

    /**
     * The background point cost of the {@code rankIndex}-th rank bought within this bracket (0-based,
     * so {@code rankIndex=0} is the bracket's first rank), or {@code null} if {@code rankIndex} is
     * beyond {@link #getRankCosts()}'s size, matching {@link ProfessionCategoryCost#getRankCost(int)}
     * exactly.
     */
    public Integer getRankCost(int rankIndex) {
        final List<Integer> costs = getRankCosts();
        if (rankIndex < 0 || rankIndex >= costs.size()) {
            return null;
        }
        return costs.get(rankIndex);
    }
}
