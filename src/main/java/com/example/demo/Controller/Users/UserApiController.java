package com.example.demo.Controller.Users;


import com.example.demo.DTO.CommonResponse;
import com.example.demo.DTO.Users.Request.Auth.*;
import com.example.demo.DTO.Users.Request.Auth.Register.*;
import com.example.demo.DTO.Users.Request.FindPasswordRequest;
import com.example.demo.DTO.Users.Request.FindUsernameRequest;
import com.example.demo.DTO.Users.Request.Info.PasswordUpdateExRequest;
import com.example.demo.DTO.Users.Request.Info.ProfileUpdateRequest;
import com.example.demo.DTO.Users.Request.WithdrawRequest;
import com.example.demo.DTO.Users.Response.LoginResponseDto;
import com.example.demo.DTO.Users.Response.MyInfoResponseDTO;
import com.example.demo.DTO.Users.Response.UserAccountCheckResponse;
import com.example.demo.DTO.Users.Response.UserDetailResponse;
import com.example.demo.Security.JWT.JwtProvider;
import com.example.demo.Security.JWT.JwtToken;
import com.example.demo.Service.Users.UserCommandService;
import com.example.demo.Service.Users.UserReadService;
import com.example.demo.Util.CookieUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Slf4j
public class UserApiController {

    private final UserCommandService userCommandService;
    private final UserReadService userReadService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CookieUtil cookieUtil;


    /*
     아이디 중복체크
    */
    @PostMapping("/register/check-duplicate-id")
    //
    public ResponseEntity<CommonResponse<?>> checkDuplicateId(@RequestBody UsernameCheckRequest request) {

        // 1. 서비스에 검증 위임
        userReadService.validateDuplicateUsername(request.username());

        // 2. 예외 없이 무사히 통과했다면 사용 가능
        return ResponseEntity.ok(new CommonResponse<>(true, "사용 가능한 아이디입니다.", null));
    }

    /*
     닉네임 중복 체크
    */
    @PostMapping("/register/check-duplicate-nickname")
    public ResponseEntity<CommonResponse<?>> checkDuplicateNickname(@Valid @RequestBody NicknameCheckRequest nicknameCheckRequest) {

        // 1. 서비스에 검증 위임
        userReadService.validateDuplicateNickname(nicknameCheckRequest.nickname());

        // 2. 예외 없이 통과했다면 사용 가능
        return ResponseEntity.ok(new CommonResponse<>(true, "사용 가능한 닉네임입니다.", null));
    }

    /*
    이메일 중복체크
    */
    @PostMapping("/register/mail-auth")
    public ResponseEntity<CommonResponse<?>> mailAuth(@Valid @RequestBody EmailSendRequest request) {

        // 1. 이메일 중복 검증
        userReadService.validateDuplicateEmail(request.getEmail());

        // 2. 검증 통과 시 바로 인증 메일 발송 로직 수행
        userCommandService.sendVerificationEmail(request.getEmail());

        // 3. 성공 응답 반환
        return ResponseEntity.ok(new CommonResponse<>(true, "인증 번호가 발송되었습니다.", null));
    }

    @PostMapping("/register/mail-verify")
    public ResponseEntity<CommonResponse<?>> verifyMail(@Valid @RequestBody EmailVerifyRequest request) {

      userCommandService.verify(
                request.getEmail(),
                request.getCode());

        return ResponseEntity.ok(new CommonResponse<>(true, "인증 번호가 일치합니다.", null));
    }

    // 회원가입 완료
    @PostMapping("/register")
    public ResponseEntity<CommonResponse<?>> register(@ModelAttribute @Valid UserRegisterRequest dto) {

        userCommandService.register(dto);

        return ResponseEntity.ok(new CommonResponse<>(true, "회원가입 성공", null));
    }

