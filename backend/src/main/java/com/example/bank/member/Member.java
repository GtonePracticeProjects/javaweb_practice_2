package com.example.bank.member;

import com.example.bank.account.Account;
import com.example.bank.common.BaseTimeEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Customer login account ("계정 정보").
 * {@code @DynamicUpdate}: Hibernate only puts changed columns into the UPDATE statement.
 */
@Entity
@Table(name = "member", indexes = {
        @Index(name = "idx_member_status_last_login", columnList = "status, last_login_at")
})
@DynamicUpdate
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_id", nullable = false, unique = true, length = 20)
    private String loginId;

    @Column(nullable = false, length = 100)
    private String password;          // BCrypt hash

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MemberStatus status = MemberStatus.ACTIVE;

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @OneToMany(mappedBy = "member")
    private List<Account> accounts = new ArrayList<>();

    protected Member() {
    }

    public Member(String loginId, String password, String name, String email, String phone, Role role) {
        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
    }

    // ---- domain behaviour -------------------------------------------------

    public void recordLoginSuccess() {
        this.failedLoginCount = 0;
        this.lastLoginAt = LocalDateTime.now();
        if (this.status == MemberStatus.DORMANT) {
            this.status = MemberStatus.ACTIVE;   // dormant users are re-activated by logging in
        }
    }

    /** @return true when this failure locked the account */
    public boolean recordLoginFailure(int maxFailures) {
        this.failedLoginCount++;
        if (this.failedLoginCount >= maxFailures) {
            this.status = MemberStatus.LOCKED;
            return true;
        }
        return false;
    }

    public void changeStatus(MemberStatus status) {
        this.status = status;
        if (status == MemberStatus.ACTIVE) {
            this.failedLoginCount = 0;
        }
    }

    public void updateProfile(String email, String phone) {
        this.email = email;
        this.phone = phone;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    // ---- getters ----------------------------------------------------------

    public Long getId() { return id; }
    public String getLoginId() { return loginId; }
    public String getPassword() { return password; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public Role getRole() { return role; }
    public MemberStatus getStatus() { return status; }
    public int getFailedLoginCount() { return failedLoginCount; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public List<Account> getAccounts() { return accounts; }
}
