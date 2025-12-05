package dev.gotcraft.gotCraftHolo.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for leaderboard holograms
 */
public class LeaderboardConfig {

    private String title;
    private int maxDisplayEntries;
    private String suffix;
    private String leaderboardType; // TOP_PLAYER_HEAD, ALL_PLAYER_HEADS, SIMPLE_TEXT
    private boolean showEmptyPlaces;

    // Format strings
    private String titleFormat;
    private String footerFormat;
    private List<String> placeFormats; // Custom formats for ranks 1, 2, 3
    private String defaultPlaceFormat;

    // Placeholder entries (each entry is a placeholder for player name and score)
    private List<LeaderboardEntry> entries;

    // Visual settings
    private double lineHeight;
    private boolean background;
    private int backgroundColor;

    public LeaderboardConfig() {
        this.title = "Leaderboard";
        this.maxDisplayEntries = 10;
        this.suffix = "points";
        this.leaderboardType = "TOP_PLAYER_HEAD";
        this.showEmptyPlaces = false;

        this.titleFormat = "<gradient:#ff6000:#ffc663>--------- {title} ---------</gradient>";
        this.footerFormat = "";
        this.placeFormats = new ArrayList<>();
        this.defaultPlaceFormat = "<color:#ffb486><bold>{place}.</bold></color> <color:#ffb486>{name}</color> <gray>{score}</gray> <white>{suffix}</white>";

        this.entries = new ArrayList<>();
        this.lineHeight = 0.25;
        this.background = false;
        this.backgroundColor = 0x54000000;
    }

    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getMaxDisplayEntries() { return maxDisplayEntries; }
    public void setMaxDisplayEntries(int maxDisplayEntries) { this.maxDisplayEntries = maxDisplayEntries; }

    public String getSuffix() { return suffix; }
    public void setSuffix(String suffix) { this.suffix = suffix; }

    public String getLeaderboardType() { return leaderboardType; }
    public void setLeaderboardType(String leaderboardType) { this.leaderboardType = leaderboardType; }

    public boolean isShowEmptyPlaces() { return showEmptyPlaces; }
    public void setShowEmptyPlaces(boolean showEmptyPlaces) { this.showEmptyPlaces = showEmptyPlaces; }

    public String getTitleFormat() { return titleFormat; }
    public void setTitleFormat(String titleFormat) { this.titleFormat = titleFormat; }

    public String getFooterFormat() { return footerFormat; }
    public void setFooterFormat(String footerFormat) { this.footerFormat = footerFormat; }

    public List<String> getPlaceFormats() { return placeFormats; }
    public void setPlaceFormats(List<String> placeFormats) { this.placeFormats = placeFormats; }

    public String getDefaultPlaceFormat() { return defaultPlaceFormat; }
    public void setDefaultPlaceFormat(String defaultPlaceFormat) { this.defaultPlaceFormat = defaultPlaceFormat; }

    public List<LeaderboardEntry> getEntries() { return entries; }
    public void setEntries(List<LeaderboardEntry> entries) { this.entries = entries; }

    public double getLineHeight() { return lineHeight; }
    public void setLineHeight(double lineHeight) { this.lineHeight = lineHeight; }

    public boolean isBackground() { return background; }
    public void setBackground(boolean background) { this.background = background; }

    public int getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(int backgroundColor) { this.backgroundColor = backgroundColor; }

    /**
     * Represents a single leaderboard entry with placeholders
     */
    public static class LeaderboardEntry {
        private int rank;
        private String namePlaceholder;
        private String scorePlaceholder;

        public LeaderboardEntry(int rank, String namePlaceholder, String scorePlaceholder) {
            this.rank = rank;
            this.namePlaceholder = namePlaceholder;
            this.scorePlaceholder = scorePlaceholder;
        }

        public int getRank() { return rank; }
        public void setRank(int rank) { this.rank = rank; }

        public String getNamePlaceholder() { return namePlaceholder; }
        public void setNamePlaceholder(String namePlaceholder) { this.namePlaceholder = namePlaceholder; }

        public String getScorePlaceholder() { return scorePlaceholder; }
        public void setScorePlaceholder(String scorePlaceholder) { this.scorePlaceholder = scorePlaceholder; }
    }
}

