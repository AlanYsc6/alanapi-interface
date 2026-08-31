package com.alan.alanapiinterface.controller;

import cn.hutool.json.JSONUtil;
import com.alan.alanapiinterface.model.User;
import com.alan.alanapiinterface.service.UserValidateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.stream.Collectors;

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
    public ResponseEntity<String> getNameByGet(String name, HttpServletRequest request) {
        if (!validRequest(request, name)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无权限");
        }
        return ResponseEntity.ok("GET 你的名字是" + name);
    }

    @PostMapping("/")
    public ResponseEntity<String> getNameByPost(@RequestParam String name, HttpServletRequest request) {
        if (!validRequest(request, name)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无权限");
        }
        return ResponseEntity.ok("POST 你的名字是" + name);
    }

    @PostMapping("/user")
    public ResponseEntity<String> getUsernameByPost(HttpServletRequest request) throws IOException {
        // 直接读原始请求体参与校验，保证与客户端签名内容完全一致，校验通过后再解析
        String body = request.getReader().lines().collect(Collectors.joining());
        if (!validRequest(request, body)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无权限");
        }
        User user = JSONUtil.toBean(body, User.class);
        return ResponseEntity.ok("POST 用户名字是" + user.getUsername());
    }

    /**
     * 从请求头中取出签名信息并校验
     *
     * @param request 请求
     * @param body    参与签名的请求体内容（GET/POST 表单接口为参数值，JSON 接口为原始请求体）
     * @return true 校验通过
     */
    private boolean validRequest(HttpServletRequest request, String body) {
        String accessKey = request.getHeader("accessKey");
        String nonce = request.getHeader("nonce");
        String timestamp = request.getHeader("timestamp");
        String sign = request.getHeader("sign");
        return userValidateService.valid(accessKey, nonce, timestamp, sign, body);
    }
}
