package com.example.demo.Config;

import com.example.demo.Error.BusinessException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MailSenderUtil {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String adminAddress;

    /**
     * 공통 메일 발송 헬퍼 메서드
     */
    private void sendMail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            message.setFrom(adminAddress);
            message.setRecipients(MimeMessage.RecipientType.TO, to);
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("메일 발송 실패 - 수신자: {}, 제목: {}", to, subject, e);
            throw new BusinessException("메일 발송 중 오류가 발생했습니다.");
        }
    }

    // 1. 이메일 인증번호 발송
    @Async
    public void sendVerificationEmail(String email, int code) {
        String subject = "이메일 인증 번호 안내";
        String content = "이메일 인증번호는 [" + code + "] 입니다.";
        sendMail(email, subject, content);
    }

    // 2. 아이디 찾기 발송
    @Async
    public void sendUsernameEmail(String email, String username) {
        String subject = email + "님의 아이디 정보 안내 메일입니다.";
        String content = "안녕하세요. 요청하신 아이디는 [" + username + "] 입니다.";
        sendMail(email, subject, content);
    }

    // 3. 임시 비밀번호 발송
    @Async
    public void sendTempPasswordEmail(String email, String tempPassword) {
        String subject = email + "님의 임시 비밀번호 안내 메일입니다.";
        String content = "안녕하세요. 임시 비밀번호는 [" + tempPassword + "] 입니다.\n로그인 후 즉시 비밀번호를 변경해 주세요.";
        sendMail(email, subject, content);
    }

    // 4. 블랙리스트 처리 안내
    @Async
    public void sendBlackUserNotice(String email) {
        String subject = "계정이 이용 정지(블랙회원) 처리되었습니다.";
        String content = "관리자의 판단으로 계정이 블랙처리 되었습니다.\n이의 제기가 있으시면 본 이메일로 답장 부탁드립니다.";
        sendMail(email, subject, content);
    }

    // 5. 일반회원 복원 안내
    @Async
    public void sendRestoreNotice(String email) {
        String subject = "일반회원으로 계정이 복원되었습니다.";
        String content = "계정이 정상적으로 일반 회원으로 복원되었습니다.\n건강한 커뮤니티 문화를 만들어 가도록 협조 부탁드립니다.";
        sendMail(email, subject, content);
    }

    // 6. 탈퇴 계정 복구 완료 안내
    @Async
    public void sendRestoreDeletedUserNotice(String email) {
        String subject = "요청하신 계정 복구가 완료되었습니다.";
        String content = "요청하신 회원 계정이 정상적으로 복원되었습니다.";
        sendMail(email, subject, content);
    }
}