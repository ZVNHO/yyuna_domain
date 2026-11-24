package com.example.demo.service;

import java.util.Locale;
import java.util.regex.Pattern;

//이메일은 앞뒤 공백을 없애고 소문자로 저장·비교한다
final class Emails {

    private static final Pattern FORMAT = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private Emails() {
    }

    static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    static boolean isValid(String normalized) {
        return normalized.length() <= 100 && FORMAT.matcher(normalized).matches();
    }

    // 응답에 보여줄 때 일부를 가린다: yuna0314@gmail.com → yu******@gmail.com
    static String mask(String email) {
        int at = email.indexOf('@');
        if (at < 0) return "***";
        String local = email.substring(0, at);
        int visible = local.length() <= 2 ? 1 : 2;
        return local.substring(0, visible) + "*".repeat(Math.max(local.length() - visible, 1)) + email.substring(at);
    }
}
