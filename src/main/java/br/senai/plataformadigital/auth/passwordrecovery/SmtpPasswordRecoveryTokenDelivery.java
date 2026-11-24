package br.senai.plataformadigital.auth.passwordrecovery;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Sends recovery links through the configured SMTP provider without logging the raw token. */
@Component
@ConditionalOnProperty(name = "security.password-recovery.delivery", havingValue = "smtp")
public final class SmtpPasswordRecoveryTokenDelivery implements PasswordRecoveryTokenDelivery {
    private final JavaMailSender mailSender;
    private final String from;
    private final String resetBaseUrl;

    public SmtpPasswordRecoveryTokenDelivery(
            JavaMailSender mailSender,
            @Value("${security.password-recovery.from}") String from,
            @Value("${security.password-recovery.reset-base-url}") String resetBaseUrl) {
        this.mailSender = mailSender;
        this.from = from;
        this.resetBaseUrl = resetBaseUrl;
    }

    @Override
    public void deliver(String normalizedSubject, String rawToken, Instant expiresAt) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(normalizedSubject);
        message.setSubject("Redefinição de senha — Plataforma Digital");
        String link = resetBaseUrl + (resetBaseUrl.contains("?") ? "&" : "?")
                + "token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        message.setText("Use o link para redefinir sua senha: " + link
                + "\n\nO link expira em " + expiresAt + ".");
        mailSender.send(message);
    }
}
