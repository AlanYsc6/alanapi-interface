package com.alan.alanapiinterface.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

/**
 * 签名工具（HMAC-SHA256 签名认证）
 * <p>
 * 签名规则：
 * 1. 参与签名的参数为 accessKey、body、nonce、timestamp（sign 与 secretKey 本身不参与）
 * 2. 参数按 key 的 ASCII 字典序排序后拼接为 k1=v1&k2=v2 形式的规范串
 * 3. 以 secretKey 为密钥对规范串计算 HMAC-SHA256，输出小写十六进制
 * <p>
 * secretKey 只用于本地计算签名，绝不随请求发送
 *
 * @author alan
 */
public class SignUtils {

    /**
     * 生成签名
     *
     * @param params    参与签名的请求参数
     * @param secretKey 密钥
     * @return 签名（小写十六进制）
     */
    public static String genSign(Map<String, String> params, String secretKey) {
        if (params == null || StrUtil.isBlank(secretKey)) {
            throw new IllegalArgumentException("签名参数不完整：params 和 secretKey 均不能为空（请检查 accessKey/secretKey 是否正确配置）");
        }
        String canonicalString = buildCanonicalString(params);
        HMac hMac = new HMac(HmacAlgorithm.HmacSHA256, secretKey.getBytes(StandardCharsets.UTF_8));
        return hMac.digestHex(canonicalString);
    }

    /**
     * 构造待签名的规范串：按 key 字典序排序，拼接为 k1=v1&k2=v2 形式
     *
     * @param params 请求参数
     * @return 规范串
     */
    public static String buildCanonicalString(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : new TreeMap<>(params).entrySet()) {
            String value = entry.getValue();
            if (value == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(entry.getKey()).append('=').append(value);
        }
        return sb.toString();
    }
}
