package com.empresa.platform.logging.converter;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MaskingConverter extends MessageConverter {

    // Regex para CPF (com ou sem pontuação)
    private static final Pattern CPF_PATTERN = Pattern.compile("\\b\\d{3}[.-]?\\d{3}[.-]?\\d{3}[.-]?\\d{2}\\b");
    
    // Regex para Cartão de Crédito (16 dígitos contínuos ou separados)
    private static final Pattern CARD_PATTERN = Pattern.compile("\\b\\d{4}[ -]?\\d{4}[ -]?\\d{4}[ -]?\\d{4}\\b");
    
    // Regex para E-mail estruturado
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");

    @Override
    public String convert(ILoggingEvent event) {
        String originalMessage = super.convert(event);
        if (originalMessage == null || originalMessage.isBlank()) {
            return originalMessage;
        }

        // Aplica as máscaras em cascata de forma limpa
        String maskedMessage = maskCpf(originalMessage);
        maskedMessage = maskCard(maskedMessage);
        maskedMessage = maskEmail(maskedMessage);

        return maskedMessage;
    }

    private String maskCpf(String text) {
        Matcher matcher = CPF_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "***.***.***-**");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String maskCard(String text) {
        Matcher matcher = CARD_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "****-****-****-****");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String maskEmail(String text) {
        Matcher matcher = EMAIL_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String email = matcher.group();
            int atIndex = email.indexOf('@');
            if (atIndex > 1) {
                // Ofusca mantendo apenas a primeira letra e o domínio visíveis (ex: b****@empresa.com)
                String masked = email.charAt(0) + "****" + email.substring(atIndex);
                matcher.appendReplacement(sb, masked);
            } else {
                matcher.appendReplacement(sb, "****@****.***");
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
