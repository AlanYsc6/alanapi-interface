package com.alan.alanapiinterface.service;

import com.alan.alanapiinterface.mapper.UserMapper;
import com.alan.alanapiinterface.model.entity.User;
import com.alan.alanapiinterface.utils.SignUtils;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户校验服务：通过查询 user 表验证调用方的 accessKey，并校验请求签名
 *
 * @author alan
 */
@Slf4j
@Service
public class UserValidateService {

    /**
     * 时间戳允许的最大误差（秒）：5 分钟
     */
    private static final long MAX_TIMESTAMP_DIFF = 5 * 60;

    /**
     * nonce 在 Redis 中的保存时长，略大于时间戳窗口即可
     */
    private static final Duration NONCE_EXPIRE = Duration.ofSeconds(MAX_TIMESTAMP_DIFF + 60);

    /**
     * nonce 在 Redis 中的 key 前缀（与 alanapi-backend 共用 Redis）
     */
    private static final String NONCE_KEY_PREFIX = "alan:interface:nonce:";

    @Resource
    private UserMapper userMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 校验请求签名
     * <p>
     * 1. 基本参数不能为空
     * 2. 通过 accessKey 查询 user 表，验证用户是否存在
     * 3. 校验时间戳，防止过期请求
     * 4. 校验 nonce，防止重放请求
     * 5. 用查到的 secretKey 重新计算签名并比对，防止参数被篡改
     *
     * @param accessKey 凭证
     * @param nonce     随机数
     * @param timestamp 时间戳（秒）
     * @param sign      签名
     * @param body      请求体内容
     * @return 校验通过返回调用用户（供调用计数等后续链路使用）；不通过返回 null
     */
    public User valid(String accessKey, String nonce, String timestamp, String sign, String body) {
        // 1. 基本参数不能为空
        if (StrUtil.hasBlank(accessKey, nonce, timestamp, sign, body)) {
            return null;
        }
        // 2. 通过 accessKey 查询 user 表，验证用户是否存在
        User invokeUser;
        try {
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getAccessKey, accessKey);
            invokeUser = userMapper.selectOne(queryWrapper);
        } catch (Exception e) {
            log.error("查询用户失败, accessKey: {}", accessKey, e);
            return null;
        }
        if (invokeUser == null) {
            log.warn("用户不存在, accessKey: {}", accessKey);
            return null;
        }
        // 账号被冻结（userStatus = 1）后开放调用立即失效
        if (invokeUser.getUserStatus() != null && invokeUser.getUserStatus() == 1) {
            log.warn("用户已被冻结, 拒绝调用, accessKey: {}", accessKey);
            return null;
        }
        // 3. 校验时间戳，与当前时间相差超过 5 分钟视为过期请求
        long diff;
        try {
            diff = Math.abs(System.currentTimeMillis() / 1000 - Long.parseLong(timestamp));
        } catch (NumberFormatException e) {
            return null;
        }
        if (diff > MAX_TIMESTAMP_DIFF) {
            log.warn("时间戳校验不通过, timestamp: {}", timestamp);
            return null;
        }
        // 4. 校验 nonce，防止时间窗口内的重放请求
        if (!tryAcquireNonce(nonce)) {
            log.warn("nonce 重复或 Redis 异常, 可能是重放请求, nonce: {}", nonce);
            return null;
        }
        // 5. 用查到的 secretKey 重新计算签名并比对（常量时间比较，防时序攻击）
        Map<String, String> params = new HashMap<>();
        params.put("accessKey", accessKey);
        params.put("body", body);
        params.put("nonce", nonce);
        params.put("timestamp", timestamp);
        String expectedSign = SignUtils.genSign(params, invokeUser.getSecretKey());
        if (!MessageDigest.isEqual(
                expectedSign.getBytes(StandardCharsets.UTF_8),
                sign.getBytes(StandardCharsets.UTF_8))) {
            return null;
        }
        return invokeUser;
    }

    /**
     * 登记并占用 nonce（SETNX + 过期时间，原子操作），nonce 已被使用则返回 false。
     * Redis 异常时拒绝请求，宁可暂时不可用也不放过重放请求。
     */
    private boolean tryAcquireNonce(String nonce) {
        try {
            Boolean first = stringRedisTemplate.opsForValue()
                    .setIfAbsent(NONCE_KEY_PREFIX + nonce, "1", NONCE_EXPIRE);
            return Boolean.TRUE.equals(first);
        } catch (Exception e) {
            log.error("nonce 校验异常, nonce: {}", nonce, e);
            return false;
        }
    }
}
