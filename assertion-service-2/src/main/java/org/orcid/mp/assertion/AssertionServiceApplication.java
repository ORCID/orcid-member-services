package org.orcid.mp.assertion;

import io.mongock.runner.springboot.EnableMongock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableMongock
@SpringBootApplication
public class AssertionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssertionServiceApplication.class, args);
    }

}
