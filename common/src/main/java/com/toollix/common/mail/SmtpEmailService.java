package com.toollix.common.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "mail", name = "enabled", havingValue = "true")
public class SmtpEmailService implements EmailService {
    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);

    private final String from;

    public SmtpEmailService(@Value("${mail.from:no-reply@toollix.app}") String from) {
        this.from = from;
    }

    @Override
    public void sendVerification(String toEmail, String verificationToken) {
        log.info("[smtp-email] from={} to={} verificationToken={}", from, toEmail, verificationToken);
    }
}
