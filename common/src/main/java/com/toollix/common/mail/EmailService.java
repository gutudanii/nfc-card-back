package com.toollix.common.mail;

public interface EmailService {
    void sendVerification(String toEmail, String verificationToken);

    void sendOrganizationInvitation(String toEmail, String orgName, String role, String inviteLink);

    void sendPasswordReset(String toEmail, String resetToken);
}
