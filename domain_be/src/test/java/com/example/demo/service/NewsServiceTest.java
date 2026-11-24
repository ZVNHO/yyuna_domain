package com.example.demo.service;

import com.example.demo.controller.dto.NewsResponse;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NewsServiceTest {

    private static ByteArrayInputStream xml(String body) {
        return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void Atom_피드의_entry를_뉴스로_바꾼다() throws Exception {
        List<NewsResponse.Item> items = NewsService.parse(xml("""
                <?xml version='1.0' encoding='UTF-8'?>
                <feed xmlns='http://www.w3.org/2005/Atom'>
                  <title>GeekNews</title>
                  <entry>
                    <title><![CDATA[한글 TeX 사용자 모임(KTUG) 홈페이지 재개 안내]]></title>
                    <link rel='alternate' href='https://news.hada.io/topic?id=37708&amp;utm_source=geeknews' />
                    <id>https://news.hada.io/topic?id=37708</id>
                    <published>2026-11-23T18:47:34+09:00</published>
                    <author><name>jsunam</name></author>
                    <content type='html'><![CDATA[<div><p>반가운 소식을 &amp; 전해 드립니다.</p><p>KTUG 홈페이지가   다시 열렸습니다.</p></div>…]]></content>
                  </entry>
                  <entry>
                    <title>BSD는 어떻게 AT&amp;amp;T 라이선스에서 벗어났나</title>
                    <id>https://news.hada.io/topic?id=37707</id>
                    <published>2026-11-23T18:00:00+09:00</published>
                  </entry>
                </feed>
                """));

        assertThat(items).hasSize(2);
        NewsResponse.Item first = items.get(0);
        assertThat(first.getTitle()).isEqualTo("한글 TeX 사용자 모임(KTUG) 홈페이지 재개 안내");
        assertThat(first.getLink()).isEqualTo("https://news.hada.io/topic?id=37708");
        assertThat(first.getAuthor()).isEqualTo("jsunam");
        assertThat(first.getPublishedAt()).isEqualTo("2026-11-23T18:47:34+09:00");
        assertThat(first.getSummary()).isEqualTo("반가운 소식을 & 전해 드립니다. KTUG 홈페이지가 다시 열렸습니다.");
        assertThat(items.get(1).getTitle()).isEqualTo("BSD는 어떻게 AT&T 라이선스에서 벗어났나");
        assertThat(items.get(1).getAuthor()).isEmpty();
        assertThat(items.get(1).getSummary()).isEmpty();
    }

    @Test
    void 요약에서_마크다운_굵게_표시를_뺀다() {
        assertThat(NewsService.summarize("<p>**친구가** 폰을 많이 써요</p>")).isEqualTo("친구가 폰을 많이 써요");
    }

    @Test
    void 요약은_160자에서_자르고_말줄임표를_붙인다() {
        String summary = NewsService.summarize("<p>" + "가".repeat(300) + "</p>");

        assertThat(summary).hasSize(161).endsWith("…");
    }

    @Test
    void DOCTYPE이_있는_XML은_거부한다_XXE_방지() {
        assertThatThrownBy(() -> NewsService.parse(xml("""
                <?xml version='1.0'?>
                <!DOCTYPE feed [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <feed xmlns='http://www.w3.org/2005/Atom'><entry><title>&xxe;</title><id>x</id></entry></feed>
                """))).isInstanceOf(Exception.class);
    }
}
