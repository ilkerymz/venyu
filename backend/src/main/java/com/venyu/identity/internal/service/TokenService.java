package com.venyu.identity.internal.service;

import com.venyu.identity.internal.config.JwtProperties;
import com.venyu.identity.internal.domain.RefreshToken;
import com.venyu.identity.internal.domain.Role;
import com.venyu.identity.internal.domain.User;
import com.venyu.identity.internal.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class TokenService {


    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;
    private final RefreshTokenRepository refreshTokens;

    public IssuedTokens issueTokens(User user) {
        return new IssuedTokens(
                issueAccessToken(user),
                issueRefreshToken(user),
                properties.accessTokenTtl().toSeconds());
    }

    private String issueAccessToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(properties.accessTokenTtl()))
                .claim("roles", user.getRoles().stream().map(Role::name).toList())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private String issueRefreshToken(User user) {
        String rawToken = SecureTokens.generate();
        Instant expiresAt = Instant.now().plus(properties.refreshTokenTtl());
        refreshTokens.save(RefreshToken.issue(user.getId(), SecureTokens.hash(rawToken), expiresAt));
        return rawToken;
    }

    static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public record IssuedTokens(String accessToken, String refreshToken, long expiresInSeconds) {
    }
}