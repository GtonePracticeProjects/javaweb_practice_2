package com.example.bank.account;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class AccountDto {

    private AccountDto() {
    }

    public record OpenRequest(@NotNull AccountType accountType,
                              @Size(max = 50) String alias,
                              @DecimalMin("0") BigDecimal initialDeposit) {
    }

    public record AmountRequest(@NotNull @DecimalMin(value = "0.01", message = "금액은 0보다 커야 합니다") BigDecimal amount,
                                @Size(max = 100) String memo) {
    }

    public record TransferRequest(@NotBlank String fromAccountNumber,
                                  @NotBlank String toAccountNumber,
                                  @NotNull @DecimalMin("0.01") BigDecimal amount,
                                  @Size(max = 100) String memo) {
    }

    public record Response(Long id, String accountNumber, AccountType accountType, String alias,
                           BigDecimal balance, AccountStatus status, LocalDateTime createdAt) {
        public static Response from(Account a) {
            return new Response(a.getId(), a.getAccountNumber(), a.getAccountType(), a.getAlias(),
                    a.getBalance(), a.getStatus(), a.getCreatedAt());
        }
    }

    /** Recipient look-up result (name is masked: 홍*동). */
    public record Lookup(String accountNumber, String ownerName) {
    }

    /** Spring Data interface projection for a NATIVE query (aliases must match getter names). */
    public interface TypeSummary {
        String getAccountType();

        Long getAccountCount();

        BigDecimal getTotalBalance();
    }
}
