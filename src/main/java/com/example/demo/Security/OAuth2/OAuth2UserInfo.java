package com.example.demo.Security.OAuth2;

public interface OAuth2UserInfo {

    String getProvider();
    String getProviderId();
    String getEmail();
    String getName();
    String getRole();

}