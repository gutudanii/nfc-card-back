package com.toollix.common.mail;

public interface EmailService {
    void sendVerification(String toEmail, String verificationToken);
<<<<<<< HEAD
=======

    void sendOrganizationInvitation(String toEmail, String orgName, String role, String inviteLink);

    void sendPasswordReset(String toEmail, String resetToken);
>>>>>>> 82aa1f0 (Initial commit)
}
