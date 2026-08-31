package com.alan.alanapiinterface.controller;

import com.alan.alanapiinterface.model.User;
import com.alan.alanapiinterface.service.UserValidateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 名称API
 *
 * @author alan
 */
@RestController
@RequestMapping("/name")
public class NameController {

    @Resource
    private UserValidateService userValidateService;

    @GetMapping("/")
    public String getNameByGet(String name) {
        return "GET 你的名字是" + name;
    }

    @PostMapping("/")
    public String getNameByPost(@RequestParam String name) {
        return "POST 你的名字是" + name;
    }

    @PostMapping("/user")
    public ResponseEntity<String> getUsernameByPost(@RequestBody User user, HttpServletRequest request) {
        // 从请求头中获取签名信息
        String accessKey = request.getHeader("accessKey");
        String nonce = request.getHeader("nonce");
        String timestamp = request.getHeader("timestamp");
        String sign = request.getHeader("sign");
        String body = request.getHeader("body");
        // 通过查询 user 表校验签名，未通过则拒绝访问
        if (!userValidateService.valid(accessKey, nonce, timestamp, sign, body)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无权限");
        }
        return ResponseEntity.ok("POST 用户名字是" + user.getUsername());
    }
}
