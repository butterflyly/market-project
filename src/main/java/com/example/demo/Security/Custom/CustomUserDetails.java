package com.example.demo.Security.Custom;

import com.example.demo.Entity.Users.UserRole;
import com.example.demo.Entity.Users.Users;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

// JWT 발급 시 UserRole을 authorities로 변환하기 위해 CustomUserDetails 구현
public class CustomUserDetails implements UserDetails {

    private final Users users;

    @Getter
    private final Long id;
    private final String username;
    private final String password;

    @Getter
    private final String email;

    @Getter
    private final String nickname;

    @Getter
    private final UserRole role;

    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(Users users) {
        this.id = users.getId();
        this.username = users.getUsername();
        this.password = users.getPassword();
        this.email = users.getEmail();
        this.nickname = users.getNickname();
        this.role = users.getRole();
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
        this.users = users;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return !users.isDeleted();  // deleted=true면 로그인 불가능
        // && users.getRole() != UserRole.Black;
    }

}