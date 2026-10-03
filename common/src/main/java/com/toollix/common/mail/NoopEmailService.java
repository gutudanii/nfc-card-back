package com.toollix.common.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "mail", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoopEmailService implements EmailService {
    private final Logger log = LoggerFactory.getLogger(NoopEmailService.class);

    @Override
    public void sendVerification(String toEmail, String verificationToken) {
        log.info("[noop-email] verification for {} -> token={} (copy to console in dev)", toEmail, verificationToken);
    }
<<<<<<< HEAD
=======

    @Override
    public void sendOrganizationInvitation(String toEmail, String orgName, String role, String inviteLink) {
        log.info("[noop-email] invitation to {} for org {} as {} -> link={}", toEmail, orgName, role, inviteLink);
    }

    @Override
    public void sendPasswordReset(String toEmail, String resetToken) {
        log.info("[noop-email] password reset for {} -> token={} (copy to console in dev)", toEmail, resetToken);
    }
>>>>>>> 82aa1f0 (Initial commit)
}
