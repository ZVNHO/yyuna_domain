package com.example.demo.service;

import java.util.regex.Pattern;

//휴대폰 번호는 숫자만 남겨(01012345678) 저장·비교한다
final class Phones {

    private static final Pattern MOBILE = Pattern.compile("^01[016789]\\d{7,8}$");

    private Phones() {
    }

    static String normalize(String phone) {
        return phone == null ? "" : phone.replaceAll("\\D", "");
    }

    static boolean isValid(String normalized) {
        return MOBILE.matcher(normalized).matches();
    }

    // 응답에 보여줄 때 가운데를 가린다: 01012345678 → 010-****-5678
    static String mask(String normalized) {
        if (normalized.length() < 10) return "***";
        return normalized.substring(0, 3) + "-****-" + normalized.substring(normalized.length() - 4);
    }
}
