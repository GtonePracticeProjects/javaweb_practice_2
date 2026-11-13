package com.example.bank.member;

public enum MemberStatus {
    ACTIVE,      // normal
    LOCKED,      // too many failed logins / locked by admin
    DORMANT,     // no login for a long time (batch)
    WITHDRAWN    // closed
}
