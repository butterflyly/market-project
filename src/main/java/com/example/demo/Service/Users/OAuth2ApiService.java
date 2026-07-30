package com.example.demo.Service.Users;

import com.example.demo.Entity.Users.Users;
import com.example.demo.Security.OAuth2.UnlinkResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2ApiService {

    private final RestTemplate restTemplate = new RestTemplate();

    // 네이버 Client ID & Secret (application.yml에 있는 값 주입받아 사용 권장)
    // @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private final String naverClientId = "YOUR_NAVER_CLIENT_ID";
    // @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private final String naverClientSecret = "YOUR_NAVER_CLIENT_SECRET";

    /**
     *  네이버 연동 해제 (Unlink)
     */
    public void unlinkNaver(Users user) {
        // 실제 운영 시에는 유저가 로그인할 때 발급받은 소셜 access_token을 넘겨받아야 합니다.
        String socialAccessToken = getSocialAccessToken(user);

        String url = "https://nid.naver.com/oauth2.0/token";

        // 네이버가 요구하는 파라미터 세팅 (grant_type=delete 필수)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", naverClientId);
        params.add("client_secret", naverClientSecret);
        params.add("access_token", socialAccessToken);
        params.add("grant_type", "delete"); // 연동 해제를 의미
        params.add("service_provider", "naver");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<UnlinkResponse> response = restTemplate.postForEntity(url, request, UnlinkResponse.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("네이버 연동 해제 성공: {}", response.getBody().getResult());
            }
        } catch (Exception e) {
            log.error("네이버 연동 해제 통신 중 에러 발생: {}", e.getMessage());
            throw new RuntimeException("네이버 연동 해제 실패");
        }
    }

    /**
     *  구글 연동 해제 (Revoke)
     */
    public void unlinkGoogle(Users user) {
        String socialAccessToken = getSocialAccessToken(user);

        // 구글은 토큰을 무효화(Revoke)하는 엔드포인트에 쿼리 스트링으로 토큰을 실어 보냅니다.
        String url = "https://oauth2.googleapis.com/revoke?token=" + socialAccessToken;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<String> request = new HttpEntity<>(headers);

        try {
            // 구글은 성공 시 200 OK와 함께 빈 응답을 반환합니다.
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("구글 연동 해제 성공 - 유저: {}", user.getUsername());
            }
        } catch (Exception e) {
            log.error("구글 연동 해제 통신 중 에러 발생: {}", e.getMessage());
            throw new RuntimeException("구글 연동 해제 실패");
        }
    }

    /**
     *  [임시 유틸 메서드] 유저의 소셜 Access Token 꺼내오기
     * (소셜 로그인 성공 시 Redis나 DB, 세션 등에 저장해 둔 토큰을 반환하는 로직이 필요합니다)
     */
    private String getSocialAccessToken(Users user) {
        // TODO: 실제 구현 시 사용자의 현재 유효한 소셜 Access Token을 조회하는 로직 작성
        // 지금은 테스트용 임시 토큰 문자열 반환
        return "MOCK_SOCIAL_ACCESS_TOKEN_FROM_STORAGE";
    }
}