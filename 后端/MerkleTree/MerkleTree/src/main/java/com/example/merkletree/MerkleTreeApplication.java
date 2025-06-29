package com.example.merkletree;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement // 添加事务支持
public class MerkleTreeApplication {

    public static void main(String[] args) {
        SpringApplication.run(MerkleTreeApplication.class, args);
    }

}
