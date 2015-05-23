package com.mt.easytv.torrents;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class SearchScore
{
    public int    relevanceScore;
    public int    seedersScore;
    public int    ratioScore;
    public double relevance;
    public double ratio;

    @Expose
    @SerializedName("overallScore")
    private int _overallScore;

    /**
     * Creates a new instance of the SearchScore and
     * sets the two required variables
     *
     * @param relevance The Levenshtein edit distance from the search terms
     * @param ratio     The ratio of seeders to leechers
     */
    public SearchScore(double relevance, double ratio) {
        this.relevance = relevance;
        this.ratio = ratio;
    }

    /**
     * Creates a new instance of SearchScore and sets up the entire scoring for a Torrent
     *
     * @param relevanceScore The score that the torrent got for its edit distance
     * @param seedersScore   The score that the torrent got for its number of seeders
     * @param ratioScore     The score that the torrent got for its S/L ratio
     * @param relevance      The edit distance from the search terms
     * @param ratio          The S/L ratio
     */
    public SearchScore(int relevanceScore, int seedersScore, int ratioScore, double relevance, double ratio) {
        this.relevanceScore = relevanceScore;
        this.seedersScore = seedersScore;
        this.ratioScore = ratioScore;
        this.relevance = relevance;
        this.ratio = ratio;
        this.updateOverallScore();
    }

    public static double calculateRatio(int seeders, int leechers) {
        if (seeders <= 0 || leechers <= 0) {
            return 0;
        }

        return seeders / leechers;
    }

    /**
     * Returns the current overall score
     *
     * @return The current overall score or null if updateOverallScore hasn't been ran to calculate it yet
     */
    public int getOverallScore() {
        return this._overallScore;
    }

    /**
     * Updates the overall score variable
     */
    public void updateOverallScore() {
        this._overallScore = this.relevanceScore + this.seedersScore + (this.ratio <= 0 ? 1000 : this.ratioScore); //ratio not important
    }
}