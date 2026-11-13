package com.example.bank.member;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Custom fragment implemented with QueryDSL (see MemberRepositoryImpl). */
public interface MemberRepositoryCustom {

    Page<MemberDto.Summary> search(MemberDto.SearchCond cond, Pageable pageable);
}
