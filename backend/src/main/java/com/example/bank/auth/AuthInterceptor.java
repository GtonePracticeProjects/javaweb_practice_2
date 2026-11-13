package com.example.bank.auth;

import com.example.bank.common.BusinessException;
import com.example.bank.member.Role;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Accepts EITHER credential:
 *  1) the login session cookie (browser apps)  2) "Authorization: Bearer &lt;jwt&gt;" (token clients)
 * Controllers read the logged-in id with {@code @RequestAttribute("memberId")}.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String SESSION_MEMBER_ID = "LOGIN_MEMBER_ID";
    public static final String SESSION_ROLE = "LOGIN_ROLE";

    private final JwtProvider jwtProvider;

    public AuthInterceptor(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // OPTIONS (CORS pre-flight) and the second pass of async requests (SSE) were already checked
        if ("OPTIONS".equals(request.getMethod()) || request.getDispatcherType() == DispatcherType.ASYNC) {
            return true;
        }

        Long memberId = null;
        Role role = null;

        HttpSession session = request.getSession(false);
        if (session != null) {
            memberId = (Long) session.getAttribute(SESSION_MEMBER_ID);
            role = (Role) session.getAttribute(SESSION_ROLE);
        }
        if (memberId == null) {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                JwtProvider.Identity identity = jwtProvider.parse(header.substring(7));
                memberId = identity.memberId();
                role = identity.role();
            }
        }
        if (memberId == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        if (request.getRequestURI().startsWith("/api/admin/") && role != Role.ADMIN) {
            throw BusinessException.forbidden("관리자만 접근할 수 있습니다.");
        }
        request.setAttribute("memberId", memberId);
        request.setAttribute("role", role);
        return true;
    }
}
