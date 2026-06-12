package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class TransactionConfig {
   // @EnableTransactionManagement가 트랜젝션 관리를 활성화 해줍니다.
}
