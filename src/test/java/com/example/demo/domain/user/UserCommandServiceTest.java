package com.example.demo.domain.user;

import com.example.demo.Config.FileStorageService;
import com.example.demo.Config.MailSenderUtil;
import com.example.demo.DTO.Users.Request.Auth.LoginRequest;
import com.example.demo.DTO.Users.Request.Auth.Register.UserRegisterRequest;
import com.example.demo.DTO.Users.Request.Info.PasswordUpdateExRequest;
import com.example.demo.DTO.Users.Request.Info.ProfileUpdateRequest;
import com.example.demo.DTO.Users.Response.LoginResponseDto;
import com.example.demo.DTO.Users.Response.UserAccountCheckResponse;
import com.example.demo.Entity.Chat.ChatRoom;
import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Users.Users;
import com.example.demo.Error.BusinessException;
import com.example.demo.Repository.Chat.ChatRoom.ChatRoomRepository;
import com.example.demo.Repository.Users.UserRepository;
import com.example.demo.Security.JWT.JwtProvider;
import com.example.demo.Security.JWT.JwtToken;
import com.example.demo.Service.Chat.ChatRoomCommandService;
import com.example.demo.Service.Users.UserCommandService;
import com.example.demo.Service.Users.UserReadService;
import com.example.demo.Util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.ArgumentMatchers.any;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserCommandServiceTest {

    @InjectMocks // 가짜 객체들을 주입받는 테스트 대상
    private UserCommandService userCommandService;

    @Mock
    private UserReadService userReadService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations; // Redis opsForValue() 모킹용

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MailSenderUtil mailSenderUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private CookieUtil cookieUtil;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomCommandService chatRoomCommandService;




    // 회원가입 공통 DTO
    private UserRegisterRequest createDefaultDto() {
        return new UserRegisterRequest("testuser", "testnick",
                "pass123!", "pass123!", "test@example.com",null);
    }

    // 로그인 공통 DTO
    private LoginRequest loginRequest()
    {
        return new LoginRequest("testuser","1q2w3e4r5t!@",false);
    }

    @Test
    @DisplayName("회원가입 성공 테스트")
    void register_success() {

        // given
        UserRegisterRequest dto = createDefaultDto();

        // Redis 이메일 인증 true 로 설정
        valueOperations = mock(ValueOperations.class); // 가짜 객체값 설정
        given(redisTemplate.opsForValue()).willReturn(valueOperations); // 가짜 객체 Redis에 대입
        given(valueOperations.get("email:verified:" + dto.email())).willReturn("true");

        // 2. 중복 검사 통과
        given(userReadService.existsByUsername(dto.username())).willReturn(false);
        given(userReadService.existsByNickname(dto.nickname())).willReturn(false);
        given(userReadService.existsByEmail(dto.email())).willReturn(false);

        // 3. 패스워드 암호화
        given(passwordEncoder.encode(dto.password())).willReturn("password");

        // when & then
        assertThatCode(() ->userCommandService.register(dto))
                .doesNotThrowAnyException();

        // Then (핵심 행위들이 실제로 일어났는지 검증)
        verify(userRepository, times(1)).save(ArgumentMatchers.<Users>any()); // save()가 1번 호출되었는가?
        verify(redisTemplate, times(1)).delete("email:verified:" + dto.email()); // Redis 삭제가 호출되었는가?
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 미인증 시 예외 발생")
    void register_unverifiedEmail_throwsException() {
        // given
        UserRegisterRequest dto = createDefaultDto();

        valueOperations = mock(ValueOperations.class); // 가짜 객체값 설정
        given(redisTemplate.opsForValue()).willReturn(valueOperations); // 가짜 객체 Redis에 대입
        given(valueOperations.get("email:verified:" + dto.email())).willReturn("false");

        // when & then
        assertThatThrownBy(() -> userCommandService.register(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessage("이메일 인증이 필요합니다.");
    }

    @Test
    @DisplayName("이메일 인증 코드 발송 성공 테스트")
    void sendVerificationEmail_success()
    {
        // given
        String email = createDefaultDto().email();
        String cooldownKey = "email:cooldown:" + email;

        given(redisTemplate.hasKey(cooldownKey)).willReturn(false);

        given(redisTemplate.opsForValue()).willReturn(valueOperations); // 가짜 객체 Redis에 대입

        // when & then
        assertThatCode(() ->userCommandService.sendVerificationEmail(email)).doesNotThrowAnyException();

        // 쿨다운 키 및 인증 코드 키가 Redis에 set 되었는지 확인
        verify(valueOperations, times(1)).set(eq(cooldownKey), eq("true"), eq(Duration.ofSeconds(60)));

        // 실제로 메일 발송 메서드가 1회 호출되었는지 확인
        verify(mailSenderUtil, times(1)).sendVerificationEmail(eq(email), anyInt());
    }

    @Test
    @DisplayName("이메일 인증 코드 발송 실패 - Redis 값이 존재함")
    void sendVerificationEmail_throwsException()
    {
        // given
        String email = createDefaultDto().email();
        String cooldownKey = "email:cooldown:" + email;

        given(redisTemplate.hasKey(cooldownKey)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userCommandService.sendVerificationEmail(email))
                .isInstanceOf(BusinessException.class)
                .hasMessage("잠시 후 다시 시도해주세요.");
    }

    @Test
    @DisplayName("회원가입 인증 통과 테스트")
    void verify_success()
    {
        // given
        String email = createDefaultDto().email();
        String inputNumber = "123456";
        String codeKey = "email:code:" + email;
        String verifiedKey = "email:verified:" + email;

        // 1. opsForValue() 모킹
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // 2. Redis에서 인증번호 조회시 123456이 출력된다고 가정
        given(valueOperations.get(codeKey)).willReturn(inputNumber);

        // when & then
        assertThatCode(() -> userCommandService.verify(email,inputNumber)).doesNotThrowAnyException();

        // 행위 검증
        // 1. 기존 인증 코드 키가 삭제(delete)되었는지 확인
        verify(redisTemplate, times(1)).delete(codeKey);

        // 2. 인증 완료 키가 10분 만료시간으로 저장(set)되었는지 확인
        verify(valueOperations, times(1))
                .set(eq(verifiedKey), eq("true"), eq(Duration.ofMinutes(10)));
    }

    @Test
    @DisplayName("회원가입 인증 통과 실패 - 인증번호의 불일치")
    void verify_Authentication_mismatch()
    {
        // given
        String email = createDefaultDto().email();
        String inputNumber = "123456";
        String codeKey = "email:code:" + email;

        // 1. opsForValue() 모킹
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // 2. Redis에서 인증번호 조회시 1234567이 출력된다고 가정
        given(valueOperations.get(codeKey)).willReturn(inputNumber+"7");

        // when & then
        assertThatThrownBy(() -> userCommandService.verify(email,inputNumber))
                .isInstanceOf(BusinessException.class)
                .hasMessage("인증 번호가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("회원가입 인증 통과 실패 - 인증번호의 불일치")
    void verify_Certification_expires()
    {
        // given
        String email = createDefaultDto().email();
        String inputNumber = "123456";
        String codeKey = "email:code:" + email;

        // 1. opsForValue() 모킹
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // 2. Redis에서 인증번호 조회시 1234567이 출력된다고 가정
        given(valueOperations.get(codeKey)).willReturn(inputNumber+"7");

        // when & then
        assertThatThrownBy(() -> userCommandService.verify(email,inputNumber))
                .isInstanceOf(BusinessException.class)
                .hasMessage("인증 번호가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("회원가입 인증 실패 - 인증번호 null(만료)")
    void 이메일인증_인증번호_Null()
    {
        // given
        String email = createDefaultDto().email();
        String inputNumber = "123456";
        String codeKey = "email:code:" + email;

        // 1. opsForValue() 모킹
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // 2. Redis에서 인증번호 조회시 null이 출력된다고 가정
        given(valueOperations.get(codeKey)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> userCommandService.verify(email,inputNumber))
                .isInstanceOf(BusinessException.class)
                .hasMessage("인증 번호가 일치하지 않습니다.");
    }


    @Test
    void 로그인_성공()
    {
        // given
        LoginRequest request = loginRequest();
        String username = request.username();

        // 1. 시큐리티 인증 모킹
        Authentication authentication = mock(Authentication.class);
        given(authentication.getName()).willReturn(username);

        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willReturn(authentication);

        // 2. 가짜 토큰 생성 및 모킹
        JwtToken mockToken = JwtToken.builder()
                .grantType("Bearer")
                .accessToken("mock-access-token")
                .refreshToken("mock-refresh-token")
                .build();

        // Mocking 설정
        given(jwtProvider.createToken(eq(authentication), eq(request.rememberMe())))
                .willReturn(mockToken);

        // 3. 쿠키 객체 생성
        ResponseCookie mockAccessCookie = ResponseCookie.from("accessToken", "mock-access-token").build();
        ResponseCookie mockRefreshCookie = ResponseCookie.from("refreshToken", "mock-refresh-token").build();

        given(jwtProvider.createAccessCookie(mockToken.getAccessToken()))
                .willReturn(mockAccessCookie);
        given(jwtProvider.createRefreshCookie(mockToken.getRefreshToken(), request.rememberMe()))
                .willReturn(mockRefreshCookie);

        // 4. 유저 데이터 모킹
        Users mockUser = Users.builder()
                .username(username)
                .nickname("테스터")
                .profileUrl("https://example.com/profile.jpg")
                .build();
        given(userReadService.getUserEntity(username)).willReturn(mockUser);

        // when
        LoginResponseDto response = userCommandService.login(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.nickname()).isEqualTo("테스터");
        assertThat(response.profileUrl()).isEqualTo("https://example.com/profile.jpg");
        assertThat(response.accessCookie()).isEqualTo(mockAccessCookie);
        assertThat(response.refreshCookie()).isEqualTo(mockRefreshCookie);

    }

    @Test
    void 로그인_실패_아이디_OR_비밀번호_오류()
    {
        // given
        LoginRequest request = loginRequest();

        // authenticationManager가 시큐리티 인증 실패 예외를 던지도록 설정
        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willThrow(new BadCredentialsException("자격 증명에 실패하였습니다."));

        // when & then
        assertThatThrownBy(() -> userCommandService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("자격 증명에 실패하였습니다.");
    }


    @Test
    void 로그아웃_성공()
    {
        // given
        // 1. 가짜 HttpServletRequest 및 토큰값 준비
        HttpServletRequest request = mock(HttpServletRequest.class);
        String refreshToken = "valid-refresh-token";
        String username = "testuser";

        // 2. CookieUtil 동작 스터빙 (쿠키에서 리프레시 토큰 추출)
        given(cookieUtil.getCookieValue(request, "refreshToken")).willReturn(refreshToken);

        // 3. JwtProvider 동작 스터빙 (토큰 유효성 검증 true 및 username 추출)
        given(jwtProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtProvider.getUsername(refreshToken)).willReturn(username);

        // when
        userCommandService.logout(request);

        // then
        // Redis 에서 해당 유저의 세션 삭제(delete)가 정확히 1번 실행되었는지 검증
        verify(redisTemplate, times(1)).delete(username);
    }

    @Test
    void 로그아웃_실패_토큰없음()
    {
        // given
        // 1. 가짜 HttpServletRequest 준비
        HttpServletRequest request = mock(HttpServletRequest.class);

        // 2. CookeUtil 을 통해 리프레시 토큰이 없음을 확인
        given(cookieUtil.getCookieValue(request, "refreshToken")).willReturn(null);

        // when
        userCommandService.logout(request);

        // then
        // 1. jwtProvider의 토큰 검증이나 유저 추출이 호출되지 않았는지 확인
        verify(jwtProvider, never()).validateToken(anyString());

        // 2. Redis 삭제 동작 역시 단 한 번도 실행되지 않았는지 검증
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void 로그아웃_실패_유효하지_않은_토큰()
    {
        // given
        HttpServletRequest request = mock(HttpServletRequest.class);
        String invalidToken = "invalid-token";

        given(cookieUtil.getCookieValue(request, "refreshToken")).willReturn(invalidToken);

        // 토큰 검증 결과 false 반환
        given(jwtProvider.validateToken(invalidToken)).willReturn(false);

        // when
        userCommandService.logout(request);

        // then
        // 유저 정보 조회나 Redis 삭제가 실행되지 않았는지 확인
        verify(jwtProvider, never()).getUsername(anyString());
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void 로그아웃_실패_Redis오류()
    {
        // given
        HttpServletRequest request = mock(HttpServletRequest.class);
        String refreshToken = "valid-token";
        String username = "testuser";

        // 기존 토큰 등 정상작동
        given(cookieUtil.getCookieValue(request, "refreshToken")).willReturn(refreshToken);
        given(jwtProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtProvider.getUsername(refreshToken)).willReturn(username);

        // Redis delete 시 런타임 예외 발생 스터빙
        given(redisTemplate.delete(username)).willThrow(new RedisConnectionFailureException("Redis 연결 실패"));

        // when & then
        assertThatThrownBy(() -> userCommandService.logout(request))
                .isInstanceOf(RedisConnectionFailureException.class);
    }

    @Test
    void 프로필_수정_성공_닉네임_및_이미지_모두_변경() {
        // given
        String username = "testuser";
        String oldNickname = "구닉네임";
        String newNickname = "신닉네임";
        String oldProfileUrl = "/uploads/profile/custom-image.png";
        String newProfileUrl = "/uploads/profile/new-image.png";

        // 1. 기존 유저 모킹
        Users mockUser = spy(Users.builder()
                .username(username)
                .nickname(oldNickname)
                .profileUrl(oldProfileUrl)
                .build());

        given(userReadService.getUserEntity(username)).willReturn(mockUser);
        given(userReadService.existsByNickname(newNickname)).willReturn(false); // 닉네임 중복 없음

        // 2. 파일 저장 모킹
        MockMultipartFile newImage = new MockMultipartFile(
                "profileImage", "new.png", MediaType.IMAGE_PNG_VALUE, "content".getBytes()
        );

        given(fileStorageService.save(eq(newImage), anyString(), anyString())).willReturn(newProfileUrl);

        ProfileUpdateRequest request = new ProfileUpdateRequest(newNickname, newImage);

        // when
        userCommandService.updateProfile(username, request);

        // then
        // 1. 유저 도메인 상태 변경 확인
        assertThat(mockUser.getNickname()).isEqualTo(newNickname);
        assertThat(mockUser.getProfileUrl()).isEqualTo(newProfileUrl);

        // 2. 외부 저장소 삭제 및 새 저장 로직 호출 검증 (verify)
        verify(fileStorageService, times(1)).delete(eq(oldProfileUrl), anyString(), anyString());
        verify(fileStorageService, times(1)).save(eq(newImage), anyString(), anyString());
    }

    @Test
    void 프로필_닉네임만_수정()
    {
        // given
        String username = "testuser";
        String oldNickname = "구닉네임";
        String newNickname = "신닉네임";
        String defaultProfileUrl = "/uploads/profile/default-profile.png";

        Users mockUser = Users.builder()
                .username(username)
                .nickname(oldNickname)
                .profileUrl(defaultProfileUrl)
                .build();

        given(userReadService.getUserEntity(username)).willReturn(mockUser);
        given(userReadService.existsByNickname(newNickname)).willReturn(false); // 닉네임 중복 없음

        ProfileUpdateRequest request = new ProfileUpdateRequest(newNickname,null);

        // when
        userCommandService.updateProfile(username, request);

        // then
        // 1. 닉네임 변경 확인 및 프로필 이미지 그대로인지
        assertThat(mockUser.getNickname()).isEqualTo(newNickname);
        assertThat(mockUser.getProfileUrl()).isEqualTo(defaultProfileUrl);

        // 2. 외부 저장소 메서드가 전혀 호출되지 않았음을 명시적으로 검증
        verify(fileStorageService, never()).delete(anyString(), anyString(), anyString());
        verify(fileStorageService, never()).save(any(), anyString(), anyString());
    }

    @Test
    void 프로필_수정_실패_이미_사용중인_닉네임() {
        // given
        String username = "testuser";
        String duplicateNickname = "중복닉네임";

        Users mockUser = Users.builder()
                .username(username)
                .nickname("기존닉네임")
                .build();

        given(userReadService.getUserEntity(username)).willReturn(mockUser);
        given(userReadService.existsByNickname(duplicateNickname)).willReturn(true); // 이미 존재함

        MockMultipartFile newImage = new MockMultipartFile(
                "profileImage", "new.png", MediaType.IMAGE_PNG_VALUE, "content".getBytes()
        );

        ProfileUpdateRequest request = new ProfileUpdateRequest(duplicateNickname, newImage);

        // when & then
        assertThatThrownBy(() -> userCommandService.updateProfile(username, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("이미 사용 중인 닉네임입니다.");

        // 예외가 발생했으므로 파일 저장이 호출되면 안 됨
        verify(fileStorageService, never()).save(any(), any(), any());
    }

    @Test
    void 비밀번호_변경_성공()
    {
        // given
        String username = "testuser";
        String rawCurrentPassword = "currentPassword123!";
        String rawNewPassword = "newPassword123!";
        String encodedCurrentPassword = "encoded-current-password";
        String encodedNewPassword = "encoded-new-password";

        // 기존 유저 모킹 (현재 암호화된 비밀번호를 가짐)
        Users mockUser = Users.builder()
                .username(username)
                .password(encodedCurrentPassword)
                .build();

        PasswordUpdateExRequest request = new PasswordUpdateExRequest(
                rawCurrentPassword,
                rawNewPassword,
                rawNewPassword
        );

        given(userReadService.getUserEntity(username)).willReturn(mockUser);

        // 현재 비밀번호 검증
        given(passwordEncoder.matches(rawCurrentPassword, encodedCurrentPassword)).willReturn(true);

        // 새 비밀번호가 현재 비밀번호와 같은지 검증
        given(passwordEncoder.matches(rawNewPassword, encodedCurrentPassword)).willReturn(false);

        // 새 비밀번호 암호화 결과 지정
        given(passwordEncoder.encode(rawNewPassword)).willReturn(encodedNewPassword);

        // when
        userCommandService.updatePassword(username,request);

        // then
        // 비밀번호가 변경되었는지 확인
        assertThat(mockUser.getPassword()).isEqualTo(encodedNewPassword);
    }

    @Test
    void 유저_소프트삭제_성공()
    {
        // given
        String username = "testuser";
        String password = "rawPassword";
        Long userId = 1L;

        Users mockUser = spy(Users.builder()
                .id(userId)
                .username(username)
                .password("encodedPassword")
                .build()); // providers가 null이므로 일반 유저

        // 1. 가짜 채팅방 2개 준비
        ChatRoom mockRoom1 = mock(ChatRoom.class);
        ChatRoom mockRoom2 = mock(ChatRoom.class);
        given(mockRoom1.getId()).willReturn(101L);
        given(mockRoom2.getId()).willReturn(102L);

        List<ChatRoom> activeRooms = List.of(mockRoom1, mockRoom2);

        // 2. 의존성 스터빙
        given(userReadService.getUserEntity(username)).willReturn(mockUser);
        given(passwordEncoder.matches(password, mockUser.getPassword())).willReturn(true);
        given(chatRoomRepository.findAllActiveRoomsByUserId(userId)).willReturn(activeRooms);

        // when
        userCommandService.softDeleteUser(username, password);

        // then
        // 1. 탈퇴 상태 변경 검증
        assertThat(mockUser.isDeleted()).isTrue();

        // 2. 채팅방 나갔는지 확인
        verify(chatRoomCommandService , times(1)).leaveRoom(101L,username);
        verify(chatRoomCommandService , times(1)).leaveRoom(102L,username);

    }

    @Test
    void 회원탈퇴_성공_소셜유저_비밀번호검증생략() {
        // given
        String username = "socialuser";
        Long userId = 2L;

        Users socialUser = spy(Users.builder()
                .id(userId)
                .username(username)
                .providers("KAKAO") // 소셜 유저
                .build());

        given(userReadService.getUserEntity(username)).willReturn(socialUser);
        given(chatRoomRepository.findAllActiveRoomsByUserId(userId)).willReturn(List.of()); // 참여 중인 방 없음

        // when
        userCommandService.softDeleteUser(username, null); // 비밀번호 없이 호출

        // then
        // 1. 소셜 유저이므로 passwordEncoder.matches()가 단 한 번도 실행되지 않았어야 함
        verify(passwordEncoder, never()).matches(anyString(), anyString());

        // 2. 탈퇴 메서드 정상 수행 검증
        verify(socialUser, times(1)).withdraw();
    }

    @Test
    void 비밀번호_찾기_성공()
    {
        // given
        String username = "testuser";
        String email = "이메일";
        Long userId = 1L;

        Users mockUser = Users.builder()
                .id(userId)
                .username(username)
                .email(email)
                .build(); // providers가 null이므로 일반 유저

        UserAccountCheckResponse response = new UserAccountCheckResponse(username,email,null);

        given(userReadService.getUserEntity(username)).willReturn(mockUser);
        given(userReadService.getAccountCheckInfoByEmail(email)).willReturn(response);

        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);

        // when
        userCommandService.sendFindPasswordEmail(email, username);

        // then
        verify(mailSenderUtil , times(1)).sendTempPasswordEmail(eq(email),passwordCaptor.capture());

        // 1. 캡처된 비밀번호 가져오기
        String capturedPassword = passwordCaptor.getValue();

        // 2. 실제 값 검증
        assertThat(capturedPassword).isNotNull().isNotBlank();

    }

    // 유저 엔티티 조회를 나중에 하는 어색함이 있음
    @Test
    void 비밀번호_찾기_실패_소셜유저()
    {
        // given
        String username = "testuser";
        String email = "이메일";

        UserAccountCheckResponse response = new UserAccountCheckResponse(username,email,"google");

        given(userReadService.getAccountCheckInfoByEmail(email)).willReturn(response);

        // when & then
        assertThatThrownBy(() -> userCommandService.sendFindPasswordEmail(email,username))
                .isInstanceOf(BusinessException.class)
                .hasMessage("소셜 로그인 계정은 비밀번호 찾기를 이용할 수 없습니다.");
    }

}