package com.example.bank.member;

import com.example.bank.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final int dormantDays;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder,
                         @Value("${bank.security.dormant-days:365}") int dormantDays) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.dormantDays = dormantDays;
    }

    public MemberDto.Response get(Long memberId) {
        return MemberDto.Response.from(find(memberId));
    }

    @Transactional
    public MemberDto.Response updateProfile(Long memberId, MemberDto.UpdateProfileRequest req) {
        Member member = find(memberId);
        if (!member.getEmail().equalsIgnoreCase(req.email()) && memberRepository.existsByEmail(req.email())) {
            throw BusinessException.conflict("이미 등록된 이메일입니다.");
        }
        member.updateProfile(req.email(), req.phone());   // dirty checking -> UPDATE (only changed columns)
        return MemberDto.Response.from(member);
    }

    @Transactional
    public void changePassword(Long memberId, MemberDto.ChangePasswordRequest req) {
        Member member = find(memberId);
        if (!passwordEncoder.matches(req.currentPassword(), member.getPassword())) {
            throw BusinessException.badRequest("현재 비밀번호가 올바르지 않습니다.");
        }
        member.changePassword(passwordEncoder.encode(req.newPassword()));
    }

    // ---- admin -----------------------------------------------------------

    /** QueryDSL dynamic search. */
    public Page<MemberDto.Summary> search(MemberDto.SearchCond cond, Pageable pageable) {
        return memberRepository.search(cond, pageable);
    }

    @Transactional
    public MemberDto.Response changeStatus(Long memberId, MemberStatus status) {
        Member member = find(memberId);
        if (member.getRole() == Role.ADMIN) {
            throw BusinessException.forbidden("관리자 계정의 상태는 변경할 수 없습니다.");
        }
        member.changeStatus(status);
        return MemberDto.Response.from(member);
    }

    /** JPQL bulk update: ACTIVE users without login for {@code dormant-days} become DORMANT. */
    @Transactional
    public int markDormant() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(dormantDays);
        return memberRepository.bulkChangeStatusByLastLogin(MemberStatus.ACTIVE, MemberStatus.DORMANT, threshold);
    }

    /** JPQL group by + constructor expression. */
    public List<MemberDto.StatusCount> countByStatus() {
        return memberRepository.countByStatus();
    }

    private Member find(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> BusinessException.notFound("회원을 찾을 수 없습니다."));
    }
}
