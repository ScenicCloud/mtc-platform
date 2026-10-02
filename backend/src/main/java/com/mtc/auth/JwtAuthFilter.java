package com.mtc.auth;

import com.mtc.common.ErrorCode;
import com.mtc.common.TraceIdHolder;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = jwtUtil.parse(token);
                Long userId = Long.valueOf(claims.getSubject());
                String username = claims.get("username", String.class);
                Date expiration = claims.getExpiration();

                // 判断是否过期
                if (expiration.before(new Date())) {
                    writeError(response, ErrorCode.TOKEN_EXPIRED);
                    return;
                }

                // 骨架阶段固定 ADMIN 角色
                var authorities = Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_ADMIN"));

                var authentication = new UsernamePasswordAuthenticationToken(
                        userId, null, authorities);
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (JwtException | IllegalArgumentException e) {
                log.warn("JWT 解析失败: {}", e.getMessage());
                writeError(response, ErrorCode.UNAUTHORIZED);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode)
            throws IOException {
        response.setStatus(errorCode == ErrorCode.TOKEN_EXPIRED
                ? 401 : 401);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("X-Trace-Id",
                TraceIdHolder.get() != null ? TraceIdHolder.get() : "");
        String json = String.format(
                "{\"code\":%d,\"message\":\"%s\",\"data\":null,\"traceId\":\"%s\"}",
                errorCode.getCode(), errorCode.getMessage(),
                TraceIdHolder.get() != null ? TraceIdHolder.get() : "");
        response.getWriter().write(json);
    }
}
