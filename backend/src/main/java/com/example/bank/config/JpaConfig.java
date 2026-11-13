package com.example.bank.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing   // enables @CreatedDate / @LastModifiedDate
public class JpaConfig {

    @PersistenceContext
    private EntityManager em;

    /** QueryDSL entry point. Shares the transaction-bound EntityManager proxy. */
    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(em);
    }
}
