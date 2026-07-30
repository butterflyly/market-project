package com.example.demo.Security.JWT;

import com.example.demo.Entity.Users.UserRole;
import com.example.demo.Security.Custom.CustomUserDetails;
import com.example.demo.Security.OAuth2.PrincipalDetails;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtProvider {

    private final Key key;
    private final UserDetailsService userDetailsService;

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String GRANT_TYPE = "Bearer";

    @Value("${jwt.access-token.expiration}")
    private long ACCESS_TOKEN_EXPIRE_TIME;

    @Value("${jwt.refresh-token.expiration}")
    private long REFRESH_TOKEN_EXPIRE_TIME;

    @Value("${jwt.refresh-token.session-expiration}")
    private long SESSION_REFRESH_TOKEN_EXPIRE_TIME;

    public JwtProvider(@Value("${jwt.secret}") String secretKey,
                       UserDetailsService userDetailsService,
                       RedisTemplate<String, Object> redisTemplate)
    {
        byte[] keyByte = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyByte);
        this.userDetailsService = userDetailsService;
        this.redisTemplate = redisTemplate; // 주입 완료
    }

    // 토큰 생성
    public JwtToken createToken(Authentication authentication, boolean rememberMe)
    {
        Object principalObj = authentication.getPrincipal();

        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        String roleCode = "N"; // 기본 NORMAL

        if (principalObj instanceof CustomUserDetails custom) {
            UserRole userRole = custom.getRole();
            roleCode = userRole.getCode();
        } else if (principalObj instanceof PrincipalDetails social) {
            UserRole userRole = social.getUsers().getRole();
            roleCode = userRole.getCode();
        }

        long now = (new Date()).getTime();
        String username = authentication.getName();

        // AccessToken 생성
        Date accessTokenExpire = new Date(now + ACCESS_TOKEN_EXPIRE_TIME);
        String accessToken = generateAccessToken(username, authorities, accessTokenExpire, roleCode);

        long refreshExpireTime = rememberMe
                ? REFRESH_TOKEN_EXPIRE_TIME
                : SESSION_REFRESH_TOKEN_EXPIRE_TIME;

        // RefreshToken 생성
        Date refreshTokenExpire = new Date(now + refreshExpireTime);
        String refreshToken = generateRefreshToken(username, refreshTokenExpire);

        //  [변경] RedisDao 대신 스프링 순정 RedisTemplate 메서드로 RefreshToken 저장
        // 지정한 밀리초(refreshExpireTime)만큼 지나면 자동 만료(TTL) 처리
        redisTemplate.opsForValue()
                .set(username, refreshToken, Duration.ofMillis(refreshExpireTime));

        return JwtToken.builder()
                .grantType(GRANT_TYPE)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    private String generateAccessToken(String username, String authorities, Date expireDate, String roleCode) {
        return Jwts.builder()
                .setSubject(username)
                .claim("auth", authorities)
                .claim("role", roleCode)
                .setExpiration(expireDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private String generateRefreshToken(String username, Date expireDate) {
        return Jwts.builder()
                .setSubject(username)
                .setExpiration(expireDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }


    public String getUsername(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    public ResponseCookie createAccessCookie(String accessToken) {
        return org.springframework.http.ResponseCookie.from("accessToken", accessToken)
                .path("/")
                .httpOnly(true)
                .secure(false) //  개발 단계(http)에선 false, 운영(https)에선 true
                .sameSite("Lax")
                .maxAge(ACCESS_TOKEN_EXPIRE_TIME / 1000) // 밀리초를 초 단위로 변환
                .build();
    }

    public ResponseCookie createRefreshCookie(String refreshToken, boolean rememberMe) {
        // rememberMe 여부에 따라 쿠키 만료 시간 세팅
        long refreshMaxAge = rememberMe
                ? REFRESH_TOKEN_EXPIRE_TIME / 1000
                : -1; // -1은 브라우저 종료 시 삭제되는 '세션 쿠키'

        return org.springframework.http.ResponseCookie.from("refreshToken", refreshToken)
                .path("/")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .maxAge(refreshMaxAge)
                .build();
    }

    public boolean validateToken(String token)
    {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);

            return true;
        }
        catch (JwtException e)
        {
            log.warn("JWT 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    public Authentication getAuthentication(String token)
    {
        // 1. 토큰 자체 파싱
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        // 2. 권한 문자열 꺼내기
        String authClaim = claims.get("auth") != null ? claims.get("auth").toString() : "";

        if (authClaim.isBlank()) {
            throw new IllegalArgumentException("토큰에 권한 정보가 존재하지 않습니다.");
        }

        // 3. 쉼표(,)로 구분된 권한들을 스프링 시큐리티가 인식하는 GrantedAuthority 리스트로 가공
        List<SimpleGrantedAuthority> authorities = Arrays.stream(authClaim.split(","))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        // 4. DB 조회 없이 토큰 정보만으로 스프링 시큐리티의 가벼운 기본 User 객체를 조립
        UserDetails principal = new User(claims.getSubject(), "", authorities);

        // 5. 시큐리티 컨텍스트에 담길 최종 인증 토큰 리턴 (비밀번호 자리는 null 또는 빈 문자열로 처리)
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }


    public Authentication getAuthenticationFromUsername(String username) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

}



