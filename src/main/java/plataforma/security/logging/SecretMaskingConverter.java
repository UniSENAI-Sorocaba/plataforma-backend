package plataforma.security.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.classic.pattern.ClassicConverter;
import plataforma.security.SecretRedactor;

/** Logback converter that sanitizes the rendered message and full throwable stack trace. */
public final class SecretMaskingConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        String message = SecretRedactor.redactText(event.getFormattedMessage());
        var throwable = event.getThrowableProxy();
        if (throwable == null) {
            return message;
        }
        var safeStack = new StringBuilder();
        appendThrowable(throwable, "", safeStack);
        return message + System.lineSeparator() + safeStack;
    }

    private void appendThrowable(IThrowableProxy throwable, String prefix, StringBuilder output) {
        output.append(prefix).append(throwable.getClassName()).append(" [exception message omitted]")
                .append(System.lineSeparator());
        if (throwable.isCyclic()) {
            output.append("[CIRCULAR REFERENCE]").append(System.lineSeparator());
            return;
        }
        for (StackTraceElementProxy frame : throwable.getStackTraceElementProxyArray()) {
            output.append("\tat ").append(frame.getSTEAsString()).append(System.lineSeparator());
        }
        for (IThrowableProxy suppressed : throwable.getSuppressed()) {
            appendThrowable(suppressed, "Suppressed: ", output);
        }
        if (throwable.getCause() != null) {
            appendThrowable(throwable.getCause(), "Caused by: ", output);
        }
    }
}
