package com.dejanvuk.springcloud.configserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"spring.profiles.active=native"})
class ConfigserverApplicationTests {

    @Test
    void contextLoads() {
    }

}
