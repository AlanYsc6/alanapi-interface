package com.alan.alanapiinterface.client;

import com.alan.alanapiinterface.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * AlanApiClientTest
 *
 * @author ALan
 * @date 2026/8/30 16:24
 *
 */
@SpringBootTest
class AlanApiClientTest {

    // 凭证从 application.yml 的 sdk 配置读取（user 表中注册用户的 accessKey / secretKey），避免密钥入库
    @Value("${sdk.access-key}")
    private String accessKey;

    @Value("${sdk.secret-key}")
    private String secretKey;

    @Test
    void getNameByGet() {
        String result = new AlanApiClient(accessKey, secretKey).getNameByGet("alan");
        System.out.println(result);
    }

    @Test
    void getNameByPost() {
        String result = new AlanApiClient(accessKey, secretKey).getNameByPost("alan");
        System.out.println(result);
    }

    @Test
    void getUsernameByPost() {
        String result = new AlanApiClient(accessKey, secretKey).getUsernameByPost(new User("alan"));
        System.out.println(result);
    }

}
