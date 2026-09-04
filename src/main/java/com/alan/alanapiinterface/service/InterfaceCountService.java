package com.alan.alanapiinterface.service;

import cn.hutool.core.util.StrUtil;
import com.alan.alanapiinterface.mapper.InterfaceInfoMapper;
import com.alan.alanapiinterface.mapper.UserInterfaceInfoMapper;
import com.alan.alanapiinterface.model.entity.InterfaceInfo;
import com.alan.alanapiinterface.model.entity.UserInterfaceInfo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 接口调用计数服务：在签名校验通过后、业务执行前调用
 * <p>
 * 计数语义（与开放平台一致）：
 * 1. 请求路径能匹配到平台登记的接口时，用户必须拥有可用调用次数才能调用
 * 2. 用户首次调用某接口时自动开通并赠送初始次数，避免每次分配都要管理员手工操作
 * 3. 平台未登记的接口（本地调试接口等）不计次、不拦截
 *
 * @author alan
 */
@Slf4j
@Service
public class InterfaceCountService {

    /**
     * 首次调用自动开通时赠送的初始调用次数
     */
    private static final int DEFAULT_INIT_LEFT_NUM = 50;

    /**
     * 请求路径 -> 接口 id 匹配结果的缓存时长（毫秒），含"未登记"的否定缓存，
     * 避免每次调用都查 interface_info 表；新发布接口最迟 60 秒后生效计数
     */
    private static final long PATH_CACHE_TTL_MS = 60 * 1000L;

    @Resource
    private InterfaceInfoMapper interfaceInfoMapper;

    @Resource
    private UserInterfaceInfoMapper userInterfaceInfoMapper;

    /**
     * 请求路径 -> 接口匹配结果缓存
     */
    private final Map<String, PathCacheEntry> pathCache = new ConcurrentHashMap<>();

    /**
     * 校验并扣除一次调用次数
     *
     * @param requestUri      请求路径（仅用于日志）
     * @param userId          调用用户 id
     * @param interfaceInfoId 接口 id（由 {@link #resolveInterfaceInfoId(String)} 解析）
     * @return null 表示放行；非 null 为拒绝调用的原因
     */
    public String checkAndCount(String requestUri, long userId, long interfaceInfoId) {
        try {
            ensureRecord(userId, interfaceInfoId);
            if (userInterfaceInfoMapper.countOnce(userId, interfaceInfoId) > 0) {
                return null;
            }
            // 条件更新未命中：区分禁用与次数用尽，给出可定位的提示
            UserInterfaceInfo record = getRecord(userId, interfaceInfoId);
            if (record == null) {
                return "调用次数记录不存在，请联系管理员";
            }
            if (record.getStatus() != null && record.getStatus() == 1) {
                return "该接口调用已被禁用，请联系管理员";
            }
            return "剩余调用次数不足，请联系管理员分配";
        } catch (Exception e) {
            // 与验签同样的兜底策略：计数链路异常时宁可暂时不可用，也不放过未计数的调用
            log.error("接口调用计数失败, uri: {}, userId: {}", requestUri, userId, e);
            return "调用计数服务异常，请稍后重试";
        }
    }

    /**
     * 保证用户对接口存在调用记录：不存在则自动开通（赠送初始次数）。
     * 唯一索引并发冲突时说明记录已被并发请求创建，直接放行即可
     */
    private void ensureRecord(long userId, long interfaceInfoId) {
        if (getRecord(userId, interfaceInfoId) != null) {
            return;
        }
        UserInterfaceInfo record = new UserInterfaceInfo();
        record.setUserId(userId);
        record.setInterfaceInfoId(interfaceInfoId);
        record.setTotalNum(0);
        record.setLeftNum(DEFAULT_INIT_LEFT_NUM);
        record.setStatus(0);
        try {
            userInterfaceInfoMapper.insert(record);
            log.info("用户首次调用接口，自动开通并赠送 {} 次, userId: {}, interfaceInfoId: {}",
                    DEFAULT_INIT_LEFT_NUM, userId, interfaceInfoId);
        } catch (DuplicateKeyException e) {
            log.info("并发首调，调用记录已由其他请求创建, userId: {}, interfaceInfoId: {}", userId, interfaceInfoId);
        }
    }

    private UserInterfaceInfo getRecord(long userId, long interfaceInfoId) {
        LambdaQueryWrapper<UserInterfaceInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserInterfaceInfo::getUserId, userId);
        queryWrapper.eq(UserInterfaceInfo::getInterfaceInfoId, interfaceInfoId);
        List<UserInterfaceInfo> records = userInterfaceInfoMapper.selectList(queryWrapper);
        return records.isEmpty() ? null : records.get(0);
    }

    /**
     * 按请求路径 + 请求方式匹配平台登记的接口，取 interface_info.url 的路径部分与请求路径比对，
     * 匹配结果带 TTL 缓存；平台未登记的接口返回 null（不计次、不记录日志）。
     * GET/POST 可能登记同一个路径，优先按请求方式精确匹配
     *
     * @param requestUri    请求路径（含 context-path）
     * @param requestMethod 请求方式（GET/POST）
     */
    public Long resolveInterfaceInfoId(String requestUri, String requestMethod) {
        if (StrUtil.isBlank(requestUri)) {
            return null;
        }
        String cacheKey = StrUtil.blankToDefault(requestMethod, "") + " " + requestUri;
        long now = System.currentTimeMillis();
        PathCacheEntry cacheEntry = pathCache.get(cacheKey);
        if (cacheEntry != null && now - cacheEntry.getCacheTime() < PATH_CACHE_TTL_MS) {
            return cacheEntry.getInterfaceInfoId();
        }
        String requestPath = normalizePath(requestUri);
        List<InterfaceInfo> interfaceInfoList = interfaceInfoMapper.selectList(null);
        Long pathMatched = null;
        Long methodMatched = null;
        for (InterfaceInfo info : interfaceInfoList) {
            if (StrUtil.isBlank(info.getUrl())) {
                continue;
            }
            if (!requestPath.equals(normalizePath(extractPath(info.getUrl())))) {
                continue;
            }
            if (pathMatched == null) {
                pathMatched = info.getId();
            }
            if (methodMatched == null
                    && StrUtil.isNotBlank(requestMethod)
                    && requestMethod.equalsIgnoreCase(info.getMethod())) {
                methodMatched = info.getId();
            }
        }
        Long interfaceInfoId = methodMatched != null ? methodMatched : pathMatched;
        pathCache.put(cacheKey, new PathCacheEntry(interfaceInfoId, now));
        return interfaceInfoId;
    }

    /**
     * 从登记的完整接口地址中取出路径部分，如 http://localhost:8123/api/name/ -> /api/name/
     */
    private String extractPath(String url) {
        try {
            return new URI(url).getPath();
        } catch (Exception e) {
            log.warn("接口地址无法解析路径, url: {}", url);
            return null;
        }
    }

    /**
     * 统一路径尾部斜杠后比较，避免登记为 /api/name 而请求为 /api/name/ 时匹配失败
     */
    private String normalizePath(String path) {
        if (StrUtil.isBlank(path)) {
            return "";
        }
        String normalized = path;
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    /**
     * 路径匹配缓存条目（interfaceInfoId 为 null 表示该路径未登记接口）
     */
    @Data
    private static class PathCacheEntry {

        private final Long interfaceInfoId;

        private final long cacheTime;
    }
}
