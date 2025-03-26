package com.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Utils {
    public static List<String> getArxivHtmlLinks(String url) {
        List<String> htmlLinks = new ArrayList<>();

        try {
            Document doc = Jsoup.connect(url).get();
            Elements links = doc.select("a[href^=/abs/]");

            for (Element link : links) {
                String paperId = link.attr("href").replace("/abs/", "");
                String htmlUrl = "https://arxiv.org/html/" + paperId;
                htmlLinks.add(htmlUrl);
            }

        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }

        return htmlLinks;
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
            if (!authorElements.isEmpty()) {
                for (int i = 0; i < authorElements.size(); i++) {
                    authorElements.get(i).select("sup").remove();
                    String author = authorElements.get(i).text().trim();
                    author = author.replaceAll("[0-9,]", "");
                    authorsBuilder.append(author);
                    if (i < authorElements.size() - 1) {
                        authorsBuilder.append(", ");
                    }
                }
                pageData.setAuthors(authorsBuilder.toString());
            } else {
                pageData.setAuthors("Not found");
            }

            // Extract content
            StringBuilder contentBuilder = new StringBuilder();
            Elements headers = doc.select("h1, h2, h3, h4, h5, h6");

            for (Element header : headers) {
                if (!header.tagName().equals("h1")) {
                    String sectionTitle = header.text();
                    contentBuilder.append("\n").append(sectionTitle).append("\n");

                    Element nextElement = header.nextElementSibling();
                    if (sectionTitle.toLowerCase().contains("reference") ||
                            sectionTitle.toLowerCase().contains("bibliography")) {
                        while (nextElement != null && !nextElement.is("h1, h2, h3, h4, h5, h6")) {
                            if (nextElement.is("ul.ltx_biblist")) {
                                Elements referenceItems = nextElement.select("li");
                                for (Element ref : referenceItems) {
                                    String reference = ref.text().trim();
                                    contentBuilder.append("    - ").append(reference).append("\n");
                                }
                            }
                            nextElement = nextElement.nextElementSibling();
                        }
                    } else {
                        while (nextElement != null && !nextElement.is("h1, h2, h3, h4, h5, h6")) {
                            if (nextElement.is("div") || nextElement.is("p") || nextElement.is("span")) {
                                String paragraph = nextElement.text();
                                contentBuilder.append("    ").append(paragraph).append("\n");
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