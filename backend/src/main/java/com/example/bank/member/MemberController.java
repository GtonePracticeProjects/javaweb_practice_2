package com.example.bank.member;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members/me")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PutMapping
    public MemberDto.Response updateProfile(@RequestAttribute("memberId") Long memberId,
                                            @Valid @RequestBody MemberDto.UpdateProfileRequest req) {
        return memberService.updateProfile(memberId, req);
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@RequestAttribute("memberId") Long memberId,
                               @Valid @RequestBody MemberDto.ChangePasswordRequest req) {
        memberService.changePassword(memberId, req);
    }
}
