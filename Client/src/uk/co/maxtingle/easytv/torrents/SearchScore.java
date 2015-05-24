package uk.co.maxtingle.easytv.torrents;

import java.util.Map;

public class SearchScore
{
    private int    relevanceScore;
    private int    seedersScore;
    private int    ratioScore;
    private int overallScore;
    private double relevance;
    private double ratio;

    public SearchScore(int relevanceScore, int seedersScore, int ratioScore, int overallScore, double relevance, double ratio) {
        this.relevanceScore = relevanceScore;
        this.seedersScore = seedersScore;
        this.ratioScore = ratioScore;
        this.overallScore = overallScore;
        this.relevance = relevance;
        this.ratio = ratio;
    }

    public static SearchScore fromMap(Map map) {
        return new SearchScore(((Number) map.get("relevanceScore")).intValue(), ((Number) map.get("seedersScore")).intValue(),
                               ((Number) map.get("ratioScore")).intValue(), ((Number) map.get("overallScore")).intValue(),
                               ((Number) map.get("relevance")).doubleValue(), ((Number) map.get("ratio")).doubleValue());
    }

    public int getRelevanceScore() {
        return this.relevanceScore;
    }

    public int getSeedersScore() {
        return this.seedersScore;
    }

    public int getRatioScore() {
        return this.ratioScore;
    }

    public double getOverallScore() {
        return this.overallScore;
    }

    public double getRelevance() {
        return this.relevance;
    }

    public double getRatio() {
        return this.ratio;
    }
}
