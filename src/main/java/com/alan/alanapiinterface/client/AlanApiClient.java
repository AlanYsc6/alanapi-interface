package com.alan.alanapiinterface.client;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.alan.alanapiinterface.model.User;
import com.alan.alanapiinterface.utils.SignUtils;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.core.util.IdUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
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

    public String getNameByGet(String name) {
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("name", name);
        String result = HttpUtil.get("http://localhost:8123/api/name/", paramMap);
        log.info("paramMap:{},result:{}", paramMap, result);
        return result;
    }

    public String getNameByPost(@RequestParam String name) {
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("name", name);
        String result = HttpUtil.post("http://localhost:8123/api/name/", paramMap);
        log.info("paramMap:{},result:{}", paramMap, result);
        return result;
    }

    public String getUsernameByPost(@RequestBody User user) {
        String json = JSONUtil.toJsonStr(user);
        // 构造请求参数并计算签名（secretKey 只参与本地签名计算，一定不能随请求发送）
        Map<String, String> params = new HashMap<>();
        params.put("accessKey", accessKey);
        params.put("body", json);
        params.put("nonce", IdUtil.simpleUUID());
        params.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        params.put("sign", SignUtils.genSign(params, secretKey));
        HttpRequest request = HttpRequest.post("http://localhost:8123/api/name/user");
        params.forEach(request::header);
        String body = request.body(json)
            .execute().body();
        log.info("user:{},body:{}", user, body);
        return body;
    }

}

