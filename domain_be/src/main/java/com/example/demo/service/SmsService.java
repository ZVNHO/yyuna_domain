package com.example.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

//문자(SMS) 발송 (솔라피). API 키·시크릿·발신번호는 secret.yaml의 app.sms.* 에 둔다
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    private static final URI SEND_URL = URI.create("https://api.solapi.com/messages/v4/send-many/fast");
    private static final String SALT_CHARS = "1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final SecureRandom random = new SecureRandom();

    @Value("${app.sms.api-key:}")
    private String apiKey;

    @Value("${app.sms.api-secret:}")
    private String apiSecret;

    @Value("${app.sms.from:}")
    private String from; // 솔라피에 등록·인증한 발신번호

    public boolean isConfigured() {
        return StringUtils.hasText(apiKey) && StringUtils.hasText(apiSecret) && StringUtils.hasText(from);
    }

    // 보냈는지 결과를 돌려준다 (실패 원인은 로그에 남긴다)
    public boolean trySend(String to, String text) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "messages", List.of(Map.of("to", to, "from", Phones.normalize(from), "text", text))));
            HttpRequest request = HttpRequest.newBuilder(SEND_URL)
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", authorization(apiKey, apiSecret, now(), salt()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.error("문자 발송 실패: status={} body={}", response.statusCode(), response.body());
                return false;
            }
            JsonNode failed = objectMapper.readTree(response.body()).path("failedMessageList");
            if (failed.isArray() && !failed.isEmpty()) {
                log.error("문자 발송 실패: {}", failed);
                return false;
            }
            return true;
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.error("문자 발송 실패: {}", e.toString());
            return false;
        }
    }

    // 솔라피 API Key 인증: HMAC-SHA256(date + salt, apiSecret)을 16진수로
    static String authorization(String apiKey, String apiSecret, String date, String salt) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = HexFormat.of().formatHex(mac.doFinal((date + salt).getBytes(StandardCharsets.UTF_8)));
        return "HMAC-SHA256 apiKey=" + apiKey + ", date=" + date + ", salt=" + salt + ", signature=" + signature;
    }

    private static String now() {
        return OffsetDateTime.now(ZoneId.of("Asia/Seoul")).truncatedTo(ChronoUnit.SECONDS)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private String salt() {
        StringBuilder salt = new StringBuilder(32);
        for (int i = 0; i < 32; i++) salt.append(SALT_CHARS.charAt(random.nextInt(SALT_CHARS.length())));
        return salt.toString();
    }
}
