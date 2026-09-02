package com.alan.alanapiinterface.aop;

import com.alan.alanapiinterface.annotation.SignCheck;
import com.alan.alanapiinterface.common.ErrorCode;
import com.alan.alanapiinterface.common.ResultUtils;
import com.alan.alanapiinterface.service.UserValidateService;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.stream.Collectors;

/**
 * 签名校验拦截切面：拦截标注了 @SignCheck 的接口，统一从请求头中取出签名信息并验签，
 * 未通过则直接返回 403，业务方法不再重复编写校验逻辑
 *
 * @author alan
 */
@Aspect
@Component
@Slf4j
public class SignInterceptor {

    @Resource
    private UserValidateService userValidateService;

    @Around("@annotation(signCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, SignCheck signCheck) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        // 参与验签的 body：表单接口取指定参数值，JSON 接口取原始请求体
        String body = StrUtil.isNotBlank(signCheck.bodyParam())
                ? request.getParameter(signCheck.bodyParam())
                : readBody(request);
        String accessKey = request.getHeader("accessKey");
        String nonce = request.getHeader("nonce");
        String timestamp = request.getHeader("timestamp");
        String sign = request.getHeader("sign");
        if (!userValidateService.valid(accessKey, nonce, timestamp, sign, body)) {
            log.warn("签名校验未通过, uri: {}, accessKey: {}", request.getRequestURI(), accessKey);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResultUtils.error(ErrorCode.NO_AUTH_ERROR));
        }
        return joinPoint.proceed();
    }

    /**
     * 读取原始请求体（配合 RequestBodyCachingFilter 支持重复读取）
     */
    private String readBody(HttpServletRequest request) {
        try {
            BufferedReader reader = request.getReader();
            return reader.lines().collect(Collectors.joining());
        } catch (IOException e) {
            log.error("读取请求体失败, uri: {}", request.getRequestURI(), e);
            return null;
        }
    }
}
