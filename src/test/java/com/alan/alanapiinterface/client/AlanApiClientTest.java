package com.alan.alanapiinterface.client;

import com.alan.alanapiinterface.model.User;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * AlanApiClientTest
 *
 * @author ALan
 * @date 2026/8/30 16:24
 *
 */
class AlanApiClientTest {

    // 凭证从环境变量读取（user 表中实际存在的 accessKey / secretKey），避免真实密钥入库
    // 运行前设置：ACCESS_KEY / SECRET_KEY
    private final AlanApiClient alanApiClient = new AlanApiClient(
            System.getenv("ACCESS_KEY"), System.getenv("SECRET_KEY"));

    @Test
    void getNameByGet() {
        String nameByGet = alanApiClient.getNameByGet("alan");
        System.out.println(nameByGet);
    }

    @Test
    void getNameByPost() {
        String nameByGet = alanApiClient.getNameByPost("alan");
        System.out.println(nameByGet);
    }

    @Test
    void getUsernameByPost() {
        String nameByGet = alanApiClient.getUsernameByPost(new User("alan"));
        System.out.println(nameByGet);
    }

}