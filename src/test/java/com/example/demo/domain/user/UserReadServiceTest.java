package com.example.demo.domain.user;

import com.example.demo.DTO.Users.Response.AdminUserResponse;
import com.example.demo.DTO.Users.Response.UserDetailResponse;
import com.example.demo.Entity.Users.UserRole;
import com.example.demo.Entity.Users.Users;
import com.example.demo.Error.BusinessException;
import com.example.demo.Repository.Users.UserRepository;
import com.example.demo.Service.Users.UserReadService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@Slf4j
class UserReadServiceTest {

    @InjectMocks
    private UserReadService userReadService;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("아이디 존재 여부 확인 - 존재할 경우 true 반환")
    void existsByUsername_true() {

        // given
        given(userRepository.existsByUsername("testuser")).willReturn(true);

        // when
        boolean result = userReadService.existsByUsername("testuser");
        log.info("결과 값  : " + result);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("아이디 존재 여부 확인 - 존재하지 않을 경우 false 반환")
    void existsByUsername_false() {

        // given
        given(userRepository.existsByUsername("noneUser")).willReturn(false);

        // when
        boolean result = userReadService.existsByUsername("noneUser");

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("중복 아이디 검증 - 중복 시 BusinessException 발생")
    void validateDuplicateUsername_throwsException() {

        // given
        given(userRepository.existsByUsername("duplicateUser")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> userReadService.validateDuplicateUsername("duplicateUser"))
                .isInstanceOf(BusinessException.class) // 튀어나온 예외가 BusinessException 타입인가?
                .hasMessage("이미 사용 중인 아이디입니다."); // 그 예외 안의 message가 이 문장과 일치하는가?
    }

    @Test
    @DisplayName("중복 아이디 검증 - 중복이 아닐 경우 예외 없이 정상 통과")
    void validateDuplicateUsername_success() {

        // given
        given(userRepository.existsByUsername("newUser")).willReturn(false);

        // when & then
        // 예외가 발생하지 않고 잘 끝나는지 검증하는 AssertJ 구문
        assertThatCode(() -> userReadService.validateDuplicateUsername("newUser"))
                // 어떤 예외(Exception)도 터지지 않고 정상적으로 종료되어야 함
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("유저 엔티티 조회 성공 - 존재하는 username으로 조회 시 Users 객체 반환")
    void getUserEntity_success() {

        // given
        Users mockUser = Users.builder()
                .username("testuser")
                .nickname("테스트닉네임")
                .email("test@example.com")
                .build();

        given(userRepository.findByUsernameAndDeletedFalse("testuser"))
                .willReturn(Optional.of(mockUser));

        // when
        Users result = userReadService.getUserEntity("testuser");

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
        assertThat(result.getNickname()).isEqualTo("테스트닉네임");
    }

    @Test
    @DisplayName("유저 엔티티 조회 실패 - 존재하지 않는 username 조회 시 BusinessException 발생")
    void getUserEntity_notFound_throwsException() {

        // given
        given(userRepository.findByUsernameAndDeletedFalse("noneUser"))
                .willReturn(Optional.empty()); // DB에 없음(empty)

        // when & then
        assertThatThrownBy(() -> userReadService.getUserEntity("noneUser"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("유저가 존재하지 않습니다.");
    }

    @Test
    @DisplayName("유저 DTO 조회 성공 - DTO로 올바르게 변환")
    void getUserResponseDTO_success()
    {
        // given
        Users mockUser = Users.builder()
                .username("testuser")
                .nickname("테스트닉네임")
                .email("test@example.com")
                .build();

        given(userRepository.findByUsernameAndDeletedFalse("testuser"))
                .willReturn(Optional.of(mockUser));

        // when
        UserDetailResponse userDetailResponse = userReadService.getUserResponseDTO("testuser");

        // then
        assertThat(userDetailResponse).isNotNull();
        assertThat(userDetailResponse)
                .extracting("username","nickname")
                .containsExactly("testuser","테스트닉네임");

    }

    @Test
    @DisplayName("유저 DTO 조회 실패 - 존재하지 않는 username 조회 시 BusinessException 발생")
    void getUserResponseDTO_notFound_throwsException() {

        // given
        given(userRepository.findByUsernameAndDeletedFalse("noneUser"))
                .willReturn(Optional.empty()); // DB에 없음

        // when & then
        assertThatThrownBy(() -> userReadService.getUserResponseDTO("noneUser"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("유저가 존재하지 않습니다.");
    }

    @Test
    @DisplayName("유저 목록 조회")
    void getTargetUserList_success()
    {
        // given
        // 1. 컨트롤러의 스펙에 맞춘 파라미터 세팅
        String tab = "ALL";
        String keyword = "testuser";
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id").descending());

        // 2. 반복문(IntStream)을 활용해 가짜 Users 엔티티 15개 생성
        List<Users> mockUsersList = IntStream.rangeClosed(1, 15)
                .mapToObj(i -> Users.builder()
                        .id((long) i)
                        .username("testuser" + i)
                        .nickname("테스트닉네임" + i)
                        .email("test" + i + "@example.com")
                        .role(UserRole.NORMAL)
                        .build())
                .toList();

        // 3. 전체 15개 데이터 중 0번째 페이지(10개)만 포함하는 PageImpl 생성
        // (content: 현재 페이지에 담길 10개, pageable: 요청 정보, total: 전체 15개)
        List<Users> pageContent = mockUsersList.subList(0, 10);
        Page<Users> mockUsersPage = new PageImpl<>(pageContent, pageable, mockUsersList.size());

        given(userRepository.findAdminTargetUsers("ALL", keyword, pageable))
                .willReturn(mockUsersPage);

        // when
        Page<AdminUserResponse> adminUserResponsePage = userReadService.getTargetUserList(tab , keyword , pageable);

        // then
        assertThat(adminUserResponsePage).isNotNull();

        // 페이징 지표 검증
        assertThat(adminUserResponsePage.getTotalElements()).isEqualTo(15); // 전체 데이터
        assertThat(adminUserResponsePage.getTotalPages()).isEqualTo(2);
        assertThat(adminUserResponsePage.getContent()).hasSize(10);
        assertThat(adminUserResponsePage.hasNext()).isTrue();  // 다음 페이지 존재여부

        // DTO 변환 데이터 검증 (첫 번째 요소 확인)
        AdminUserResponse firstUser = adminUserResponsePage.getContent().get(0);
        assertThat(firstUser.id()).isEqualTo(1L);
        assertThat(firstUser.username()).isEqualTo("testuser1");
        assertThat(firstUser.role()).isEqualTo(UserRole.NORMAL);

    }

    @Test
    @DisplayName("게시글 작성 권한 검증 - 경고 1회 이하일 경우 정상 통과")
    void validateItemCreatePermission_success() {

        // given
        Users mockUser = Users.builder()
                .username("cleanUser")
                .build(); // warningCount는 생성자에서 기본 0으로 세팅됨

        given(userRepository.findByUsernameAndDeletedFalse("cleanUser"))
                .willReturn(Optional.of(mockUser));

        // when & then
        assertThatCode(() -> userReadService.validateItemCreatePermission("cleanUser"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("게시글 작성 권한 검증 - 경고 2회 이상 시 BusinessException 발생")
    void validateItemCreatePermission_warningOver_throwsException() {

        // given
        Users warnedUser = Users.builder()
                .username("badUser")
                .build();

        // 경고 2회 추가
        warnedUser.receiveWarning();
        warnedUser.receiveWarning();

        given(userRepository.findByUsernameAndDeletedFalse("badUser"))
                .willReturn(Optional.of(warnedUser));

        // when & then
        assertThatThrownBy(() -> userReadService.validateItemCreatePermission("badUser"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("경고 2회 이상 누적으로 인해 현재 게시글 작성이 제한된 상태입니다.");
    }

    @Test
    @DisplayName("블랙리스트 유저 수 카운트 조회 성공")
    void getBlackUserCount_success() {

        // given
        given(userRepository.countByRole(UserRole.Black))
                .willReturn(5L);

        // when
        long count = userReadService.getBlackUserCount();

        // then
        assertThat(count).isEqualTo(5L);
    }
}