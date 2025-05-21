package com.example;

import java.util.List;

public class Main {
    private static final List<String> CATEGORIES = List.of("cs.AI", "cs.CL", "cs.LG", "cs.CV", "cs.NE");
    private static final int MAX_PAPERS_PER_CATEGORY = 100;

    public static void main(String[] args) {
        Db.initialize();
        long startTime = System.currentTimeMillis();

        try {
            Utils.fetchAndProcessAll(CATEGORIES, MAX_PAPERS_PER_CATEGORY);

            long endTime = System.currentTimeMillis();
            double elapsedTime = (endTime - startTime) / 1000.0;
            System.out.println("All papers processed and saved to database.");
            System.out.printf("Elapsed time: %.2f seconds%n", elapsedTime);
        } catch (Exception ex) {
            System.err.println("Error in processing: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}