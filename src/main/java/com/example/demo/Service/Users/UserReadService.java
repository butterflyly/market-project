package com.example.demo.Service.Users;


import com.example.demo.DTO.Chat.Response.ChatRoomSummaryResponse;
import com.example.demo.DTO.Review.ReviewResponse;
import com.example.demo.DTO.Users.Response.*;
import com.example.demo.DTO.item.Response.ItemSummaryResponse;
import com.example.demo.Entity.Users.UserRole;
import com.example.demo.Entity.Users.Users;
import com.example.demo.Error.BusinessException;
import com.example.demo.Repository.Users.UserRepository;
import com.example.demo.Service.Chat.ChatRoomReadService;
import com.example.demo.Service.Item.ItemLikeReadService;
import com.example.demo.Service.Item.ItemReadService;
import com.example.demo.Service.Review.ReviewReadService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.example.demo.Entity.Users.UserRole.Black;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserReadService {

    private final UserRepository userRepository;


    /**
     *  유저 중복 체크 
     **/
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByNickname(String nickname) {
        return userRepository.existsByNickname(nickname);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public void validateDuplicateUsername(String username) {
        if(existsByUsername(username))
        {
            throw new BusinessException("이미 사용 중인 아이디입니다.");
        }
    }

    public void validateDuplicateNickname(String nickname) {
        if (existsByNickname(nickname)) {
            throw new BusinessException("이미 사용 중인 닉네임입니다.");
        }
    }

    public void validateDuplicateEmail(String email) {
        if (existsByEmail(email)) {
            throw new BusinessException("이미 가입된 이메일입니다.");
        }
    }

    /**
     *  유저 엔티티 조회
     */
    public Users getUserEntity(String username) {
        return userRepository.findByUsernameAndDeletedFalse(username).
                orElseThrow(() -> new BusinessException("유저가 존재하지 않습니다."));
    }

    public Users getUserEntityById(Long userId) {
        return userRepository.findByIdAndDeletedFalse(userId).
                orElseThrow(() -> new BusinessException("유저가 존재하지 않습니다."));
    }

    /**
     *  유저 DTO 값 조회
     */
    public UserDetailResponse getUserResponseDTO(String username) {

        Users users = getUserEntity(username);

        return UserDetailResponse.of(users);
    }

    public UserDetailResponse getUserResponseDTOById(Long userId) {

        Users users = getUserEntityById(userId);

        return UserDetailResponse.of(users);
    }


    /**
     *  탭 필터(BLACK, DELETED, ALL)와 검색어를 기반으로 타겟 유저 리스트 반환
     */
    public Page<AdminUserResponse> getTargetUserList(String filterType, String keyword, Pageable pageable) {

        // 혹시 모를 Null 방어로 기본값 ALL 세팅
        String filter = (filterType == null || filterType.isBlank()) ? "ALL" : filterType.toUpperCase();
        return userRepository.findAdminTargetUsers(filter, keyword, pageable)
                .map(AdminUserResponse::from);
    }

    /**
     * 관리자용 유저 상세 단건 조회 (마이페이지 요약 팝업용)
     */
    public AdminUserDetailResponse getUserDetail(Long id) {
        Optional<Users> user = userRepository.findById(id);

        if(user.isPresent())
        {
            return AdminUserDetailResponse.from(user.get());
        }
        else {
            throw new BusinessException("해당 유저는 존재하지 않습니다");
        }
    }

    public long getBlackUserCount() {
        return userRepository.countByRole(Black);
    }

    public UserAccountCheckResponse getAccountCheckInfoByEmail(String email) {

        Optional<Users> users = userRepository.findByEmailAndDeletedFalse(email);

        if(users.isEmpty())
        {
            throw new BusinessException("해당 유저는 존재하지 않습니다");
        }
        else {
            return UserAccountCheckResponse.of(users.get());
        }
    }


    /**
     * 게시글 작성 권한 검증 (경고 2회 이상 시 작성 불가능)
     */
    public void validateItemCreatePermission(String username) {
        Users user = getUserEntity(username);

        if (user.getWarningCount() >= 2) {
            throw new BusinessException("경고 2회 이상 누적으로 인해 현재 게시글 작성이 제한된 상태입니다.");
        }
    }

    public UserSummaryResponse getUserSummary(String username) {

        Users users = getUserEntity(username);

        return UserSummaryResponse.of(users);
    }
}
