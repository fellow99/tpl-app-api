package org.fellow99.tpl.appapi;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("org.fellow99.tpl.appapi.mapper")
public class TplAppApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TplAppApiApplication.class, args);
    }
}
