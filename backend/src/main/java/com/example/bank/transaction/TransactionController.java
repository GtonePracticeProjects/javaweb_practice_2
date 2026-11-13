package com.example.bank.transaction;

import com.example.bank.account.AccountService;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final AccountService accountService;

    public TransactionController(AccountService accountService) {
        this.accountService = accountService;
    }

    /** Same search, returned as a downloadable CSV file. */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestAttribute("memberId") Long memberId,
                                         TransactionDto.SearchCond cond) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("transactions.csv", StandardCharsets.UTF_8).build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(accountService.exportCsv(memberId, cond));
    }

    /** QueryDSL dynamic search (all query params optional). */
    @GetMapping
    public Page<TransactionDto.Response> search(@RequestAttribute("memberId") Long memberId,
                                                TransactionDto.SearchCond cond,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        return accountService.search(memberId, cond, page, Math.min(size, 100));
    }
}
