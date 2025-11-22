package com.stitch.app.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessagePreparator;

import jakarta.mail.internet.MimeMessage;
import java.io.InputStream;
import java.util.Properties;

@Configuration
public class MailConfig {

    private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

    @Autowired(required = false)
    private MailProperties mailProperties;

    @Bean
    public JavaMailSender javaMailSender() {
        MailProperties mp = this.mailProperties;
        // If mail host is not configured, return a no-op sender to avoid application startup failure
        if (mp == null || mp.getHost() == null || mp.getHost().isEmpty()) {
            log.info("Mail host not configured - creating no-op JavaMailSender");
            return new NoOpJavaMailSender();
        }

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mp.getHost());
        mailSender.setPort(mp.getPort());
        mailSender.setUsername(mp.getUsername());
        mailSender.setPassword(mp.getPassword());

        Properties props = mailSender.getJavaMailProperties();
        props.putAll(mp.getProperties());
        return mailSender;
    }

    static class NoOpJavaMailSender extends JavaMailSenderImpl {
        @Override
        public void send(@NonNull org.springframework.mail.SimpleMailMessage simpleMessage) {
            // no-op
        }

        @Override
        public void send(@NonNull org.springframework.mail.SimpleMailMessage... simpleMessages) {
            // no-op
        }

        @Override
        @NonNull
        public MimeMessage createMimeMessage() {
            return super.createMimeMessage();
        }

        @Override
        @NonNull
        public MimeMessage createMimeMessage(@NonNull InputStream contentStream) {
            return super.createMimeMessage(contentStream);
        }

        @Override
        public void send(@NonNull MimeMessage mimeMessage) {
            // no-op
        }

        @Override
        public void send(@NonNull MimeMessage... mimeMessages) {
            // no-op
        }

        @Override
        public void send(@NonNull MimeMessagePreparator mimeMessagePreparator) {
            // no-op
        }

        @Override
        public void send(@NonNull MimeMessagePreparator... mimeMessagePreparators) {
            // no-op
        }
    }
}