    @GetMapping("/me")
    public ResponseEntity<CommonResponse<MyInfoResponseDTO>> me(@AuthenticationPrincipal UserDetails userDetails) {


        if (userDetails == null) {
            return ResponseEntity.ok(new CommonResponse<>(false, "비로그인 상태입니다.", null));
        }

        UserDetailResponse users =
                userReadService.getUserResponseDTO(userDetails.getUsername());

        MyInfoResponseDTO myInfo = new MyInfoResponseDTO(users.nickname(), users.profileUrl(), users.userRole().name());

        return ResponseEntity.ok(new CommonResponse<>(true, "조회 성공", myInfo));
    }

    @PostMapping("/login")
    public ResponseEntity<CommonResponse<?>> login(@RequestBody @Valid LoginRequest request, HttpServletResponse response) {

        // 1. 기존 쿠키 청소
        cookieUtil.clearTokenCookies(response);

        // 2. 서비스를 통해 로그인 로직 수행 및 쿠키/닉네임 획득
        LoginResponseDto loginResult = userCommandService.login(request);

        // 3. 응답 헤더에 쿠키 추가 (컨트롤러 고유의 역할)
        response.addHeader("Set-Cookie", loginResult.accessCookie().toString());
        response.addHeader("Set-Cookie", loginResult.refreshCookie().toString());

        // 4. 깔끔한 성공 반환
        return ResponseEntity.ok(new CommonResponse<>(true, "로그인 성공",
                Map.of("nickname", loginResult.nickname())
        ));
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<CommonResponse<?>> logout(HttpServletRequest request, HttpServletResponse response) {

        userCommandService.logout(request);
        cookieUtil.clearTokenCookies(response);

        return ResponseEntity.ok(new CommonResponse<>(true, "로그아웃 성공", null));
    }

    @PutMapping("/update")
    public ResponseEntity<CommonResponse<?>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute ProfileUpdateRequest request) { // DTO 사용

        userCommandService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(new CommonResponse<>(true, "회원정보가 수정되었습니다.", null));
    }

    @PutMapping("/update-password")
    public ResponseEntity<CommonResponse<?>> updatePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PasswordUpdateExRequest passwordUpdateExRequest
            ,HttpServletResponse response)
    {
        userCommandService.updatePassword(userDetails.getUsername(), passwordUpdateExRequest);

        //  브라우저 쿠키 삭제 (로그아웃 처리)
        cookieUtil.clearTokenCookies(response);

        //  Redis에 저장된 리프레시 토큰이 있다면 삭제
        redisTemplate.delete(userDetails.getUsername());

        return ResponseEntity.ok(new CommonResponse<>(true, "비밀번호가 변경되었습니다.", null));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<CommonResponse<?>> deleteUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody WithdrawRequest request, // 비밀번호 Body로 수신
            HttpServletResponse response) {

        userCommandService.softDeleteUser(userDetails.getUsername(), request.password());
        cookieUtil.clearTokenCookies(response);
        return ResponseEntity.ok(new CommonResponse<>(true, "회원 탈퇴가 완료되었습니다.", null));
    }


    // 아이디 찾기 메일 전송
    @PostMapping("/find-username")
    public ResponseEntity<CommonResponse<Void>> findUsername(@Valid @RequestBody FindUsernameRequest request) {

        userCommandService.sendFindUsernameEmail(request.email());

        return ResponseEntity.ok(
                new CommonResponse<>(true, "아이디 안내 메일이 성공적으로 발송되었습니다.", null)
        );
    }

    // 비밀번호 찾기 (임시 비밀번호 발송)
    @PostMapping("/find-password")
    public ResponseEntity<CommonResponse<Void>> findPassword(@Valid @RequestBody FindPasswordRequest request) {
        userCommandService.sendFindPasswordEmail(request.email(), request.username());

        return ResponseEntity.ok(
                new CommonResponse<>(true, "임시 비밀번호가 이메일로 발송되었습니다.", null)
        );
    }
}

