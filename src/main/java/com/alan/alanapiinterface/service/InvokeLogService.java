package com.alan.alanapiinterface.service;

import cn.hutool.core.util.StrUtil;
import com.alan.alanapiinterface.common.BaseResponse;
import com.alan.alanapiinterface.mapper.InvokeLogMapper;
import com.alan.alanapiinterface.model.entity.InvokeLog;
import com.alan.alanapiinterface.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 接口调用日志服务：每次实际调用（验签、计数均通过）后写入 invoke_log 表
 * <p>
 * 日志写入失败只记录错误日志，绝不影响调用本身
 *
 * @author alan
 */
@Slf4j
@Service
public class InvokeLogService {

    /**
     * 请求参数最长保留长度
     */
    private static final int MAX_PARAM_LENGTH = 1024;

    /**
     * 响应数据最长保留长度
     */
    private static final int MAX_RESPONSE_LENGTH = 512;

    /**
     * 平台未登记接口在日志中使用的接口 id 占位值
     */
    private static final long UNREGISTERED_INTERFACE_ID = 0L;

    @Resource
    private InvokeLogMapper invokeLogMapper;

    /**
     * 记录调用成功的日志：从统一响应中解析业务是否成功及返回数据
     */
    public void recordResult(Long interfaceInfoId, User invokeUser, HttpServletRequest request,
                             String requestParams, Object result, long costTime) {
        boolean success = false;
        Object data = null;
        if (result instanceof ResponseEntity) {
            Object body = ((ResponseEntity<?>) result).getBody();
            if (body instanceof BaseResponse) {
                BaseResponse<?> baseResponse = (BaseResponse<?>) body;
                success = baseResponse.getCode() == 200;
                data = baseResponse.getData();
            }
        }
        insert(interfaceInfoId, invokeUser, request, requestParams, success, costTime, data);
    }

    /**
     * 记录调用异常的日志
     */
    public void recordError(Long interfaceInfoId, User invokeUser, HttpServletRequest request,
                            String requestParams, long costTime, Throwable error) {
        insert(interfaceInfoId, invokeUser, request, requestParams, false, costTime,
                error == null ? null : error.getMessage());
    }

    private void insert(Long interfaceInfoId, User invokeUser, HttpServletRequest request,
                        String requestParams, boolean success, long costTime, Object responseData) {
        try {
            InvokeLog invokeLog = new InvokeLog();
            invokeLog.setUserId(invokeUser.getId());
            invokeLog.setInterfaceInfoId(interfaceInfoId == null ? UNREGISTERED_INTERFACE_ID : interfaceInfoId);
            invokeLog.setRequestPath(request.getRequestURI());
            invokeLog.setRequestMethod(request.getMethod());
            invokeLog.setRequestParams(truncate(requestParams, MAX_PARAM_LENGTH));
            invokeLog.setResponseBody(truncate(String.valueOf(responseData), MAX_RESPONSE_LENGTH));
            invokeLog.setStatus(success ? 1 : 0);
            invokeLog.setCostTime(costTime);
            invokeLogMapper.insert(invokeLog);
        } catch (Exception e) {
            log.error("记录调用日志失败, uri: {}, userId: {}", request.getRequestURI(), invokeUser.getId(), e);
        }
    }

    private String truncate(String content, int maxLength) {
        if (StrUtil.isBlank(content)) {
            return null;
        }
        return content.length() <= maxLength ? content : content.substring(0, maxLength);
    }
}
