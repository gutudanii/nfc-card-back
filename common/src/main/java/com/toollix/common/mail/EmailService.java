package com.toollix.common.mail;

public interface EmailService {
    void sendVerification(String toEmail, String verificationToken);
}
