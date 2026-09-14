package ru.donskikh.incidenthub.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import ru.donskikh.incidenthub.auth.application.AccessTokenIssuer;
import ru.donskikh.incidenthub.identity.User;
import ru.donskikh.incidenthub.identity.UserRole;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

public class JwtTokenService implements AccessTokenIssuer {

    private static final String EMAIL_CLAIM = "email";
    private static final String ROLE_CLAIM = "role";

    private final JwtProperties properties;
    private final Clock clock;
    private final SecretKey signingKey;

    public JwtTokenService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
    }

    @Override
    public IssuedAccessToken issue(User user) {
        return issue(
                new AuthenticatedUser(user.getId(), user.getEmail(), user.getRole()),
                clock.instant()
        );
    }

    IssuedAccessToken issue(AuthenticatedUser user, Instant issuedAt) {
        Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
        String token = Jwts.builder()
                .subject(user.userId().toString())
                .claim(EMAIL_CLAIM, user.email())
                .claim(ROLE_CLAIM, user.role().name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
        return new IssuedAccessToken(token, properties.accessTokenTtl().toSeconds());
    }

    public AuthenticatedUser parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String subject = requireClaim(claims.getSubject(), "sub");
        String email = requireClaim(claims.get(EMAIL_CLAIM, String.class), EMAIL_CLAIM);
        String role = requireClaim(claims.get(ROLE_CLAIM, String.class), ROLE_CLAIM);

        return new AuthenticatedUser(
                Long.valueOf(subject),
                email,
                UserRole.valueOf(role)
        );
    }

    private static String requireClaim(String value, String claimName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("JWT claim is missing: " + claimName);
        }
        return value;
    }
}
