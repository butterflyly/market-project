package com.example.demo.Repository.Users;

import com.example.demo.Entity.Users.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserRepositoryCustom {

    Page<Users> findAdminTargetUsers(String filterType, String keyword, Pageable pageable);
}
