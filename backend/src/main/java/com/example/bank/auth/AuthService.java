package com.example.bank.auth;

import com.example.bank.common.BusinessException;
import com.example.bank.member.Member;
import com.example.bank.member.MemberDto;
import com.example.bank.member.MemberRepository;
import com.example.bank.member.MemberStatus;
import com.example.bank.member.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final int maxLoginFailures;

    public AuthService(MemberRepository memberRepository, PasswordEncoder passwordEncoder,
                       @Value("${bank.security.max-login-failures:5}") int maxLoginFailures) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.maxLoginFailures = maxLoginFailures;
    }

    public Member signup(MemberDto.SignupRequest req) {
        if (memberRepository.existsByLoginId(req.loginId())) {
            throw BusinessException.conflict("이미 사용 중인 아이디입니다.");
        }
        if (memberRepository.existsByEmail(req.email())) {
            throw BusinessException.conflict("이미 등록된 이메일입니다.");
        }
        return memberRepository.save(new Member(req.loginId(), passwordEncoder.encode(req.password()),
                req.name(), req.email(), req.phone(), Role.USER));
    }

    /**
     * noRollbackFor: when the password is wrong we throw, but the incremented failure counter
     * (and the LOCKED status) must still be committed.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public Member login(MemberDto.LoginRequest req) {
        Member member = memberRepository.findByLoginId(req.loginId())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."));

        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        if (member.getStatus() == MemberStatus.LOCKED) {
            throw BusinessException.forbidden("잠긴 계정입니다. 관리자에게 문의하세요.");
        }
        if (!passwordEncoder.matches(req.password(), member.getPassword())) {
            boolean locked = member.recordLoginFailure(maxLoginFailures);
            throw new BusinessException(HttpStatus.UNAUTHORIZED, locked
                    ? "비밀번호를 " + maxLoginFailures + "회 틀려 계정이 잠겼습니다."
                    : "아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        member.recordLoginSuccess();
        return member;
    }
}
