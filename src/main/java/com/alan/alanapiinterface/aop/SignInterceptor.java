package com.alan.alanapiinterface.aop;

import com.alan.alanapiinterface.annotation.SignCheck;
import com.alan.alanapiinterface.common.ErrorCode;
import com.alan.alanapiinterface.common.ResultUtils;
import com.alan.alanapiinterface.model.entity.User;
import com.alan.alanapiinterface.service.InterfaceCountService;
import com.alan.alanapiinterface.service.InvokeLogService;
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
 * 未通过则直接返回 403，业务方法不再重复编写校验逻辑；
 * 验签通过后再执行调用计数（扣减 user_interface_info 的剩余次数），次数不足拒绝调用；
 * 实际调用完成后记录调用日志（invoke_log），失败调用同样留痕
 *
 * @author alan
 */
@Aspect
@Component
@Slf4j
public class SignInterceptor {

    @Resource
    private UserValidateService userValidateService;

    @Resource
    private InterfaceCountService interfaceCountService;

    @Resource
    private InvokeLogService invokeLogService;

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
        User invokeUser = userValidateService.valid(accessKey, nonce, timestamp, sign, body);
        if (invokeUser == null) {
            log.warn("签名校验未通过, uri: {}, accessKey: {}", request.getRequestURI(), accessKey);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResultUtils.error(ErrorCode.NO_AUTH_ERROR));
        }
        // 验签通过后计数，次数不足 / 已禁用的调用直接拒绝，不再进入业务方法
        Long interfaceInfoId = interfaceCountService.resolveInterfaceInfoId(request.getRequestURI());
        if (interfaceInfoId != null) {
            String countError = interfaceCountService.checkAndCount(request.getRequestURI(), invokeUser.getId(), interfaceInfoId);
            if (countError != null) {
                log.warn("调用计数拦截, uri: {}, userId: {}, 原因: {}", request.getRequestURI(), invokeUser.getId(), countError);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ResultUtils.error(ErrorCode.FORBIDDEN_ERROR, countError));
            }
        }
        // 实际调用并记录日志：正常返回按统一响应 code 判定成败，抛异常记为失败后原样抛出
        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            invokeLogService.recordResult(interfaceInfoId, invokeUser, request, body,
                    result, System.currentTimeMillis() - startTime);
            return result;
        } catch (Throwable e) {
            invokeLogService.recordError(interfaceInfoId, invokeUser, request, body,
                    System.currentTimeMillis() - startTime, e);
            throw e;
        }
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
