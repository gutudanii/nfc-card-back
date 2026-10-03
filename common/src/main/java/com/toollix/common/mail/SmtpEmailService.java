package com.toollix.common.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
@ConditionalOnProperty(prefix = "mail", name = "enabled", havingValue = "true")
public class SmtpEmailService implements EmailService {
    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final String frontendUrl;

    public SmtpEmailService(
            JavaMailSender mailSender,
            @Value("${mail.from:contact.toollix@gmail.com}") String from,
            @Value("${mail.frontend-url:http://localhost:3000}") String frontendUrl) {
        this.mailSender = mailSender;
        this.from = from;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void sendVerification(String toEmail, String verificationToken) {
        String link = frontendUrl + "/verify?token=" + verificationToken;
        String subject = "Verify your Toollix Account";
        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin: 0; padding: 0; background-color: #F8FAFC; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                    <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color: #F8FAFC; padding: 40px 20px;">
                        <tr>
                            <td align="center">
                                <table width="100%%" max-width="600" border="0" cellspacing="0" cellpadding="0" style="max-width: 600px; background-color: #FFFFFF; border-radius: 16px; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -2px rgba(0, 0, 0, 0.05); overflow: hidden; border: 1px solid #E2E8F0;">

                                    <!-- Header -->
                                    <tr>
                                        <td align="center" style="padding: 40px 40px 30px; background-color: #FFFFFF; border-bottom: 1px solid #F1F5F9;">
                                            <div style="width: 48px; height: 48px; background-color: #EF4444; border-radius: 12px; display: inline-block; margin-bottom: 16px;"></div>
                                            <h1 style="margin: 0; font-size: 24px; font-weight: 800; color: #0F172A; letter-spacing: -0.5px;">Welcome to Toollix</h1>
                                        </td>
                                    </tr>

                                    <!-- Body -->
                                    <tr>
                                        <td style="padding: 40px;">
                                            <h2 style="margin: 0 0 16px; font-size: 18px; font-weight: 700; color: #0F172A;">Let's get started.</h2>
                                            <p style="margin: 0 0 24px; font-size: 16px; line-height: 24px; color: #475569;">
                                                Thank you for creating an account with Toollix, the premier NFC management platform. To get started securely, we just need to verify your email address.
                                            </p>

                                            <!-- CTA Button -->
                                            <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="margin-bottom: 32px;">
                                                <tr>
                                                    <td align="center">
                                                        <a href="%s" target="_blank" style="display: inline-block; padding: 14px 32px; background-color: #0F172A; color: #FFFFFF; font-size: 16px; font-weight: 600; text-decoration: none; border-radius: 9px; box-shadow: 0 2px 4px rgba(15, 23, 42, 0.2);">
                                                            Verify Email Address
                                                        </a>
                                                    </td>
                                                </tr>
                                            </table>

                                            <div style="background-color: #F8FAFC; padding: 20px; border-radius: 8px; margin-bottom: 24px;">
                                                <p style="margin: 0 0 8px; font-size: 13px; font-weight: 600; color: #64748B; text-transform: uppercase; letter-spacing: 0.5px;">Having trouble?</p>
                                                <p style="margin: 0; font-size: 14px; line-height: 20px; color: #475569;">
                                                    Copy and paste this link into your browser:<br>
                                                    <a href="%s" style="color: #3B82F6; text-decoration: none; word-break: break-all;">%s</a>
                                                </p>
                                            </div>

                                            <p style="margin: 0; font-size: 16px; color: #475569;">
                                                Best,<br>
                                                <strong style="color: #0F172A;">The Toollix Team</strong>
                                            </p>
                                        </td>
                                    </tr>

                                    <!-- Footer -->
                                    <tr>
                                        <td align="center" style="padding: 24px 40px; background-color: #F8FAFC; border-top: 1px solid #E2E8F0;">
                                            <p style="margin: 0; font-size: 13px; color: #94A3B8;">
                                                &copy; 2026 Toollix Platform. All rights reserved.<br>
                                                This email was sent to securely verify your account identity.
                                            </p>
                                        </td>
                                    </tr>

                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """
                .formatted(link, link, link);

        sendHtml(toEmail, subject, html);
    }

    @Override
    public void sendOrganizationInvitation(String toEmail, String orgName, String role, String inviteLink) {
        String subject = "Invitation to work together on Toollix";
        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin: 0; padding: 0; background-color: #F8FAFC; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                    <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color: #F8FAFC; padding: 40px 20px;">
                        <tr>
                            <td align="center">
                                <table width="100%%" max-width="600" border="0" cellspacing="0" cellpadding="0" style="max-width: 600px; background-color: #FFFFFF; border-radius: 16px; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -2px rgba(0, 0, 0, 0.05); overflow: hidden; border: 1px solid #E2E8F0;">

                                    <!-- Header -->
                                    <tr>
                                        <td align="center" style="padding: 40px 40px 30px; background-color: #FFFFFF; border-bottom: 1px solid #F1F5F9;">
                                            <div style="background-color: #F1F5F9; border-radius: 12px; display: inline-block; padding: 12px; margin-bottom: 16px;">
                                                <span style="font-size: 24px; opacity: 0.8;">🏢</span>
                                            </div>
                                            <h1 style="margin: 0; font-size: 24px; font-weight: 800; color: #0F172A; letter-spacing: -0.5px;">Workspace Invitation</h1>
                                        </td>
                                    </tr>

                                    <!-- Body -->
                                    <tr>
                                        <td style="padding: 40px;">
                                            <h2 style="margin: 0 0 16px; font-size: 18px; font-weight: 700; color: #0F172A;">You've been invited!</h2>
                                            <p style="margin: 0 0 24px; font-size: 16px; line-height: 24px; color: #475569;">
                                                You have been invited to join the <strong style="color: #0F172A;">%s</strong> workspace on Toollix with the <span style="background-color: #F1F5F9; color: #334155; padding: 2px 6px; border-radius: 4px; font-size: 14px; font-weight: 600;">%s</span> role.
                                            </p>

                                            <!-- CTA Button -->
                                            <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="margin-bottom: 32px;">
                                                <tr>
                                                    <td align="center">
                                                        <a href="%s" target="_blank" style="display: inline-block; padding: 14px 32px; background-color: #3B82F6; color: #FFFFFF; font-size: 16px; font-weight: 600; text-decoration: none; border-radius: 9px; box-shadow: 0 2px 4px rgba(59, 130, 246, 0.25);">
                                                            Accept Invitation
                                                        </a>
                                                    </td>
                                                </tr>
                                            </table>

                                            <div style="background-color: #F8FAFC; padding: 20px; border-radius: 8px; margin-bottom: 24px;">
                                                <p style="margin: 0 0 8px; font-size: 13px; font-weight: 600; color: #64748B; text-transform: uppercase; letter-spacing: 0.5px;">Trouble accepting?</p>
                                                <p style="margin: 0; font-size: 14px; line-height: 20px; color: #475569;">
                                                    Copy and paste this secure link into your browser:<br>
                                                    <a href="%s" style="color: #3B82F6; text-decoration: none; word-break: break-all;">%s</a>
                                                </p>
                                            </div>

                                            <p style="margin: 0; font-size: 16px; color: #475569;">
                                                Welcome aboard,<br>
                                                <strong style="color: #0F172A;">The Toollix Team</strong>
                                            </p>
                                        </td>
                                    </tr>

                                    <!-- Footer -->
                                    <tr>
                                        <td align="center" style="padding: 24px 40px; background-color: #F8FAFC; border-top: 1px solid #E2E8F0;">
                                            <p style="margin: 0; font-size: 13px; color: #94A3B8;">
                                                &copy; 2026 Toollix Platform. All rights reserved.<br>
                                                If you are not expecting this invitation, you can safely ignore this email.
                                            </p>
                                        </td>
                                    </tr>

                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """
                .formatted(orgName, role, inviteLink, inviteLink, inviteLink);

        sendHtml(toEmail, subject, html);
    }

    private void sendHtml(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(from, "Cards By Toollix");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("[smtp-email] Sent nicely formatted HTML email to {} (subject: {})", to, subject);
        } catch (MessagingException e) {
            log.error("[smtp-email] Failed to send email to {}", to, e);
        } catch (UnsupportedEncodingException e) {

        }
    }

