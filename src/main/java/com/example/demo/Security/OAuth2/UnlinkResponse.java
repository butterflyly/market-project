package com.example.demo.Security.OAuth2;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UnlinkResponse {

    @JsonProperty("access_token")
    private String accessToken;
    private String result;
}