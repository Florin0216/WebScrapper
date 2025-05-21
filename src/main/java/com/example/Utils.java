package com.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class Utils {
    private static final String ARXIV_API_URL = "http://export.arxiv.org/api/query?";
    private static final int MAX_RESULTS = 100;
    private static final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public static void fetchAndProcessAll(List<String> categories, int maxPapersPerCategory) throws InterruptedException, ExecutionException {
        List<String> allUrls = new ArrayList<>();

        List<Thread> fetchThreads = categories.stream()
                .map(category -> Thread.ofVirtual().start(() -> {
                    List<String> urls = fetchPapersFromCategory(category, maxPapersPerCategory);
                    synchronized (allUrls) {
                        allUrls.addAll(urls);
                    }
                }))
                .collect(Collectors.toList());

        for (Thread thread : fetchThreads) {
            thread.join();
        }

        List<Thread> processThreads = allUrls.stream()
                .map(url -> Thread.ofVirtual().start(() -> {
                    PageData paperData = processArxivDocument(url);
                    if (paperData != null) {
                        Db.savePaper(
                                paperData.getTitle(),
                                paperData.getAuthors(),
                                paperData.getContent(),
                                paperData.getUrl()
                        );
                    }
                }))
                .collect(Collectors.toList());

        for (Thread thread : processThreads) {
            thread.join();
        }
    }

    public static List<String> fetchPapersFromCategory(String category, int maxPapers) {
        List<String> links = new ArrayList<>();
        int start = 0;
        int totalResults = 0;

        try {
            while (true) {
                String query = String.format("search_query=cat:%s&start=%d&max_results=%d&sortBy=submittedDate&sortOrder=descending",
                        category, start, MAX_RESULTS);
                Document doc = Jsoup.connect(ARXIV_API_URL + query).get();
                Elements entries = doc.select("entry");

                if (entries.isEmpty()) break;

                for (Element entry : entries) {
                    if (maxPapers > 0 && totalResults >= maxPapers) break;
                    String id = entry.select("id").text();
                    String paperId = id.substring(id.lastIndexOf('/') + 1);
                    links.add("https://arxiv.org/html/" + paperId);
                    totalResults++;
                }

                if (entries.size() < MAX_RESULTS || (maxPapers > 0 && totalResults >= maxPapers)) {
                    break;
                }

                start += MAX_RESULTS;
                Thread.sleep(3000);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error fetching from " + category + ": " + e.getMessage());
        }

        System.out.printf("Fetched %d papers from category %s%n", links.size(), category);
        return links;
    }

    public static PageData processArxivDocument(String url) {
        PageData pageData = new PageData();
        pageData.setUrl(url);

        try {
            Document doc = Jsoup.connect(url).get();

            String title = doc.select("h1").text();
            pageData.setTitle(title);

            Elements authorElements = doc.select(".ltx_personname");
            StringBuilder authorsBuilder = new StringBuilder();
            for (int i = 0; i < authorElements.size(); i++) {
                authorElements.get(i).select("sup").remove();
                String author = authorElements.get(i).text().trim().replaceAll("[0-9,]", "");
                authorsBuilder.append(author);
                if (i < authorElements.size() - 1) {
                    authorsBuilder.append(", ");
                }
            }
            pageData.setAuthors(authorsBuilder.length() > 0 ? authorsBuilder.toString() : "Not found");

            StringBuilder contentBuilder = new StringBuilder();
            Elements headers = doc.select("h1, h2, h3, h4, h5, h6");

            for (Element header : headers) {
                if (!header.tagName().equals("h1")) {
                    String sectionTitle = header.text();
                    contentBuilder.append("\n").append(sectionTitle).append("\n");

                    Element nextElement = header.nextElementSibling();
                    if (sectionTitle.toLowerCase().contains("reference") ||
                            sectionTitle.toLowerCase().contains("bibliography")) {
                        while (nextElement != null && !nextElement.tagName().matches("h[1-6]")) {
                            if (nextElement.is("ul.ltx_biblist")) {
                                Elements referenceItems = nextElement.select("li");
                                for (Element ref : referenceItems) {
                                    contentBuilder.append("    - ").append(ref.text().trim()).append("\n");
                                }
                            }
                            nextElement = nextElement.nextElementSibling();
                        }
                    } else {
                        while (nextElement != null && !nextElement.tagName().matches("h[1-6]")) {
                            if (nextElement.is("div") || nextElement.is("p") || nextElement.is("span")) {
                                contentBuilder.append("    ").append(nextElement.text()).append("\n");
                            }
                            nextElement = nextElement.nextElementSibling();
                        }
                    }
                }
            }

            pageData.setContent(contentBuilder.toString());

        } catch (IOException e) {
            System.out.println("Error processing " + url + ": " + e.getMessage());
        }

        return pageData;
    }
}