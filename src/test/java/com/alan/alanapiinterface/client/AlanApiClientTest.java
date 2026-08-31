package com.alan.alanapiinterface.client;

import com.alan.alanapiclientsdk.client.AlanApiClient;
import com.alan.alanapiclientsdk.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

/**
 * AlanApiClientTest（客户端改由 alanapi-client-sdk 自动装配提供）
 *
 * @author ALan
 * @date 2026/8/30 16:24
 *
 */
@SpringBootTest
class AlanApiClientTest {

    @Resource
    private AlanApiClient alanApiClient;

    @Test
    void getNameByGet() {
        String result = alanApiClient.getNameByGet("alan");
        System.out.println(result);
    }

    @Test
    void getNameByPost() {
        String result = alanApiClient.getNameByPost("alan");
        System.out.println(result);
    }

    @Test
    void getUsernameByPost() {
        String result = alanApiClient.getUsernameByPost(new User("alan"));
        System.out.println(result);
    }

}
