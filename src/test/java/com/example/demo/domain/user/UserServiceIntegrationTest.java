package com.example.demo.domain.user;

import com.example.demo.Config.FileStorageService;
import com.example.demo.DTO.Users.Request.Auth.Register.UserRegisterRequest;
import com.example.demo.DTO.Users.Request.Info.ProfileUpdateRequest;
import com.example.demo.Entity.Chat.ChatRoom;
import com.example.demo.Entity.Chat.ChatRoomStatus;
import com.example.demo.Entity.Users.UserRole;
import com.example.demo.Entity.Users.Users;
import com.example.demo.Repository.Chat.ChatRoom.ChatRoomRepository;
import com.example.demo.Repository.Users.UserRepository;
import com.example.demo.Service.Users.UserCommandService;
import com.example.demo.Service.Users.UserReadService;
import groovy.util.logging.Slf4j;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@Transactional
@Slf4j
public class UserServiceIntegrationTest {

    @Autowired
    private UserCommandService userCommandService;

    @Autowired
    private UserReadService userReadService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private PasswordEncoder passwordEncoder; // 주입 받기

    @MockBean // 스프링 컨테이너 안의 RedisTemplate을 가짜(Mock)로 덮어씌움
    private RedisTemplate<String, String> redisTemplate;

    @MockBean // 파일 스토리지는 실제 I/O가 일어나지 않도록 Mock 처리
    private FileStorageService fileStorageService;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations; // ValueOperations도 목 객체로 선언

    @Test
    void 회원가입_성공() {
        // given
        String uniqueId = "test_" + UUID.randomUUID().toString().substring(0, 8);
        String email = uniqueId + "@example.com";
        String nickname = "홍_" + UUID.randomUUID().toString().substring(0, 4); // 닉네임도 고유하게!



        // 1. RedisTemplate이 호출되면 valueOperations를 반환하도록 설정
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // 2. isVerified() 내부에서 valueOperations.get(...)을 호출할 때 "true"를 리턴하도록 스텁 설정
        given(valueOperations.get("email:verified:" + email)).willReturn("true");


        // 2. DTO 생성 (inputCode는 아무 값이나 채워도 됨, 검증을 이미 우회했으므로)
        UserRegisterRequest request = new UserRegisterRequest(
                uniqueId,                     // 고유한 username
                nickname,                     // 고유한 nickname
                "Password123!",
                "Password123!",
                email,                        // 고유한 email
                null
        );

        // when: 실제 Command 서비스의 회원가입 실행 (내부에서 userRepository.save까지 일어남)
        userCommandService.register(request);

        // then: DB에 진짜 유저가 잘 박혔는지 확인
        Users savedUser = userRepository.findByUsernameAndDeletedFalse(uniqueId).orElse(null);
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getNickname()).isEqualTo(nickname);
    }

    @Test
    void 유저수정_성공()
    {
        // given
        Users existingUser = Users.builder()
                .username("testuser1")
                .password("encodedPassword")
                .nickname("기존닉네임")
                .email("test@example.com")
                .role(UserRole.NORMAL)
                .profileUrl("/profileImages/default.png")
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(existingUser);

        // 1. 테스트용 가짜 MultipartFile 생성
        // (파라미터: fieldName, originalFileName, contentType, fileContent바이트배열)
        MockMultipartFile fakeImage = new MockMultipartFile(
                "profileImage",                 // DTO의 필드명과 일치시키기
                "test-image.png",               // 원본 파일명
                MediaType.IMAGE_PNG_VALUE,      // Content-Type (image/png)
                "fake-image-content".getBytes() // 파일 내용 (바이트)
        );

        // 2. 파일 스토리지 Mocking 설정 (파일 저장이 호출되면 가짜 URL 리턴)
        given(fileStorageService.save(any(), any(), any()))
                .willReturn("/profileImages/new-test-image.png");

        // 3. 닉네임과 가짜 이미지 파일을 담은 수정 요청 DTO 생성
        ProfileUpdateRequest updateRequest = new ProfileUpdateRequest(
                "새로운닉네임",
                fakeImage
        );

        // when
        userCommandService.updateProfile("testuser1", updateRequest);

        // then: DB에 닉네임과 새로운 프로필 이미지 URL이 잘 반영되었는지 확인
        Users updatedUser = userRepository.findByUsernameAndDeletedFalse("testuser1").orElse(null);
        assertThat(updatedUser).isNotNull();
        assertThat(updatedUser.getNickname()).isEqualTo("새로운닉네임");
        assertThat(updatedUser.getProfileUrl()).isEqualTo("/profileImages/new-test-image.png");
    }

    @Test
    void 회원탈퇴_소프트삭제_성공() {
        // given
        String rawPassword = "Password123!";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        Users existingUser = Users.builder()
                .username("testuser1")
                .password(encodedPassword)
                .nickname("기존닉네임")
                .email("test@example.com")
                .role(UserRole.NORMAL)
                .profileUrl("/profileImages/default.png")
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(existingUser);

        Users existingUser2 = Users.builder()
                .username("testuser2")
                .password(encodedPassword)
                .nickname("기존닉네임2")
                .email("test2@example.com")
                .role(UserRole.NORMAL)
                .profileUrl("/profileImages/default.png")
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(existingUser2);

        ChatRoom chatRoom = ChatRoom.builder()
                .seller(existingUser)
                .buyer(existingUser2)
                .build();

        chatRoomRepository.save(chatRoom);

        // when
        userCommandService.softDeleteUser("testuser1", rawPassword);

        // then
        assertThat(existingUser.isDeleted()).isEqualTo(true);
        assertThat(chatRoom.getStatus()).isEqualTo(ChatRoomStatus.HOST_OUT);
        assertThat(chatRoom.getBuyer().getUsername()).isEqualTo("testuser2");
    }

}
