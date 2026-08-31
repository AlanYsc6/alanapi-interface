package com.alan.alanapiinterface.client;

import cn.hutool.core.util.StrUtil;
import com.alan.alanapiinterface.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * AlanApiClientTest
 *
 * @author ALan
 * @date 2026/8/30 16:24
 *
 */
class AlanApiClientTest {

    // 凭证从 application.yml 的 sdk 配置读取（user 表中注册用户的 accessKey / secretKey），避免密钥入库
    // 未配置时测试自动跳过而不是报错
    private final AlanApiClient alanApiClient = createClient();

    private AlanApiClient createClient() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        Properties props = yaml.getObject();
        String accessKey = props == null ? null : props.getProperty("sdk.access-key");
        String secretKey = props == null ? null : props.getProperty("sdk.secret-key");
        assumeTrue(StrUtil.isNotBlank(accessKey) && StrUtil.isNotBlank(secretKey),
                "application.yml 未配置 sdk.access-key / sdk.secret-key，跳过测试");
        return new AlanApiClient(accessKey, secretKey);
    }

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