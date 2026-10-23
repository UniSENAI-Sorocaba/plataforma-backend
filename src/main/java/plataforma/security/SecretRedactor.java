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

    private static final String SECRET_KEYS = "password|senha|token|access[_-]?token|refresh[_-]?token|"
            + "authorization|secret|api[_-]?key|cookie";
    private static final Pattern KEY_VALUE = Pattern.compile(
            "(?i)([\\\"']?(?:" + SECRET_KEYS + ")[\\\"']?\\s*[:=]\\s*)(\\\"(?:\\\\.|[^\\\"\\\\])*\\\"|"
                    + "'(?:\\\\.|[^'\\\\])*'|[^,\\s;&}\\]]+)");
    private static final Pattern BEARER = Pattern.compile("(?i)(\\bBearer\\s+)[A-Za-z0-9._~+/-]+=*");
    private static final Set<String> SECRET_KEY_NAMES = Set.of(
            "password", "senha", "token", "accesstoken", "refreshtoken", "authorization", "secret", "apikey",
            "cookie");

    private SecretRedactor() {
    }

    /** Masks key/value and bearer-token forms without including the original value in output. */
    public static String redactText(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        String bearerRedacted = BEARER.matcher(value).replaceAll("$1[REDACTED]");
        var keyed = KEY_VALUE.matcher(bearerRedacted);
        var result = new StringBuffer();
        while (keyed.find()) {
            String secretValue = keyed.group(2);
            String quote = secretValue.startsWith("\"") ? "\"" : secretValue.startsWith("'") ? "'" : "";
            keyed.appendReplacement(result, Matcher.quoteReplacement(
                    keyed.group(1) + quote + "[REDACTED]" + quote));
        }
        keyed.appendTail(result);
        return result.toString();
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
