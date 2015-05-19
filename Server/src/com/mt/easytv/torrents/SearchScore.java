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
    private double _overallScore;

    public SearchScore(double relevance, double ratio) {
        this.relevance = relevance;
        this.ratio = ratio;
    }

    public SearchScore(int relevanceScore, int seedersScore, int ratioScore, double relevance, double ratio) {
        this.relevanceScore = relevanceScore;
        this.seedersScore = seedersScore;
        this.ratioScore = ratioScore;
        this.relevance = relevance;
        this.ratio = ratio;
        this.updateOverallScore();
    }

    public double getOverallScore() {
        return this._overallScore;
    }

    public void updateOverallScore() {
        this._overallScore = this.relevanceScore + this.seedersScore + this.ratioScore;
    }
}