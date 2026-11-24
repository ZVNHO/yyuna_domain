package com.example.demo.controller.dto;

import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
public class NewsResponse {
    private List<Item> items;
    private Instant fetchedAt; // 원본 피드를 마지막으로 받아온 시각
    private String source;

    public static NewsResponse createResponse(List<Item> items, Instant fetchedAt) {
        NewsResponse response = new NewsResponse();
        response.items = items;
        response.fetchedAt = fetchedAt;
        response.source = "https://news.hada.io";
        return response;
    }

    @Getter
    public static class Item {
        private String id;
        private String title;
        private String summary; // 본문 앞부분 (HTML 태그를 뺀 짧은 글)
        private String link;
        private String author;
        private String publishedAt;

        public static Item createItem(String id, String title, String summary, String link, String author, String publishedAt) {
            Item item = new Item();
            item.id = id;
            item.title = title;
            item.summary = summary;
            item.link = link;
            item.author = author;
            item.publishedAt = publishedAt;
            return item;
        }
    }
}
