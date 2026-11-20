package com.dawnofmankind.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Runs Dawn of Mankind on its own. Open http://localhost:8080/dawn/ */
@SpringBootApplication(scanBasePackages = "com.dawnofmankind.web")
public class DawnApplication {
    public static void main(String[] args) {
        SpringApplication.run(DawnApplication.class, args);
    }
}
