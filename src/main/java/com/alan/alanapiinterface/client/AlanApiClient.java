package com.alan.alanapiinterface.client;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.alan.alanapiinterface.model.User;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.HashMap;

/**
 * 调用第三方接口的客户端
 *
 * @author ALan
 * @date 2026/8/30 16:11
 *
 */
@Slf4j
public class AlanApiClient {

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
        String body = HttpRequest.post("http://localhost:8123/api/name/user")
            .body(json)
            .execute().body();
        log.info("user:{},body:{}", user, body);
        return body;
    }

}

