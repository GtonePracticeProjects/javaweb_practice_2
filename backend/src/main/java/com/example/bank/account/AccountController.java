package com.example.bank.account;

import com.example.bank.transaction.TransactionDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public Map<String, Object> list(@RequestAttribute("memberId") Long memberId) {
        List<AccountDto.Response> accounts = accountService.list(memberId);
        BigDecimal total = accountService.totalBalance(memberId);
        return Map.of("accounts", accounts, "totalBalance", total);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountDto.Response open(@RequestAttribute("memberId") Long memberId,
                                    @Valid @RequestBody AccountDto.OpenRequest req) {
        return accountService.open(memberId, req);
    }

    @GetMapping("/{id}")
    public AccountDto.Response get(@RequestAttribute("memberId") Long memberId, @PathVariable Long id) {
        return accountService.get(memberId, id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void close(@RequestAttribute("memberId") Long memberId, @PathVariable Long id) {
        accountService.close(memberId, id);
    }

    @GetMapping("/lookup")
    public AccountDto.Lookup lookup(@RequestParam String accountNumber) {
        return accountService.lookup(accountNumber);
    }

    @PostMapping("/{id}/deposit")
    public AccountDto.Response deposit(@RequestAttribute("memberId") Long memberId, @PathVariable Long id,
                                       @Valid @RequestBody AccountDto.AmountRequest req) {
        return accountService.deposit(memberId, id, req);
    }

    @PostMapping("/{id}/withdraw")
    public AccountDto.Response withdraw(@RequestAttribute("memberId") Long memberId, @PathVariable Long id,
                                        @Valid @RequestBody AccountDto.AmountRequest req) {
        return accountService.withdraw(memberId, id, req);
    }

    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void transfer(@RequestAttribute("memberId") Long memberId,
                         @Valid @RequestBody AccountDto.TransferRequest req) {
        accountService.transfer(memberId, req);
    }

    // ---- history / reports (each uses a different query style) -----------------

    /** Spring Data derived query + Pageable */
    @GetMapping("/{id}/transactions")
    public Page<TransactionDto.Response> history(@RequestAttribute("memberId") Long memberId, @PathVariable Long id,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        return accountService.history(memberId, id, page, Math.min(size, 100));
    }

    /** JPQL constructor expression */
    @GetMapping("/{id}/summary")
    public List<TransactionDto.TypeSum> summary(@RequestAttribute("memberId") Long memberId, @PathVariable Long id) {
        return accountService.summary(memberId, id);
    }

    /** Native SQL */
    @GetMapping("/{id}/monthly")
    public List<Map<String, Object>> monthly(@RequestAttribute("memberId") Long memberId, @PathVariable Long id) {
        // interface projections are proxies; copy into plain maps for JSON
        return accountService.monthly(memberId, id).stream()
                .map(v -> Map.<String, Object>of(
                        "year", v.getYr(), "month", v.getMon(), "type", v.getTxType(),
                        "total", v.getTotalAmount(), "count", v.getCnt()))
                .toList();
    }
}
