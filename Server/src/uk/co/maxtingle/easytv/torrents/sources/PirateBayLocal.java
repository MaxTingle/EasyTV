package uk.co.maxtingle.easytv.torrents.sources;

import uk.co.maxtingle.communication.debug.Debugger;
import uk.co.maxtingle.easytv.Helpers;
import uk.co.maxtingle.easytv.Main;
import uk.co.maxtingle.easytv.interaction.Progress;
import uk.co.maxtingle.easytv.torrents.SearchScore;
import uk.co.maxtingle.easytv.torrents.Torrent;

import java.io.*;
import java.util.ArrayList;

/**
 * Searches an un-indexed local copy of the piratebay's database
 * For use when the website goes down
 */
public final class PirateBayLocal implements ITorrentSource
{
    private final class Structure
    { //int representing the array index of each column
        public static final int EXPECTED_ITEMS = 6;
        public int id;
        public int name;
        public int size;
        public int seeders;
        public int leechers;
        public int magnet;
    }

    /**
     * Gets a single torrent from the url/magnet provided
     *
     * @return The torrent or null if it is not found
     */
    @Override
    public Torrent view(String url) throws Exception {
        /* Load file */
        String torrentsPath = Main.config.concatValues(new String[]{"storage", "tpbIndex"}, "\\");

        if (!new File(torrentsPath).exists()) {
            Debugger.log("App", "PirateBayLocal: Index file not found.");
            return null;
        }

        BufferedReader reader = new BufferedReader(new FileReader(torrentsPath));

        /* Parts structure */
        String line = reader.readLine();
        Structure structure = this._readStructure(line);

        /* Tracking */
        int lineIndex = 0;

        while ((line = reader.readLine()) != null) {
            String[] lineParts = line.split("\\|");

            /* Validation / reformatting of line */
            if (lineParts.length < Structure.EXPECTED_ITEMS) {
                continue;
            }
            else if (lineParts.length > Structure.EXPECTED_ITEMS) { //torrent name can have |, remake the array based upon the total extra |
                String[] realLineParts = new String[Structure.EXPECTED_ITEMS];
                int overflow = lineParts.length - Structure.EXPECTED_ITEMS;
                int overflowProcessed = 0;
                int namePos = -1;
                int realIndex = 0;

                for (int i = 0; i < lineParts.length; i++) {
                    if (i == structure.name) {
                        namePos = i;
                        realLineParts[i] = lineParts[i];
                        realIndex++;
                    }
                    else if (namePos != -1 && overflowProcessed < overflow) {
                        realLineParts[namePos] += lineParts[i];
                        overflowProcessed++;
                    }
                    else {
                        realLineParts[realIndex] = lineParts[i];
                        realIndex++;
                    }
                }

                if (realIndex != Structure.EXPECTED_ITEMS) {
                    throw new Exception("Line" + lineIndex + " has an overflow of " + overflow + " pipes. Splitting rejoining failed.");
                }

                lineParts = realLineParts;
            }

            /* Processing into a torrent */
            if (lineParts[structure.magnet].equals(url)) {
                int seeders = Integer.parseInt(lineParts[structure.seeders]);
                int leechers = Integer.parseInt(lineParts[structure.leechers]);

                Torrent torrent = new Torrent(new SearchScore(0, seeders <= 0 || leechers <= 0 ? 0 : seeders / leechers));
                torrent.name = lineParts[structure.name];
                torrent.seeders = seeders;
                torrent.leechers = leechers;
                torrent.size = Helpers.byteToMB(Float.parseFloat(lineParts[structure.size])); //bytes
                torrent.url = lineParts[structure.magnet];
                reader.close();
                return torrent;
            }

            lineIndex++;
        }

        return null;
    }

