package com.mtc;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@MapperScan("com.mtc.**.mapper")
public class MtcApplication {

    public static void main(String[] args) {
        SpringApplication.run(MtcApplication.class, args);
    }
}
