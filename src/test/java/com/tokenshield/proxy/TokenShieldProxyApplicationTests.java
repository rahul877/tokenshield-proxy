package com.tokenshield.proxy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.r2dbc.url=r2dbc:h2:mem:///tokenshield?options=DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379"
})
class TokenShieldProxyApplicationTests {

    @Test
    void contextLoads() {
    }
}
