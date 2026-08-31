package com.alan.alanapiinterface.controller;

import cn.hutool.json.JSONUtil;
import com.alan.alanapiinterface.annotation.SignCheck;
import com.alan.alanapiinterface.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 名称API（签名校验由 @SignCheck + SignInterceptor 统一处理）
 *
 * @author alan
 */
@RestController
@RequestMapping("/name")
public class NameController {

    @SignCheck(bodyParam = "name")
    @GetMapping("/")
    public ResponseEntity<String> getNameByGet(String name) {
        return ResponseEntity.ok("GET 你的名字是" + name);
    }

    @SignCheck(bodyParam = "name")
    @PostMapping("/")
    public ResponseEntity<String> getNameByPost(@RequestParam String name) {
        return ResponseEntity.ok("POST 你的名字是" + name);
    }

    @SignCheck
    @PostMapping("/user")
    public ResponseEntity<String> getUsernameByPost(@RequestBody String body) {
        User user = JSONUtil.toBean(body, User.class);
        return ResponseEntity.ok("POST 用户名字是" + user.getUsername());
    }
}
