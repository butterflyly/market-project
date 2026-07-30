package com.example.demo.Repository.Users;

import com.example.demo.Entity.Users.UserRole;
import com.example.demo.Entity.Users.Users;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users, Long> , UserRepositoryCustom{

    boolean existsByUsername(String username);

    boolean existsByNickname(@NotBlank String nickname);

    boolean existsByEmail(@Email String email);

    Optional<Users> findByUsernameAndDeletedFalse(String username);

    Optional<Users> findByIdAndDeletedFalse(Long userId);

    Optional<Users> findByEmailAndDeletedFalse(String email);


    // 한 달 지난 만료 회원 중 삭제 제동이 안 걸린 대상 ID 조회
    @Query("select u.id from Users u where u.deleted = true and u.deletedAt < :beforeTime and u.isPermanentPreserved = false")
    List<Long> findExpiredUserIdsForHardDelete(@Param("beforeTime") LocalDateTime beforeTime);

    long countByRole(UserRole userRole);


}
