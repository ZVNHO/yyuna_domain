package com.example.demo.service;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

class SmsServiceTest {

    @Test
    void 솔라피_인증_헤더는_date_salt를_시크릿으로_서명한_16진수() throws Exception {
        String date = "2026-11-28T15:04:05+09:00";
        String salt = "abcdefghijklmnopqrstuvwxyz012345";

        String header = SmsService.authorization("KEY123", "SECRET456", date, salt);

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("SECRET456".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String expected = HexFormat.of().formatHex(mac.doFinal((date + salt).getBytes(StandardCharsets.UTF_8)));
        assertThat(header).isEqualTo("HMAC-SHA256 apiKey=KEY123, date=" + date + ", salt=" + salt + ", signature=" + expected);
        assertThat(expected).matches("[0-9a-f]{64}");
    }

    @Test
    void 휴대폰_번호는_숫자만_남기고_가운데를_가린다() {
        assertThat(Phones.normalize("010-1234-5678")).isEqualTo("01012345678");
        assertThat(Phones.isValid("01012345678")).isTrue();
        assertThat(Phones.isValid("0101234567")).isTrue();  // 옛 10자리 번호
        assertThat(Phones.isValid("0212345678")).isFalse(); // 유선 번호
        assertThat(Phones.isValid("010123")).isFalse();
        assertThat(Phones.mask("01012345678")).isEqualTo("010-****-5678");
    }
}
