package com.example.bank.member;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class MemberDto {

    private MemberDto() {
    }

    public record SignupRequest(
            @NotBlank @Size(min = 4, max = 20) @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "영문/숫자/_ 만 가능") String loginId,
            @NotBlank @Size(min = 8, max = 30) String password,
            @NotBlank @Size(max = 50) String name,
            @NotBlank @Email String email,
            @Pattern(regexp = "^[0-9\\-]{9,15}$", message = "전화번호 형식이 올바르지 않습니다") String phone) {
    }

    public record LoginRequest(@NotBlank String loginId, @NotBlank String password) {
    }

    public record UpdateProfileRequest(@NotBlank @Email String email,
                                       @Pattern(regexp = "^[0-9\\-]{9,15}$") String phone) {
    }

    public record ChangePasswordRequest(@NotBlank String currentPassword,
                                        @NotBlank @Size(min = 8, max = 30) String newPassword) {
    }

    public record ChangeStatusRequest(@NotNull MemberStatus status) {
    }

    public record Response(Long id, String loginId, String name, String email, String phone,
                           Role role, MemberStatus status, LocalDateTime lastLoginAt, LocalDateTime createdAt) {
        public static Response from(Member m) {
            return new Response(m.getId(), m.getLoginId(), m.getName(), m.getEmail(), m.getPhone(),
                    m.getRole(), m.getStatus(), m.getLastLoginAt(), m.getCreatedAt());
        }
    }

    /** Admin list row built by a QueryDSL constructor projection (member + aggregated accounts). */
    public record Summary(Long id, String loginId, String name, String email, MemberStatus status,
                          LocalDateTime createdAt, Long accountCount, BigDecimal totalBalance) {
    }

    /** Dynamic search condition for QueryDSL (every field optional). */
    public record SearchCond(String loginId, String name, MemberStatus status,
                             BigDecimal minTotalBalance, LocalDate createdFrom, LocalDate createdTo) {
    }

    /** JPQL constructor-expression result: "select new ...StatusCount(m.status, count(m))". */
    public record StatusCount(MemberStatus status, Long count) {
    }
}
