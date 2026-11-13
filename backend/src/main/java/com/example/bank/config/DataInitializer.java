package com.example.bank.config;

import com.example.bank.account.AccountDto;
import com.example.bank.account.AccountService;
import com.example.bank.account.AccountType;
import com.example.bank.member.Member;
import com.example.bank.member.MemberRepository;
import com.example.bank.member.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Demo data (only when the member table is empty):
 *   admin / Admin1234!   (ADMIN)
 *   hong  / Hong1234!    (USER, 2 accounts)
 *   kim   / Kim12345!    (USER, 1 account)
 */
@Component
@ConditionalOnProperty(name = "bank.seed.enabled", havingValue = "true")
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final MemberRepository memberRepository;
    private final AccountService accountService;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;

    public DataInitializer(MemberRepository memberRepository, AccountService accountService,
                           PasswordEncoder passwordEncoder, JdbcTemplate jdbc) {
        this.memberRepository = memberRepository;
        this.accountService = accountService;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (memberRepository.count() > 0) {
            return;
        }
        memberRepository.save(new Member("admin", passwordEncoder.encode("Admin1234!"),
                "관리자", "admin@bank.example", "010-0000-0000", Role.ADMIN));
        Member hong = memberRepository.save(new Member("hong", passwordEncoder.encode("Hong1234!"),
                "홍길동", "hong@bank.example", "010-1111-2222", Role.USER));
        Member kim = memberRepository.save(new Member("kim", passwordEncoder.encode("Kim12345!"),
                "김영희", "kim@bank.example", "010-3333-4444", Role.USER));

        var hongChecking = accountService.open(hong.getId(),
                new AccountDto.OpenRequest(AccountType.CHECKING, "생활비 통장", new BigDecimal("3000000")));
        var hongSavings = accountService.open(hong.getId(),
                new AccountDto.OpenRequest(AccountType.SAVINGS, "비상금", new BigDecimal("10000000")));
        var kimChecking = accountService.open(kim.getId(),
                new AccountDto.OpenRequest(AccountType.CHECKING, "월급 통장", new BigDecimal("500000")));

        accountService.withdraw(hong.getId(), hongChecking.id(), new AccountDto.AmountRequest(new BigDecimal("120000"), "ATM 출금"));
        accountService.transfer(hong.getId(), new AccountDto.TransferRequest(
                hongChecking.accountNumber(), kimChecking.accountNumber(), new BigDecimal("250000"), "점심값 정산"));
        accountService.transfer(kim.getId(), new AccountDto.TransferRequest(
                kimChecking.accountNumber(), hongChecking.accountNumber(), new BigDecimal("50000"), "일부 환급"));
        accountService.deposit(hong.getId(), hongSavings.id(), new AccountDto.AmountRequest(new BigDecimal("1000000"), "추가 적립"));

        // back-date a few ledger rows so the monthly reports have more than one month
        jdbc.update("update account_transaction set created_at = date_sub(created_at, interval 35 day) where id in (1, 3)");
        jdbc.update("update account_transaction set created_at = date_sub(created_at, interval 70 day) where id = 2");
        log.info("Seed data created. Logins: admin/Admin1234!, hong/Hong1234!, kim/Kim12345!");
    }
}
