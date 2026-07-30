package com.example.demo.Security.OAuth2;


import com.example.demo.Entity.Users.UserRole;
import com.example.demo.Entity.Users.Users;
import com.example.demo.Repository.Users.UserRepository;
import com.example.demo.Security.OAuth2.Google.GoogleUserDetails;
import com.example.demo.Security.OAuth2.Naver.NaverUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/*시큐리티 설정(SecurityConfig)에서 loginProcessingUrl("/login") 해놔서
 * /login 요청오면 자동으로 UserDetailsService타입으로 IoC되어있는
 * loadUserByUsername 메서드가 실행되도록 되어있다.
 * */
@Service
@Slf4j
@RequiredArgsConstructor
public class PrincipalOAuth2UserService  extends DefaultOAuth2UserService {

    private final UserRepository userRepository;


    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest); // OAauh2의 정보를 가져옴

        log.info("getAttributes : {}",oAuth2User.getAttributes());


        String provider = userRequest.getClientRegistration().getRegistrationId(); // 소셜사이트 정보 가져오기
        OAuth2UserInfo oAuth2UserInfo;

        // 뒤에 진행할 다른 소셜 서비스 로그인을 위해 구분 => 구글
        if(provider.equals("google")){
            log.info("구글 로그인");
            oAuth2UserInfo = new GoogleUserDetails(oAuth2User.getAttributes()); // 인터페이스에 구글 유저정보 넣어주기
        }
        else
        {
            log.info("네이버 로그인");
            oAuth2UserInfo = new NaverUserDetails(oAuth2User.getAttributes());
        }

        String providerId = oAuth2UserInfo.getProviderId();
        String email = oAuth2UserInfo.getEmail();
        String loginId = provider + "_" + providerId;
        String name = oAuth2UserInfo.getName()+"("+ provider + ")"; // 임시로 소셜 플랫폼 이름을 닉네임에 넣어줌
        UserRole role = UserRole.NORMAL;

        Users findMember = userRepository.findByUsernameAndDeletedFalse(loginId).orElse(null);
        //  Users deleteFindMember = deleteUserRepository.findByusername(loginId).orElse(null);
        Users member = null;
        boolean isNewUser = false; //  신규 여부 변수 선언

        // 그냥 아얘 없는 회원 0 0
        // USER_ROLE 을 GUEST 로 설정하고 회원가입
        if (findMember == null)
        {
            member = Users.builder()
                    .username(loginId)
                    .email(email)
                    .nickname(name)
                    .providers(provider)
                    .providerIds(providerId)
                    .profileUrl("/") // 일단 빈값
                    .role(role)
                    .build();

            userRepository.save(member);
            isNewUser = true;
        }
        else {
            member = findMember; //  이 줄이 핵심
        }

        // PrincipalDetails 반환 → 이후 SuccessHandler에서 JWT 발급
        return new PrincipalDetails(member, oAuth2User.getAttributes(), isNewUser);
    }
}

