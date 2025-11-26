package com.campus.tracker.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ECampusManager {

    public Map<String, Object> fetchAllData(String username, String password) {
        Map<String, Object> combinedData = new HashMap<>();
        ECampusScraper oldScraper = null;
        New_ECampusScraper newScraper = null;

        try {
            // === STEP 1: Fetch Exam Results from Old eCampus ===
            System.out.println("Step 1: Fetching exam results from old eCampus...");
            oldScraper = new ECampusScraper();
            Map<String, Object> oldData = oldScraper.loginAndScrape(username, password);
            System.out.println("Exam results fetched successfully");

            // === STEP 2: Fetch CA Marks from New eCampus ===
            System.out.println("Step 2: Fetching CA marks from new eCampus...");
            newScraper = new New_ECampusScraper();
            Map<String, Object> newData = newScraper.loginAndScrape(username, password);
            System.out.println("CA marks fetched successfully");

            // === MERGE ALL DATA ===
            combinedData.put("name", getOrDefault(oldData, "name", "Student"));
            combinedData.put("rollNo", getOrDefault(oldData, "rollNo", username));
            combinedData.put("programme", getOrDefault(oldData, "programme", "Unknown Programme"));
            combinedData.put("examResults", getOrDefault(oldData, "examResults", new ArrayList<>()));
            combinedData.put("caMarks", getOrDefault(newData, "caMarks", new ArrayList<>()));

            System.out.println("All data merged successfully!");
            return combinedData;

        } catch (Exception e) {
            System.err.println("Error in eCampus data fetch: " + e.getMessage());
            e.printStackTrace();

            // === FALLBACK: Return partial data if one scraper fails ===
            if (oldScraper != null) {
                try {
                    Map<String, Object> oldData = oldScraper.loginAndScrape(username, password);
                    combinedData.put("name", getOrDefault(oldData, "name", "Student"));
                    combinedData.put("rollNo", getOrDefault(oldData, "rollNo", username));
                    combinedData.put("programme", getOrDefault(oldData, "programme", "Unknown"));
                    combinedData.put("examResults", getOrDefault(oldData, "examResults", new ArrayList<>()));
                    combinedData.put("caMarks", new ArrayList<>());
                    System.out.println("Partial data loaded (only exam results)");
                    return combinedData;
                } catch (Exception ignored) {}
            }

            throw new RuntimeException("Failed to fetch any eCampus data: " + e.getMessage(), e);
        } finally {
            // Ensure drivers are always closed
            closeQuietly(oldScraper);
            closeQuietly(newScraper);
        }
    }

    // === HELPER: Safe get with default ===
    private <T> T getOrDefault(Map<String, Object> map, String key, T defaultValue) {
        Object value = map.get(key);
        return value != null ? (T) value : defaultValue;
    }

    // === HELPER: Close scraper safely ===
    private void closeQuietly(AutoCloseable scraper) {
        if (scraper != null) {
            try {
                scraper.close();
            } catch (Exception e) {
                System.err.println("Warning: Failed to close scraper: " + e.getMessage());
            }
        }
    }
}