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

    // 본인에게 보여줄 때: 01012345678 → 010-1234-5678
    static String format(String normalized) {
        if (normalized == null || normalized.isEmpty()) return null;
        if (normalized.length() == 11) return normalized.substring(0, 3) + "-" + normalized.substring(3, 7) + "-" + normalized.substring(7);
        if (normalized.length() == 10) return normalized.substring(0, 3) + "-" + normalized.substring(3, 6) + "-" + normalized.substring(6);
        return normalized;
    }

    // 응답에 보여줄 때 가운데를 가린다: 01012345678 → 010-****-5678
    static String mask(String normalized) {
        if (normalized.length() < 10) return "***";
        return normalized.substring(0, 3) + "-****-" + normalized.substring(normalized.length() - 4);
    }
}
