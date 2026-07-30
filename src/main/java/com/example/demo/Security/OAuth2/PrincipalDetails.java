package com.example.demo.Security.OAuth2;

import com.example.demo.Entity.Users.Users;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/*
    스프링시큐리티가 /login 주소요청오면 낚아채 로그인진행시고
 * 로그인 진행이 완료되면 security session(세션내에 시큐리티세션이 있음.Security ContextHolder키값을 가짐) 에 넣어줌
 * security session에 들어갈 객체는(오브젝트타입) Authentication타입의 객체여야 하고,
 * Authentication객체 안에는 유저정보(ex.User)가 있어야 하는데 이는 UserDetails타입객체여야 한다.
 * -- security session => Authentication => UserDetails
 * */

public class PrincipalDetails implements UserDetails, OAuth2User {

    @Getter
    private final Users users;

    private final Map<String, Object> attributes;
    private final boolean isNewUser;


    public PrincipalDetails(Users users, Map<String, Object> attributes, boolean isNewUser) { // 소셜 로그인
        this.users = users;
        this.attributes = attributes;
        this.isNewUser = isNewUser;
    }


    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return users.getNickname();
    }

    // 해당 유저의 권한을 리턴하는 곳
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<GrantedAuthority> collection = new ArrayList<>();

        collection.add(new GrantedAuthority() {

            @Override
            public String getAuthority() {
                return users.getRole().getCode();
            }

        });

        return collection;
    }

    // 비밀번호 리턴
    @Override
    public String getPassword() {
        return users.getPassword();
    }


    // 유저 아이디 리턴
    @Override
    public String getUsername() {
        return users.getUsername();
    }


    // 계정이 만료되지 않았음을 리턴(TRUE는 만료되지 않음)
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    // 계정이 잠겨있지 않음
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    // 계정의 패스워드가 만료되지 않음
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // 계정이 사용가능함을 리턴
    @Override
    public boolean isEnabled() {
        return !users.isDeleted(); // 탈퇴 회원 로그인 방지
    }

    public boolean isNewUser() {
        return isNewUser;
    }
}

