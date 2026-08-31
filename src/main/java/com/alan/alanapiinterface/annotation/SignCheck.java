package com.alan.alanapiinterface.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要进行签名校验的接口，由 SignInterceptor 统一验签
 * <p>
 * 参与验签的 body 取值规则：
 * 1. bodyParam 为空（JSON 接口）：取原始请求体
 * 2. bodyParam 不为空（表单接口）：取该请求参数的值
 *
 * @author alan
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SignCheck {

    /**
     * 参与验签的请求参数名（表单接口用），默认为空表示校验原始请求体
     */
    String bodyParam() default "";
}
