package com.mt.easytv.torrents.sources;

import com.mt.easytv.Helpers;
import com.mt.easytv.Main;
import com.mt.easytv.interaction.Messager;
import com.mt.easytv.interaction.Progress;
import com.mt.easytv.torrents.Torrent;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;

public final class PirateBayLocal implements TorrentSource
{
    @Override
    public Torrent view(String url) throws Exception {
        return null;
    }

    @Override
    public ArrayList<Torrent> search(String searchTerms, boolean showProgress) throws Exception {
        /* Load file */
        String torrentsPath = Main.config.getValue("tpbIndex");
        Progress progressTracker = new Progress(this._getTotalLines(new FileReader(torrentsPath)));
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
            double matchLevel = Helpers.matchPercent(searchTerms, lineParts[structure.name]);
            if (matchLevel * 100 >= matchThreshold) {
                Torrent torrent = new Torrent();
                torrent.name = lineParts[structure.name];
                torrent.seeders = Integer.parseInt(lineParts[structure.seeders]);
                torrent.leechers = Integer.parseInt(lineParts[structure.leechers]);
                torrent.size = Float.parseFloat(lineParts[structure.size]); //bytes
                torrent.size = (torrent.size / 1024f) / 1024f; //MB
                torrent.url = "magnet:?xt=urn:btih:" + lineParts[structure.magnet];
                matches.add(torrent);
            }

            progressTracker.increment();

            if (showProgress) {
                progressTracker.display();
            }
            lineIndex++;
        }
        reader.close();

        if (skipped > 0) {
            Messager.message("Skipped " + skipped + " during search");
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
}