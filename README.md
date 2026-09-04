# alanapi-interface — 接口服务（API 提供方）

API 开放平台的接口执行方：对外提供真实的 API（如名称服务），并统一处理**签名校验、调用计数、调用日志**三件横切事情。调用方（alanapi-client-sdk 或平台在线调用）不直接接触业务逻辑，全部经过 `@SignCheck` 切面。

## 技术栈

- Spring Boot 2.7 + MyBatis-Plus + Redis（nonce 防重放）
- MySQL（`alan` 库，与 alanapi-backend 共用：读 user / interface_info，写 user_interface_info / invoke_log）

## 请求处理链路

所有标注 `@SignCheck` 的接口统一经过 `SignInterceptor` 切面：

```
请求 → 签名校验 → 调用计数 → 业务执行 → 记录调用日志
```

各环节语义（失败调用同样留痕）：

| 调用情形 | 总次数 | 剩余次数 | 调用日志 |
|---|---|---|---|
| 业务调用成功 | +1 | -1 | 记成功 |
| 业务调用失败（异常 / 返回非 200） | +1 | -1 | 记失败，含错误信息 |
| 验签失败 | 不计 | 不扣 | 记失败（"签名校验未通过"，用户记"未认证"） |
| 次数不足 / 已禁用被拒 | 不计 | 不扣 | 记失败，含具体原因 |
| 平台未登记的接口 | 不计 | 不扣 | 记日志（接口 id 记 0） |

### 签名校验（UserValidateService）
1. 请求头取 `accessKey / nonce / timestamp / sign`
2. 按 accessKey 查 user 表验证用户存在，**冻结用户（userStatus=1）直接拒绝**
3. 时间戳误差超过 5 分钟拒绝（防过期重放）
4. nonce 写入 Redis（SETNX + 过期），重复请求拒绝（防重放）
5. 用用户 secretKey 重新计算 HMAC-SHA256 签名，常量时间比较（防时序攻击）

签名生成规则见 `SignUtils`：对 `accessKey、body、nonce、timestamp` 按 key 排序后拼接，用 secretKey 做 HMAC-SHA256。secretKey 只参与本地计算，绝不随请求发送。

### 调用计数（InterfaceCountService）
- 按「请求路径 + 请求方式」匹配 `interface_info` 表登记的接口（GET/POST 可同路径，优先按 method 精确匹配），匹配结果带 60 秒缓存
- 用户首次调用某接口时**自动开通**并赠送初始次数（`DEFAULT_INIT_LEFT_NUM = 50`，唯一索引兜底并发首调）
- 扣次为条件更新（`leftNum > 0`），并发不会扣成负数

### 调用日志（InvokeLogService）
写 `invoke_log` 表：用户、接口、请求方式 / 路径 / 参数（截断至 1024）、响应（截断至 512）、成败、耗时。日志写入失败只打错误日志，不影响调用本身。

## 快速启动

```bash
# 前置：与 backend 共用的 MySQL / Redis 可用，user 表中已有带 accessKey 的用户
# 默认端口 8123，context-path /api
mvn spring-boot:run
```

## 如何新增一个 API

```java
@RestController
@RequestMapping("/hello")
public class HelloController {

    @SignCheck(bodyParam = "name")   // 表单接口：参与签名的是指定参数的值
    @PostMapping("/")
    public BaseResponse<String> sayHello(@RequestParam String name) {
        return ResultUtils.success("hello " + name);
    }

    @SignCheck                        // JSON 接口：参与签名的是原始请求体
    @PostMapping("/json")
    public BaseResponse<String> json(@RequestBody String body) {
        return ResultUtils.success("received: " + body);
    }
}
```

写完即自动纳入验签、计数、日志，无需额外代码。要在平台上线，还需在管理后台「接口管理」登记该接口（URL 与 method 需与这里一致），发布后用户即可申请调用次数并调用。

## 相关项目

- [alanapi-backend](../alanapi-backend)：平台主后端
- [alanapi-client-sdk](../alanapi-client-sdk)：客户端 SDK
- [alanapi-frontend](../alanapi-frontend)：Web 前端
