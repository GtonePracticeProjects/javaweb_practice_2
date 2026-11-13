package com.example.bank.auth;

import com.example.bank.member.Member;
import com.example.bank.member.MemberDto;
import com.example.bank.member.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final MemberService memberService;

    private final JwtProvider jwtProvider;

    public AuthController(AuthService authService, MemberService memberService, JwtProvider jwtProvider) {
        this.authService = authService;
        this.memberService = memberService;
        this.jwtProvider = jwtProvider;
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds,
                                MemberDto.Response member) {
    }

    /** Token login: no session is created; the client sends "Authorization: Bearer ..." afterwards. */
    @PostMapping("/token")
    public TokenResponse token(@Valid @RequestBody MemberDto.LoginRequest req) {
        Member member = authService.login(req);
        return new TokenResponse(jwtProvider.issue(member), "Bearer", jwtProvider.expiresInSeconds(),
                MemberDto.Response.from(member));
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberDto.Response signup(@Valid @RequestBody MemberDto.SignupRequest req) {
        return MemberDto.Response.from(authService.signup(req));
    }

    @PostMapping("/login")
    public MemberDto.Response login(@Valid @RequestBody MemberDto.LoginRequest req, HttpServletRequest request) {
        Member member = authService.login(req);
        HttpSession old = request.getSession(false);
        if (old != null) {
            old.invalidate();                       // prevent session fixation
        }
        HttpSession session = request.getSession(true);
        session.setAttribute(AuthInterceptor.SESSION_MEMBER_ID, member.getId());
        session.setAttribute(AuthInterceptor.SESSION_ROLE, member.getRole());
        return MemberDto.Response.from(member);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpSession session) {
        session.invalidate();
    }

    @GetMapping("/me")
    public MemberDto.Response me(@RequestAttribute("memberId") Long memberId) {
        return memberService.get(memberId);
    }
}
