package com.example.bank.auth;

import com.example.bank.common.BusinessException;
import com.example.bank.member.Member;
import com.example.bank.member.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/** Issues / verifies the HS256 access token used by the "Bearer token" demo. */
@Component
public class JwtProvider {

    public record Identity(Long memberId, Role role) {
    }

    private final SecretKey key;
    private final long expiresMinutes;

    public JwtProvider(@Value("${bank.jwt.secret}") String secret,
                       @Value("${bank.jwt.expires-minutes:30}") long expiresMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiresMinutes = expiresMinutes;
    }

    public long expiresInSeconds() {
        return expiresMinutes * 60;
    }

    public String issue(Member member) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(member.getId()))
                .claim("loginId", member.getLoginId())
                .claim("role", member.getRole().name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiresInSeconds() * 1000))
                .signWith(key)
                .compact();
    }

    public Identity parse(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return new Identity(Long.valueOf(c.getSubject()), Role.valueOf(c.get("role", String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "토큰이 유효하지 않거나 만료되었습니다.");
        }
    }
}