    @Override
    public void sendPasswordReset(String toEmail, String resetToken) {
        String link = frontendUrl + "/reset-password?token=" + resetToken;
        String subject = "Reset your Toollix Password";
        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin: 0; padding: 0; background-color: #F8FAFC; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                    <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color: #F8FAFC; padding: 40px 20px;">
                        <tr>
                            <td align="center">
                                <table width="100%%" max-width="600" border="0" cellspacing="0" cellpadding="0" style="max-width: 600px; background-color: #FFFFFF; border-radius: 16px; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -2px rgba(0, 0, 0, 0.05); overflow: hidden; border: 1px solid #E2E8F0;">

                                    <!-- Header -->
                                    <tr>
                                        <td align="center" style="padding: 40px 40px 30px; background-color: #FFFFFF; border-bottom: 1px solid #F1F5F9;">
                                            <div style="background-color: #F1F5F9; border-radius: 12px; display: inline-block; padding: 12px; margin-bottom: 16px;">
                                                <span style="font-size: 24px; opacity: 0.8;">🔒</span>
                                            </div>
                                            <h1 style="margin: 0; font-size: 24px; font-weight: 800; color: #0F172A; letter-spacing: -0.5px;">Password Reset</h1>
                                        </td>
                                    </tr>

                                    <!-- Body -->
                                    <tr>
                                        <td style="padding: 40px;">
                                            <h2 style="margin: 0 0 16px; font-size: 18px; font-weight: 700; color: #0F172A;">Need a new password?</h2>
                                            <p style="margin: 0 0 24px; font-size: 16px; line-height: 24px; color: #475569;">
                                                We received a request to reset the password for your Toollix account. If you made this request, please click the button below to choose a new password.
                                            </p>

                                            <!-- CTA Button -->
                                            <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="margin-bottom: 32px;">
                                                <tr>
                                                    <td align="center">
                                                        <a href="%s" target="_blank" style="display: inline-block; padding: 14px 32px; background-color: #0F172A; color: #FFFFFF; font-size: 16px; font-weight: 600; text-decoration: none; border-radius: 9px; box-shadow: 0 2px 4px rgba(15, 23, 42, 0.25);">
                                                            Reset Password
                                                        </a>
                                                    </td>
                                                </tr>
                                            </table>

                                            <p style="margin: 0 0 24px; font-size: 14px; line-height: 21px; color: #64748B;">
                                                <strong style="color: #475569;">Didn't request this?</strong> If you didn't ask to reset your password, you can safely ignore this email. Your account remains completely secure.
                                            </p>

                                            <div style="background-color: #F8FAFC; padding: 20px; border-radius: 8px; margin-bottom: 24px;">
                                                <p style="margin: 0 0 8px; font-size: 13px; font-weight: 600; color: #64748B; text-transform: uppercase; letter-spacing: 0.5px;">Having trouble?</p>
                                                <p style="margin: 0; font-size: 14px; line-height: 20px; color: #475569;">
                                                    Copy and paste this secure link into your browser:<br>
                                                    <a href="%s" style="color: #3B82F6; text-decoration: none; word-break: break-all;">%s</a>
                                                </p>
                                            </div>
                                        </td>
                                    </tr>

                                    <!-- Footer -->
                                    <tr>
                                        <td align="center" style="padding: 24px 40px; background-color: #F8FAFC; border-top: 1px solid #E2E8F0;">
                                            <p style="margin: 0; font-size: 13px; color: #94A3B8;">
                                                &copy; 2026 Toollix Platform. All rights reserved.<br>
                                                This is a mandatory service email regarding your account security.
                                            </p>
                                        </td>
                                    </tr>

                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """
                .formatted(link, link, link);

        sendHtml(toEmail, subject, html);
    }
}
