package com.pinfan;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 拼饭吧后端启动类
 */
@Slf4j
@SpringBootApplication
public class PinfanBackendApplication {

    public static void main(String[] args) {

        SpringApplication.run(PinfanBackendApplication.class, args);
        log.info("拼饭吧项目后端启动..地址：http://localhost:8080");
    }

}
