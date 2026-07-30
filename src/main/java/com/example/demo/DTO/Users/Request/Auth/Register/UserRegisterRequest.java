package com.example.demo.DTO.Users.Request.Auth.Register;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public record UserRegisterRequest(
        @NotBlank(message = "아이디는 필수입니다")
        @Pattern(regexp = "^[a-z0-9A-Z]{6,15}$", message = "아이디 형식이 맞지 않습니다.")
        String username,

        @NotBlank(message = "닉네임은 필수입니다")
        @Size(min = 2, max = 10, message = "닉네임은 2~10자여야 합니다.")
        String nickname,

        @NotBlank(message = "비밀번호는 필수입니다")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[~!@#$%^&*()+|=]).{8,16}$")
        String password,

        String password2,

        @Email(message = "이메일 형식이 아닙니다.")
        String email,

        MultipartFile profileImage,

        @NotBlank(message = "인증번호를 입력해주세요.")
        String inputCode
) {}