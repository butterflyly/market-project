package com.example.demo.Entity.Users;

import com.example.demo.Error.BusinessException;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;


@Entity
@EntityListeners(AuditingEntityListener.class)
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "username"),
                @UniqueConstraint(columnNames = "email"),
                @UniqueConstraint(columnNames = "nickname")
        }
)
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nickname;

    @Column
    private String displayNickname;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    @CreatedDate
    @Column(updatable = false) // 생성 후 수정되지 않도록 보호
    private LocalDateTime createdAt;

    private String profileUrl;

    @Column(nullable = false, name = "is_deleted")
    private boolean deleted = false; // 디폴트 false

    @Column
    private LocalDateTime deletedAt;

    private String providers;
    private String providerIds;

    @Column(nullable = false)
    private boolean isPermanentPreserved = false; // 기본값 false (1달 뒤 자동 영구삭제 대상)


    // 생성자에만 Builder 적용
    @Builder
    private Users(Long id,
                  String username,
                  String password,
                  String nickname,
                  String email,
                  UserRole role,String displayNickname,
                  LocalDateTime createdAt,String profileUrl
                    ,String providers,String providerIds
    ) {
        this.id = id;

        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.email = email;
        //  [안전장치 1] 가입할 땐 빌더에서 안 넘겨줘도 기본 '일반 회원(NORMAL)' 권한 부여
        this.role = (role != null) ? role : UserRole.NORMAL;

        this.displayNickname =displayNickname;
        this.deleted = false;
        this.createdAt = createdAt;
        this.profileUrl = profileUrl;
        this.providers = providers;
        this.providerIds = providerIds;

        // [안전장치 2] 빌더가 필드 초기화를 씹어먹는 버그를 원천 차단
        this.warningCount = 0;
    }

    @Column(nullable = false)
    private int warningCount = 0; // 경고 누적 횟수 (기본값 0)

    public void updateNickname(String newNickname) {
        if (newNickname == null || newNickname.isBlank()) {
            throw new BusinessException("닉네임은 비어있을 수 없습니다.");
        }
        this.nickname = newNickname;
        this.displayNickname = newNickname;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void updateProfileUrl(String newProfileUrl) {
        this.profileUrl = newProfileUrl;
    }

    public void withdraw() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
        // 필요하다면 닉네임에 (탈퇴) 접미사를 붙이기도 합니다.
        this.nickname = "탈퇴사용자_" + this.id;
    }

    /**
     *  유저에게 경고를 1회 부여하는 도메인 비즈니스 메서드
     */
    public void receiveWarning() {
        this.warningCount++;

        //  [핵심 정책] 경고가 3회 이상 누적되면 자동으로 블랙 회원 격하
        if (this.warningCount >= 3) {
            this.role = UserRole.Black;
        }
    }

    /**
     *  관리자가 상황의 심각성을 인지하고 즉시 블랙으로 찍어 누를 때 (원스톱 제재)
     */
    public void forceBlack() {
        this.role = UserRole.Black;
    }

    public void changeRole(UserRole userRole) {
        this.role = userRole;
    }

    public void resetWarningCount() {
        this.warningCount = 0;
    }

    public void restoreSoftDelete() {
        this.deletedAt = null;
        this.deleted = false;
        this.nickname = displayNickname;
    }

    /**
     *  관리자 전용: 영구 삭제 제동(보존) 스위치 토글
     */
    public void togglePermanentPreserve() {
        this.isPermanentPreserved = !this.isPermanentPreserved;
    }

}
