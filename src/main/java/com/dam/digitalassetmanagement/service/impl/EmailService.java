package com.dam.digitalassetmanagement.service.impl;

import com.dam.digitalassetmanagement.exception.CustomExceptions;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendOtpEmail(String toEmail, String otpCode, String username) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Password Reset OTP - DAM System");

            String emailContent = buildOtpEmailContent(username, otpCode);
            helper.setText(emailContent, true);

            mailSender.send(message);
            log.info("OTP email sent successfully to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send OTP email to: {}", toEmail, e);
            throw new CustomExceptions.BadRequestException("Failed to send OTP email. Please try again later.");
        }
    }

    private String buildOtpEmailContent(String username, String otpCode) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            line-height: 1.6;
                            color: #333;
                            max-width: 600px;
                            margin: 0 auto;
                            padding: 20px;
                        }
                        .container {
                            background-color: #f9f9f9;
                            border-radius: 10px;
                            padding: 30px;
                            box-shadow: 0 2px 5px rgba(0,0,0,0.1);
                        }
                        .header {
                            text-align: center;
                            color: #4CAF50;
                            margin-bottom: 30px;
                        }
                        .otp-box {
                            background-color: #4CAF50;
                            color: white;
                            font-size: 32px;
                            font-weight: bold;
                            text-align: center;
                            padding: 20px;
                            border-radius: 8px;
                            letter-spacing: 8px;
                            margin: 20px 0;
                        }
                        .info {
                            background-color: #fff3cd;
                            border-left: 4px solid #ffc107;
                            padding: 15px;
                            margin: 20px 0;
                            border-radius: 4px;
                        }
                        .footer {
                            margin-top: 30px;
                            text-align: center;
                            color: #666;
                            font-size: 12px;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🔐 Password Reset Request</h1>
                        </div>
                        
                        <p>Hello <strong>%s</strong>,</p>
                        
                        <p>We received a request to reset your password for your DAM System account. Use the OTP code below to reset your password:</p>
                        
                        <div class="otp-box">%s</div>
                        
                        <div class="info">
                            <strong>⚠️ Important:</strong>
                            <ul>
                                <li>This OTP is valid for <strong>10 minutes</strong></li>
                                <li>Do not share this code with anyone</li>
                                <li>If you didn't request this, please ignore this email</li>
                            </ul>
                        </div>
                        
                        <p>If you're having trouble, please contact our support team.</p>
                        
                        <div class="footer">
                            <p>© 2024 Digital Asset Management System</p>
                            <p>This is an automated email. Please do not reply.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(username, otpCode);
    }

    public void sendPasswordChangedNotification(String toEmail, String username) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Password Changed Successfully - DAM System");

            String emailContent = buildPasswordChangedContent(username);
            helper.setText(emailContent, true);

            mailSender.send(message);
            log.info("Password changed notification sent to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send password changed notification to: {}", toEmail, e);
            // Don't throw exception here, it's just a notification
        }
    }

    private String buildPasswordChangedContent(String username) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            line-height: 1.6;
                            color: #333;
                            max-width: 600px;
                            margin: 0 auto;
                            padding: 20px;
                        }
                        .container {
                            background-color: #f9f9f9;
                            border-radius: 10px;
                            padding: 30px;
                            box-shadow: 0 2px 5px rgba(0,0,0,0.1);
                        }
                        .header {
                            text-align: center;
                            color: #4CAF50;
                            margin-bottom: 30px;
                        }
                        .success-box {
                            background-color: #d4edda;
                            border-left: 4px solid #28a745;
                            padding: 15px;
                            margin: 20px 0;
                            border-radius: 4px;
                        }
                        .footer {
                            margin-top: 30px;
                            text-align: center;
                            color: #666;
                            font-size: 12px;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>✅ Password Changed Successfully</h1>
                        </div>
                        
                        <p>Hello <strong>%s</strong>,</p>
                        
                        <div class="success-box">
                            Your password has been successfully changed for your DAM System account.
                        </div>
                        
                        <p>If you did not make this change, please contact our support team immediately.</p>
                        
                        <div class="footer">
                            <p>© 2024 Digital Asset Management System</p>
                            <p>This is an automated email. Please do not reply.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(username);
    }
}