package com.example.demo.Service.Users;


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
import com.example.demo.Entity.Users.UserRole;
import com.example.demo.Entity.Users.Users;
import com.example.demo.Error.BusinessException;
import com.example.demo.Repository.Chat.ChatRoom.ChatRoomRepository;
import com.example.demo.Repository.Item.Item.ItemRepository;
import com.example.demo.Repository.Users.UserRepository;
import com.example.demo.Security.JWT.JwtProvider;
import com.example.demo.Security.JWT.JwtToken;
import com.example.demo.Service.Chat.ChatRoomCommandService;
import com.example.demo.Util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseCookie;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserCommandService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserReadService userReadService;
    private final RedisTemplate<String, String> redisTemplate;
    private final FileStorageService fileStorageService;

    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final CookieUtil cookieUtil;
    private final ChatRoomCommandService chatRoomCommandService;
    private final ChatRoomRepository chatRoomRepository;
    private final MailSenderUtil mailSenderUtil;
    private final ItemRepository itemRepository;


    @Value("${spring.mail.username}")
    private String ADMIN_ADDRESS;

    //  하위 폴더명과 브라우저 접근 URL 접두사를 상수로 안전하게 관리
    private static final String PROFILE_SUB_FOLDER = "profileImageUpload/";
    private static final String PROFILE_URL_PREFIX = "/profileImages/";
    private static final String DEFAULT_PROFILE_URL = "/images/default-profile.png";

    // 회원가입
    public void register(UserRegisterRequest dto) {

        if(!isVerified(dto.email())) {
            throw new BusinessException("이메일 인증이 필요합니다.");
        }

        if (!dto.password().equals(dto.password2())) {
            throw new BusinessException("비밀번호가 일치하지 않습니다.");
        }

        if(userReadService.existsByUsername(dto.username())) {
            throw new BusinessException("중복되는 아이디입니다");
        }

        if(userReadService.existsByNickname(dto.nickname())) {
            throw new BusinessException("중복되는 닉네임입니다");
        }

        if(userReadService.existsByEmail(dto.email())) {
            throw new BusinessException("중복되는 이메일입니다.");
        }

        //  기본 이미지 설정 및 파일 저장 로직 상수 연동
        String profileUrl = DEFAULT_PROFILE_URL;

        if (dto.profileImage() != null && !dto.profileImage().isEmpty()) {
            profileUrl = fileStorageService.save(dto.profileImage(), PROFILE_SUB_FOLDER, PROFILE_URL_PREFIX);
        }

        Users user = Users.builder()
                .username(dto.username())
                .password(passwordEncoder.encode(dto.password()))
                .nickname(dto.nickname())
                .displayNickname(dto.nickname())
                .email(dto.email())
                .role(UserRole.NORMAL)
                .profileUrl(profileUrl)
                .createdAt(LocalDateTime.now())
                .build();

        redisTemplate.delete("email:verified:" + dto.email());

        userRepository.save(user);
    }

    // 랜덤숫자 생성(인증번호)
    public int createNumber() {
        return (int)(Math.random() * 900000) + 100000;
    }

    public void createAuthCode(String email, int code) {
        String key = "email:code:" + email;
        redisTemplate.opsForValue()
                .set(key, String.valueOf(code), Duration.ofMinutes(5));
    }

    // 이메일 인증코드 발송
    public void sendVerificationEmail(String email) {
        String cooldownKey = "email:cooldown:" + email;

        if (redisTemplate.hasKey(cooldownKey)) {
            throw new BusinessException("잠시 후 다시 시도해주세요.");
        }

        int code = createNumber();
        createAuthCode(email, code);

        redisTemplate.opsForValue()
                .set(cooldownKey, "true", Duration.ofSeconds(60));

        mailSenderUtil.sendVerificationEmail(email, code);
    }

    public void verify(String email, String inputNumber) {
        String key = "email:code:" + email;
        String storedCode = redisTemplate.opsForValue().get(key);

        // 1. 인증번호가 일치하지 않을 경우
        if (storedCode == null || !storedCode.equals(inputNumber)) {
            throw new BusinessException("인증 번호가 일치하지 않습니다.");
        }

        // 2. 예외를 통과한 정상 흐름은 들여쓰기 없이 깔끔하게 아래에서 처리
        redisTemplate.delete(key);
        redisTemplate.opsForValue()
                .set("email:verified:" + email, "true", Duration.ofMinutes(10));
    }

    public boolean isVerified(String email) {
        String key = "email:verified:" + email;
        String value = redisTemplate.opsForValue().get(key);
        return value != null && value.equals("true");
    }

    /*
     * 로그인
     */
    public LoginResponseDto login(LoginRequest request) {

        // 1. 스프링 시큐리티 인증 진행
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        // 2. 토큰 생성 및 갱신 (내부적으로 Redis 저장까지 완료)
        JwtToken token = jwtProvider.createToken(authentication, request.rememberMe());

        // 3. 쿠키 객체 생성
        ResponseCookie accessCookie = jwtProvider.createAccessCookie(token.getAccessToken());
        ResponseCookie refreshCookie = jwtProvider.createRefreshCookie(token.getRefreshToken(), request.rememberMe());

        // 4. 컨트롤러에게 전달할 DTO 반환 (쿠키들과 유저 정보 포함)
        Users users = userReadService.getUserEntity(authentication.getName());
        return new LoginResponseDto(accessCookie, refreshCookie, users.getNickname() ,users.getProfileUrl());
    }

    // 로그아웃
    public void logout(HttpServletRequest request) {
        // 1. 쿠키 유틸을 통해 리프레시 토큰 추출
        String refreshToken = cookieUtil.getCookieValue(request, "refreshToken");

        // 2. 토큰이 유효하면 Redis에 저장된 세션 제거
        if (refreshToken != null && jwtProvider.validateToken(refreshToken)) {
            String username = jwtProvider.getUsername(refreshToken);
            redisTemplate.delete(username);
        }
    }

    // 유저 데이터 수정
    public void updateProfile(String username, ProfileUpdateRequest profileUpdateRequest) {

        Users user = userReadService.getUserEntity(username);

        // 1. 닉네임 수정
        if (!user.getNickname().equals(profileUpdateRequest.nickname())) {
            if (userReadService.existsByNickname(profileUpdateRequest.nickname())) {
                throw new BusinessException("이미 사용 중인 닉네임입니다.");
            }
            user.updateNickname(profileUpdateRequest.nickname());
        }

        // 2. 프로필 이미지 수정
        if (profileUpdateRequest.profileImage() != null && !profileUpdateRequest.profileImage().isEmpty()) {

            //  [선택적 고도화] 기존에 커스텀 프로필 이미지가 있었다면 스토리지에서 삭제
            // (단, 'default-profile.png' 같은 기본 이미지는 지워지면 안 되므로 예외 처리 필요)
            if (user.getProfileUrl() != null && !user.getProfileUrl().contains("default-profile")) {
                fileStorageService.delete(user.getProfileUrl(),PROFILE_SUB_FOLDER,PROFILE_URL_PREFIX);
            }

            // 새 이미지 저장
            String profileUrl = fileStorageService.save(profileUpdateRequest.profileImage(), PROFILE_SUB_FOLDER, PROFILE_URL_PREFIX);
            user.updateProfileUrl(profileUrl);
        }
    }

    // 비밀번호 변경
    public void updatePassword(String username, PasswordUpdateExRequest passwordUpdateExRequest) {
        Users user = userReadService.getUserEntity(username);

        // 1. 현재 비밀번호 검증
        if (!passwordEncoder.matches(passwordUpdateExRequest.currentPassword(), user.getPassword())) {
            throw new BusinessException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 2. [선택적 추가] 새 비밀번호가 현재 비밀번호와 같은지 검증 (보안 권장 사항)
        if (passwordEncoder.matches(passwordUpdateExRequest.newPassword(), user.getPassword())) {
            throw new BusinessException("새 비밀번호는 현재 비밀번호와 다르게 설정해야 합니다.");
        }

        // 3. 새 비밀번호와 확인 비밀번호 일치 검증
        if (!passwordUpdateExRequest.newPassword().equals(passwordUpdateExRequest.confirmNewPassword())) {
            throw new BusinessException("새 비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }

        // 3. 비밀번호 변경 및 암호화
        user.changePassword(passwordEncoder.encode(passwordUpdateExRequest.newPassword()));
    }

    public void softDeleteUser(String username, String password) {
        Users user = userReadService.getUserEntity(username);
        boolean isSocialUser = (user.getProviders() != null && !user.getProviders().isBlank());

        // 1. 일반 유저인 경우에만 비밀번호 검증
        // 백엔드에서도 검증하기
        if (!isSocialUser) {
            if (!passwordEncoder.matches(password, user.getPassword())) {
                throw new BusinessException("비밀번호가 일치하지 않아 탈퇴할 수 없습니다.");
            }
        }

        // 2. 이 유저가 참여 중인 모든 채팅방을 돌며 '퇴장 공지' 날리기
        List<ChatRoom> activeRooms = chatRoomRepository.findAllActiveRoomsByUserId(user.getId());
        for (ChatRoom room : activeRooms) {
            chatRoomCommandService.leaveRoom(room.getId(), user.getUsername());
        }

        // 3. 공통 탈퇴 처리 (Soft Delete)
        user.withdraw();
        log.info("회원 탈퇴 처리 완료 (30일 유예 데이터 보존 시작): {}", username);
    }

    // 강제 블랙 처리
    public void forceBlackUser(Long id) {
        Users user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("존재하지 않는 회원입니다."));
        user.forceBlack();

        mailSenderUtil.sendBlackUserNotice(user.getEmail());
    }

    public void restoreBlackUser(Long id) {

        Users user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("존재하지 않는 회원입니다."));
        user.changeRole(UserRole.NORMAL);
        user.resetWarningCount();

        mailSenderUtil.sendRestoreNotice(user.getEmail());
    }

    public void restoreDeletedUser(Long id) {
        Users user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("존재하지 않는 회원입니다."));
        user.restoreSoftDelete();

       mailSenderUtil.sendRestoreNotice(user.getEmail());
    }

    /**
     *  [어드민] 유저 데이터 1달 뒤 자동 영구삭제 제동(보존/해제) 토글
     */
    public void toggleUserPreservation(Long userId) {

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("존재하지 않는 회원입니다."));

        user.togglePermanentPreserve(); // 더티 체킹으로 자동 update
    }

    // 아이디 찾기 메일 발송
    @Transactional(readOnly = true)
    public void sendFindUsernameEmail(String email) {
        UserAccountCheckResponse user = userReadService.getAccountCheckInfoByEmail(email);

        // 소셜 유저인 경우 예외 발생
        if (user.isSocialUser()) {
            throw new BusinessException("해당 계정은 소셜 로그인 계정입니다. 소셜 로그인을 이용해 주세요.");
        }

        mailSenderUtil.sendUsernameEmail(user.email(), user.username());
    }

    // 비밀번호 찾기 메일 발송
    public void sendFindPasswordEmail(String email, String username) {
        UserAccountCheckResponse user = userReadService.getAccountCheckInfoByEmail(email);

        if (user.isSocialUser()) {
            throw new BusinessException("소셜 로그인 계정은 비밀번호 찾기를 이용할 수 없습니다.");
        }

        // 아이디 일치 여부 검증
        if (!user.username().equals(username)) {
            throw new BusinessException("입력하신 아이디와 이메일 정보가 일치하지 않습니다.");
        }

        String newPassword = PasswordGenerator.generateRandomPassword();
        Users entity = userReadService.getUserEntity(user.username());
        entity.changePassword(passwordEncoder.encode(newPassword));

        mailSenderUtil.sendTempPasswordEmail(user.email(), newPassword);
    }


    // 랜덤 비밀번호 생성
    public static class PasswordGenerator {
        private static final String CHAR_LOWER = "abcdefghijklmnopqrstuvwxyz";
        private static final String CHAR_UPPER = CHAR_LOWER.toUpperCase();
        private static final String NUMBER = "0123456789";
        private static final String OTHER_CHAR = "!@#$%&*()_+-=[]?";

        private static final String PASSWORD_ALLOW_BASE = CHAR_LOWER + CHAR_UPPER + NUMBER + OTHER_CHAR;
        private static final int PASSWORD_LENGTH = 12;
        private static final SecureRandom RANDOM = new SecureRandom();

        public static String generateRandomPassword() {
            StringBuilder sb = new StringBuilder(PASSWORD_LENGTH);

            // 1. 보안 정책 충족을 위해 각 종류별 최소 1개 필수 포함
            sb.append(CHAR_LOWER.charAt(RANDOM.nextInt(CHAR_LOWER.length())));
            sb.append(CHAR_UPPER.charAt(RANDOM.nextInt(CHAR_UPPER.length())));
            sb.append(NUMBER.charAt(RANDOM.nextInt(NUMBER.length())));
            sb.append(OTHER_CHAR.charAt(RANDOM.nextInt(OTHER_CHAR.length())));

            // 2. 나머지 길이만큼은 전체 문집합에서 랜덤 추출
            for (int i = 4; i < PASSWORD_LENGTH; i++) {
                int rndCharAt = RANDOM.nextInt(PASSWORD_ALLOW_BASE.length());
                sb.append(PASSWORD_ALLOW_BASE.charAt(rndCharAt));
            }

            // 3. 앞선 4개 문자의 위치가 고정되지 않도록 문자열 섞기
            List<Character> letters = sb.toString().chars()
                    .mapToObj(c -> (char) c)
                    .collect(Collectors.toList());
            Collections.shuffle(letters, RANDOM);

            StringBuilder result = new StringBuilder();
            for (char c : letters) {
                result.append(c);
            }

            return result.toString();
        }
    }


    public void warnSellerByItem(Long itemId) {
        Item item = itemRepository.findByIdAndDeletedFalse(itemId)
                .orElseThrow(() -> new BusinessException("존재하지 않는 게시글입니다."));

        Users seller = item.getSeller();

        // 1. 작성자 경고 횟수 증가
        seller.receiveWarning(); // 엔티티 내에서 count + 1 로직

        // 2. 문제가 된 게시글도 즉시 삭제(또는 블라인드) 처리
        item.delete();
    }

    public void forceBlackSellerByItem(Long itemId) {

        Item item = itemRepository.findByIdAndDeletedFalse(itemId)
                .orElseThrow(() -> new BusinessException("존재하지 않는 게시글입니다."));

        Users seller = item.getSeller();

        // 1. 바로 블랙
        seller.forceBlack();

        // 2. 문제가 된 게시글도 즉시 삭제(또는 블라인드) 처리
        item.delete();
    }


    /**
     * 회원 경고 부여
     * TODO: [제재 정책 비즈니스 로직 구현 예정]
     * - 1회차: 단순 인앱/SSE 알림 전송 (역할 변경 없음)
     * - 2회차: UserRole.BLACK 으로 변경 및 작성 권한 제한
     */
    @Transactional
    public void warnUser(Long userId, String reason) {
        // 1. 유저 조회
        // 2. 경고 횟수 증가 (user.increaseWarningCountCount())
        // 3. if (user.getWarningCount() >= 2) -> user.updateRole(UserRole.BLACK);
        // 4. 알림 발송 로직 호출
    }

}
