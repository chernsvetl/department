package com.backend.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    /** Mock-режим: письма не отправляются, содержимое печатается в лог (для локального запуска без SMTP). */
    private final boolean mock;

    public EmailService(JavaMailSender mailSender,
                        @Value("${auth.mail.mock:true}") boolean mock) {
        this.mailSender = mailSender;
        this.mock = mock;
    }

    public void sendVerificationCode(String to, String code) {
        if (mock) {
            log.info("[mock-mail] Код подтверждения для {}: {}", to, code);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Код подтверждения — Система генерации документов");
        message.setText("""
                Ваш код подтверждения: %s

                Код действует 10 минут.
                Если вы не запрашивали код — проигнорируйте это письмо.
                """.formatted(code));
        mailSender.send(message);
    }

    /**
     * Отправляет ZIP-архив с заполненными документами студенту.
     */
    public void sendDocuments(String to, byte[] zipBytes) throws Exception {
        if (mock) {
            log.info("[mock-mail] Документы ({} байт) для {} не отправлены — mock-режим", zipBytes.length, to);
            return;
        }
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
        helper.setTo(to);
        helper.setSubject("Ваши документы для практики");
        helper.setText("""
                Здравствуйте!

                Ваша заявка принята. Во вложении — пакет документов для прохождения практики.

                Если у вас есть вопросы, обратитесь на кафедру.
                """);
        helper.addAttachment("documents.zip", new ByteArrayResource(zipBytes), "application/zip");
        mailSender.send(msg);
    }

    /**
     * Отправляет напоминание студенту.
     */
    public void sendReminder(String to) {
        if (mock) {
            log.info("[mock-mail] Напоминание для {} не отправлено — mock-режим", to);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Напоминание — документы для практики");
        message.setText("""
                Здравствуйте!

                Напоминаем: вы подали заявку на прохождение практики.
                Пожалуйста, проверьте полученные документы и при необходимости обратитесь на кафедру.
                """);
        mailSender.send(message);
    }
}
