package com.example.bank.admin;

import com.example.bank.account.AccountDto;
import com.example.bank.member.MemberDto;
import com.example.bank.member.MemberService;
import com.example.bank.transaction.TransactionDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Everything under /api/admin/** requires Role.ADMIN (checked in AuthInterceptor). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final MemberService memberService;
    private final StatisticsService statisticsService;

    public AdminController(MemberService memberService, StatisticsService statisticsService) {
        this.memberService = memberService;
        this.statisticsService = statisticsService;
    }

    /** QueryDSL dynamic search + paging */
    @GetMapping("/members")
    public Page<MemberDto.Summary> members(MemberDto.SearchCond cond,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        return memberService.search(cond, PageRequest.of(page, Math.min(size, 100)));
    }

    @PatchMapping("/members/{id}/status")
    public MemberDto.Response changeStatus(@PathVariable Long id,
                                           @Valid @RequestBody MemberDto.ChangeStatusRequest req) {
        return memberService.changeStatus(id, req.status());
    }

    /** JPQL bulk update */
    @PostMapping("/members/dormant-batch")
    public Map<String, Integer> dormantBatch() {
        return Map.of("updated", memberService.markDormant());
    }

    /** native SQL */
    @GetMapping("/stats/accounts")
    public List<Map<String, Object>> accountStats() {
        return statisticsService.accountsByType().stream()
                .map(v -> Map.<String, Object>of("accountType", v.getAccountType(),
                        "accountCount", v.getAccountCount(), "totalBalance", v.getTotalBalance()))
                .toList();
    }

    /** JPQL constructor expression */
    @GetMapping("/stats/members")
    public List<MemberDto.StatusCount> memberStats() {
        return statisticsService.membersByStatus();
    }

    /** QueryDSL group by */
    @GetMapping("/stats/transactions")
    public List<TransactionDto.MonthlyStat> transactionStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return statisticsService.monthlyTransactions(from, to);
    }
}