    /**
     * Searches all the torrents and returns all the ones that have a name similiarity
     * score greater than the config threshold
     *
     * @return The search matches
     * @throws Exception The search failed because of a critical error
     */
    @Override
    public ArrayList<Torrent> search(String searchTerms, boolean showProgress) throws Exception {
        searchTerms = Helpers.cleanTorrentName(searchTerms).toLowerCase();

        /* Load file */
        String torrentsPath = Main.config.getValue("tpbIndex");

        if (!new File(torrentsPath).exists()) {
            Debugger.log("App", "PirateBayLocal: Index file not found.");
            return new ArrayList<>();
        }

        Progress progressTracker = new Progress(this._getTotalLines(new FileReader(torrentsPath)), "Piratebay local search:");
        BufferedReader reader = new BufferedReader(new FileReader(torrentsPath));

        /* Parts structure */
        String line = reader.readLine();
        Structure structure = this._readStructure(line);

        /* Get matches */
        double matchThreshold = Double.parseDouble(Main.config.getValue("matchThreshold"));
        ArrayList<Torrent> matches = new ArrayList<>();

        /* Tracking */
        int lineIndex = 0;
        int skipped = 0;

        if (showProgress) {
            progressTracker.show();
        }

        int minimumSeeders = Integer.parseInt(Main.config.getValue("minimumSeeders"));

        while ((line = reader.readLine()) != null) {
            String[] lineParts = line.split("\\|");

            /* Validation / reformatting of line */
            if (lineParts.length < Structure.EXPECTED_ITEMS) {
                skipped++;
                continue;
            }
            else if (lineParts.length > Structure.EXPECTED_ITEMS) { //torrent name can have |, remake the array based upon the total extra |
                String[] realLineParts = new String[Structure.EXPECTED_ITEMS];
                int overflow = lineParts.length - Structure.EXPECTED_ITEMS;
                int overflowProcessed = 0;
                int namePos = -1;
                int realIndex = 0;

                for (int i = 0; i < lineParts.length; i++) {
                    if (i == structure.name) {
                        namePos = i;
                        realLineParts[i] = lineParts[i];
                        realIndex++;
                    }
                    else if (namePos != -1 && overflowProcessed < overflow) {
                        realLineParts[namePos] += lineParts[i];
                        overflowProcessed++;
                    }
                    else {
                        realLineParts[realIndex] = lineParts[i];
                        realIndex++;
                    }
                }

                if (realIndex != Structure.EXPECTED_ITEMS) {
                    throw new Exception("Line" + lineIndex + " has an overflow of " + overflow + " pipes. Splitting rejoining failed.");
                }

                lineParts = realLineParts;
            }

            /* Processing into a torrent */
            int seeders = Integer.parseInt(lineParts[structure.seeders]);
            if(seeders < minimumSeeders) {
                continue;
            }

            String name = Helpers.cleanTorrentName(lineParts[structure.name]).toLowerCase();
            double levenshteinDistance = Helpers.similarity(searchTerms, name) * 100;

            if (Math.round(levenshteinDistance) >= matchThreshold) {
                int leechers = Integer.parseInt(lineParts[structure.leechers]);

                Torrent torrent = new Torrent(new SearchScore(levenshteinDistance, SearchScore.calculateRatio(seeders, leechers)));
                torrent.name = lineParts[structure.name];
                torrent.seeders = seeders;
                torrent.leechers = leechers;
                torrent.size = Helpers.byteToMB(Float.parseFloat(lineParts[structure.size])); //bytes
                torrent.url = "magnet:?xt=urn:btih:" + lineParts[structure.magnet];
                matches.add(torrent);
            }

            progressTracker.increment();
            lineIndex++;
        }

        reader.close();
        progressTracker.hide();

        if (skipped > 0) {
            Debugger.log("App", "Piratebay local search: Skipped " + skipped + " during search");
        }

        return matches;
    }

    private int _getTotalLines(FileReader reader) throws IOException {
        LineNumberReader lineNumberReader = new LineNumberReader(reader);
        lineNumberReader.skip(Long.MAX_VALUE);
        return lineNumberReader.getLineNumber() + 1;
    }

    private Structure _readStructure(String firstRow) throws Exception {
        if (firstRow == null) {
            throw new Exception("Structure row is null.");
        }

        String[] parts = firstRow.split("\\|");
        Structure structure = new Structure();
        int loadedStructure = 0;

        for (int i = 0; i < parts.length; i++) {
            switch (parts[i]) {
                default:
                    throw new Exception("Column " + parts[i] + " not recognized.");
                case "id":
                    loadedStructure++;
                    structure.id = i;
                    break;
                case "name":
                    loadedStructure++;
                    structure.name = i;
                    break;
                case "size":
                    loadedStructure++;
                    structure.size = i;
                    break;
                case "seeders":
                    loadedStructure++;
                    structure.seeders = i;
                    break;
                case "leechers":
                    loadedStructure++;
                    structure.leechers = i;
                    break;
                case "magnet":
                    loadedStructure++;
                    structure.magnet = i;
                    break;
            }
        }

        if (loadedStructure < Structure.EXPECTED_ITEMS) {
            throw new Exception("Incomplete structure.");
        }

        return structure;
    }
}