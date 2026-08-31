package com.alan.alanapiinterface;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.alan.alanapiinterface.mapper")
public class AlanapiInterfaceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlanapiInterfaceApplication.class, args);
    }

}
