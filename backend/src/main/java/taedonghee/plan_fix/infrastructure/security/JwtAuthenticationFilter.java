package taedonghee.plan_fix.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import taedonghee.plan_fix.domain.user.UserModel;
import taedonghee.plan_fix.domain.user.UserRepository;
import taedonghee.plan_fix.domain.user.UserStatus;

import java.io.IOException;
import java.util.List;

/**
 * JWT 기반 요청 인증 필터
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    /**
     * Authorization 헤더 토큰 검증 및 SecurityContext 인증 저장
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(token);
        }
        filterChain.doFilter(request, response);
    }

    /**
     * JWT 인증 처리
     */
    private void authenticate(String token) {
        jwtTokenProvider.parse(token)
                .flatMap(claims -> userRepository.findById(claims.userId()))
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .ifPresent(this::setAuthentication);
    }

    /**
     * SecurityContext 인증 객체 저장
     */
    private void setAuthentication(UserModel user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getUsername(), user.getRole());
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * Bearer 토큰 추출
     */
    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorization.substring(BEARER_PREFIX.length());
    }
}
