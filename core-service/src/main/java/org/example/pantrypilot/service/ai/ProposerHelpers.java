package org.example.pantrypilot.service.ai;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

final class ProposerHelpers {

    private ProposerHelpers() {
    }

    static String stringArg(AiFunctionCall call, String key) {
        Object v = call.args().get(key);
        return v == null ? null : v.toString();
    }

    static String stringArg(AiFunctionCall call, String key, String fallback) {
        Object v = call.args().get(key);
        return v == null ? fallback : v.toString();
    }

    static String nullableStringArg(AiFunctionCall call, String key, String fallback) {
        Object v = call.args().get(key);
        if (v == null) {
            return fallback;
        }
        String s = v.toString();
        return s.isBlank() ? fallback : s;
    }

    static Long longArg(AiFunctionCall call, String key) {
        Object v = call.args().get(key);
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(v.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    static Integer intArg(AiFunctionCall call, String key) {
        Long l = longArg(call, key);
        return l == null ? null : l.intValue();
    }

    static BigDecimal bigDecimalArg(AiFunctionCall call, String key, BigDecimal fallback) {
        Object v = call.args().get(key);
        if (v == null) {
            return fallback;
        }
        if (v instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        try {
            return new BigDecimal(v.toString());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    static LocalDate nullableLocalDateArg(AiFunctionCall call, String key, String fallback) {
        Object v = call.args().get(key);
        String raw = v == null ? fallback : v.toString();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
