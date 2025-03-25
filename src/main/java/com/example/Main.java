package com.example;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        Db.initialize();

        long startTime = System.currentTimeMillis();
        List<String> arxivLinks = Utils.getArxivHtmlLinks("https://arxiv.org/list/cs.SE/recent");

        for (String url : arxivLinks) {
            PageData paperData = Utils.processArxivDocument(url);
            Db.savePaper(paperData.getTitle(),
                    paperData.getAuthors(),
                    paperData.getContent(),
                    paperData.getUrl());
        }

        System.out.println("Results saved in database");
        long endTime = System.currentTimeMillis();
        long elapsedTime = (endTime - startTime)/1000;
        System.out.println("Elapsed time: " + elapsedTime + " seconds");
    }
}