package com.example.demo.service;

import com.example.demo.controller.dto.NewsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

//GeekNews(news.hada.io) 공식 RSS(Atom) 피드를 받아 최신 뉴스 목록으로 돌려준다
//원본 사이트에 부담을 주지 않도록 1분 동안은 저장해 둔 결과를 쓴다
@Slf4j
@Service
public class NewsService {

    private static final URI FEED_URL = URI.create("https://news.hada.io/rss/news");
    private static final String ATOM = "http://www.w3.org/2005/Atom";
    private static final Duration CACHE_TTL = Duration.ofMinutes(1);
    private static final int SUMMARY_LENGTH = 160;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private NewsResponse cached;
    private Instant cachedAt = Instant.EPOCH;

    public synchronized NewsResponse getLatest() {
        Instant now = Instant.now();
        if (cached != null && cachedAt.plus(CACHE_TTL).isAfter(now)) {
            return cached;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(FEED_URL)
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "YunasBakeryLab/1.0 (+http://yyuna03.cafe24.com)")
                    .GET()
                    .build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("피드 응답 코드 " + response.statusCode());
            }
            try (InputStream body = response.body()) {
                cached = NewsResponse.createResponse(parse(body), now);
                cachedAt = now;
            }
            return cached;
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.warn("GeekNews 피드를 받아오지 못했습니다: {}", e.toString());
            if (cached != null) return cached; // 잠깐 실패해도 마지막 결과를 보여준다
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "뉴스를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.");
        }
    }

    // Atom 피드의 entry를 뉴스 항목으로 바꾼다. 외부 XML이므로 DOCTYPE(외부 엔티티)을 허용하지 않는다
    static List<NewsResponse.Item> parse(InputStream xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setExpandEntityReferences(false);
        Document document = factory.newDocumentBuilder().parse(xml);

        List<NewsResponse.Item> items = new ArrayList<>();
        NodeList entries = document.getElementsByTagNameNS(ATOM, "entry");
        for (int i = 0; i < entries.getLength(); i++) {
            Element entry = (Element) entries.item(i);
            String id = text(entry, "id");
            if (id.isEmpty()) continue;
            Element author = (Element) entry.getElementsByTagNameNS(ATOM, "author").item(0);
            items.add(NewsResponse.Item.createItem(
                    id,
                    decodeEntities(text(entry, "title")), // 원본 제목에 &amp;가 한 번 더 들어 있는 경우가 있다
                    summarize(text(entry, "content")),
                    id, // <id>가 utm 파라미터 없는 원본 글 주소(https://news.hada.io/topic?id=...)
                    author == null ? "" : text(author, "name"),
                    text(entry, "published")));
        }
        return items;
    }

    // 본문 HTML에서 태그를 빼고 글자만 남겨 앞부분만 자른다 (화면에는 일반 글자로만 보여준다)
    static String summarize(String html) {
        String plain = decodeEntities(html.replaceAll("<[^>]*>", " "))
                .replace("**", "") // 본문에 섞여 있는 마크다운 굵게 표시
                .replaceAll("\\s+", " ")
                .replaceAll("…$", "")
                .trim();
        return plain.length() <= SUMMARY_LENGTH ? plain : plain.substring(0, SUMMARY_LENGTH).trim() + "…";
    }

    static String decodeEntities(String text) {
        return text.replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&amp;", "&");
    }

    private static String text(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagNameNS(ATOM, tag);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
    }
}
