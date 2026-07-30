package com.example.demo.Config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuerydslConfig {

    @PersistenceContext
    private EntityManager em; // 영속성 컨텍스트를 가져옴

    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(em); // JPAQueryFactory 를 스프링 빈으로 등록
    }
}