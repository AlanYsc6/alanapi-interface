package com.alan.alanapiinterface.client;

import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.alan.alanapiinterface.model.User;
import com.alan.alanapiinterface.utils.SignUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.HashMap;
import java.util.Map;

/**
 * 调用第三方接口的客户端
 *
 * @author ALan
 * @date 2026/8/30 16:11
 *
 */
@Slf4j
public class AlanApiClient {

    private String accessKey;

    private String secretKey;

    public AlanApiClient(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    /**
     * 构造签名请求头（所有接口都需要），secretKey 只参与本地签名计算，一定不能随请求发送
     */
    private Map<String, String> getHeaderMap(String body) {
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("accessKey", accessKey);
        headerMap.put("body", body);
        headerMap.put("nonce", IdUtil.simpleUUID());
        headerMap.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        headerMap.put("sign", SignUtils.genSign(headerMap, secretKey));
        return headerMap;
    }

    public String getNameByGet(String name) {
        HttpRequest request = HttpRequest.get("http://localhost:8123/api/name/")
            .form("name", name);
        getHeaderMap(name).forEach(request::header);
        String result = request.execute().body();
        log.info("name:{},result:{}", name, result);
        return result;
    }

    public String getNameByPost(String name) {
        HttpRequest request = HttpRequest.post("http://localhost:8123/api/name/")
            .form("name", name);
        getHeaderMap(name).forEach(request::header);
        String result = request.execute().body();
        log.info("name:{},result:{}", name, result);
        return result;
    }

    public String getUsernameByPost(@RequestBody User user) {
        String json = JSONUtil.toJsonStr(user);
        HttpRequest request = HttpRequest.post("http://localhost:8123/api/name/user");
        getHeaderMap(json).forEach(request::header);
        String body = request.body(json)
            .execute().body();
        log.info("user:{},body:{}", user, body);
        return body;
    }

}
