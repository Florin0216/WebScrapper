package com.example;

import java.util.List;
import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        Db.initialize();

        List<String> categoryUrls = new ArrayList<>();
        categoryUrls.add("https://arxiv.org/list/cs.SE/recent");
        categoryUrls.add("https://arxiv.org/list/cs.AI/recent");
        categoryUrls.add("https://arxiv.org/list/cs.CL/recent");
        categoryUrls.add("https://arxiv.org/list/cs.CV/recent");
        categoryUrls.add("https://arxiv.org/list/cs.SY/recent");
        categoryUrls.add("https://arxiv.org/list/cs.DB/recent");
        categoryUrls.add("https://arxiv.org/list/cs.IR/recent");
        categoryUrls.add("https://arxiv.org/list/cs.GT/recent");

        long startTime = System.currentTimeMillis();

        List<String> arxivLinks = new ArrayList<>();
        for (String categoryUrl : categoryUrls) {
            arxivLinks.addAll(Utils.getArxivHtmlLinks(categoryUrl));
        }

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