package plataforma.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Central sanitizer for known secret-bearing fields in API errors and telemetry payloads. */
public final class SecretRedactor {

    private static final String SECRET_KEYS = "password|senha|access[_-]?token|refresh[_-]?token|token|"
            + "authorization|secret|api[_-]?key|set-cookie|cookie";
    private static final Pattern KEY_PREFIX = Pattern.compile(
            "(?i)([\\\"']?(?:" + SECRET_KEYS + ")[\\\"']?\\s*[:=]\\s*)");
    private static final Pattern NEXT_KEY = Pattern.compile("(?i)(?:^|[\\s,;])(?:" + SECRET_KEYS + ")\\s*[:=]");
    private static final Pattern CPF = Pattern.compile("(?<![0-9])(?:[0-9]{3}\\.?[0-9]{3}\\.?[0-9]{3}-?[0-9]{2})(?![0-9])");
    private static final Set<String> SECRET_KEY_NAMES = Set.of(
            "password", "senha", "token", "accesstoken", "refreshtoken", "authorization", "secret", "apikey",
            "cookie", "setcookie");

    private SecretRedactor() {
    }

    /** Masks complete known secret values while preserving surrounding text and delimiters. */
    public static String redactText(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        String fieldsRedacted = redactFields(value);
        Matcher cpfMatcher = CPF.matcher(fieldsRedacted);
        StringBuffer result = new StringBuffer();
        while (cpfMatcher.find()) {
            String candidate = cpfMatcher.group();
            if (isValidCpf(candidate)) {
                cpfMatcher.appendReplacement(result, "[REDACTED]");
            }
        }
        cpfMatcher.appendTail(result);
        return result.toString();
    }

    private static String redactFields(String text) {
        Matcher fields = KEY_PREFIX.matcher(text);
        StringBuilder result = new StringBuilder(text.length());
        int cursor = 0;
        while (fields.find(cursor)) {
            result.append(text, cursor, fields.start());
            result.append(fields.group(1));
            int valueStart = fields.end();
            int lineEnd = valueStart;
            while (lineEnd < text.length() && text.charAt(lineEnd) != '\n' && text.charAt(lineEnd) != '\r') {
                lineEnd++;
            }

            if (valueStart < lineEnd && (text.charAt(valueStart) == '\"' || text.charAt(valueStart) == '\'')) {
                char quote = text.charAt(valueStart);
                int end = quotedEnd(text, valueStart + 1, lineEnd, quote);
                result.append(quote).append("[REDACTED]");
                if (end < lineEnd && text.charAt(end) == quote) {
                    result.append(quote);
                    cursor = end + 1;
                } else {
                    cursor = end;
                }
                continue;
            }

            int valueEnd = findValueEnd(text, valueStart, lineEnd);
            result.append("[REDACTED]");
            cursor = valueEnd;
        }
        result.append(text, cursor, text.length());
        return result.toString();
    }

    private static int quotedEnd(String text, int start, int lineEnd, char quote) {
        for (int index = start; index < lineEnd; index++) {
            char current = text.charAt(index);
            if (current == '\\') {
                index++;
            } else if (current == quote) {
                return index;
            }
        }
        return lineEnd;
    }

    private static int findValueEnd(String text, int start, int lineEnd) {
        int end = start;
        while (end < lineEnd && text.charAt(end) != '&' && text.charAt(end) != ',' && text.charAt(end) != ';') {
            end++;
        }
        Matcher next = NEXT_KEY.matcher(text);
        next.region(start, end);
        if (next.find()) {
            int keyStart = next.start();
            while (keyStart < next.end() && Character.isWhitespace(text.charAt(keyStart))) {
                keyStart++;
            }
            int trimEnd = keyStart;
            while (trimEnd > start && Character.isWhitespace(text.charAt(trimEnd - 1))) {
                trimEnd--;
            }
            return trimEnd;
        }
        return end;
    }

    private static boolean isValidCpf(String candidate) {
        String digits = candidate.replaceAll("[^0-9]", "");
        if (digits.length() != 11 || digits.chars().distinct().count() == 1) {
            return false;
        }
        int first = cpfDigit(digits, 9, 10);
        int second = cpfDigit(digits, 10, 11);
        return digits.charAt(9) - '0' == first && digits.charAt(10) - '0' == second;
    }

    private static int cpfDigit(String digits, int length, int weightStart) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (digits.charAt(i) - '0') * (weightStart - i);
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    /** Recursively sanitizes structured event data before it is persisted or returned. */
    public static Map<String, Object> redactMap(Map<String, Object> values) {
        if (values == null) {
            return null;
        }
        var result = new LinkedHashMap<String, Object>();
        values.forEach((key, value) -> result.put(key, isSecretKey(key) ? "[REDACTED]" : redactValue(value)));
        return result;
    }

    private static Object redactValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            var nested = new LinkedHashMap<String, Object>();
            map.forEach((key, nestedValue) -> {
                String name = String.valueOf(key);
                nested.put(name, isSecretKey(name) ? "[REDACTED]" : redactValue(nestedValue));
            });
            return nested;
        }
        if (value instanceof Collection<?> collection) {
            var sanitized = new ArrayList<>(collection.size());
            collection.forEach(item -> sanitized.add(redactValue(item)));
            return sanitized;
        }
        if (value instanceof String text) {
            return redactText(text);
        }
        return value;
    }

    private static boolean isSecretKey(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.toLowerCase(Locale.ROOT).replaceAll("[_-]", "");
        return SECRET_KEY_NAMES.contains(normalized) || normalized.contains("password")
                || normalized.contains("senha") || normalized.endsWith("token")
                || normalized.contains("secret") || normalized.contains("apikey")
                || normalized.contains("credentialhash");
    }
}
