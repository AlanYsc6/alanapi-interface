package com.alan.alanapiinterface.filter;

import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 请求体缓存过滤器：提前读取请求体并缓存，保证请求体可重复读取
 * （签名校验切面和 Controller 各读一次）
 *
 * @author alan
 */
@Component
public class RequestBodyCachingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            chain.doFilter(new CacheRequestBodyWrapper((HttpServletRequest) request), response);
        } else {
            chain.doFilter(request, response);
        }
    }

    /**
     * 请求体缓存包装器：构造时一次性读入请求体，之后可任意次读取。
     * 表单请求不缓存：参数由 Servlet 容器原生解析，预读请求体会导致 getParameter 拿不到参数
     */
    public static class CacheRequestBodyWrapper extends HttpServletRequestWrapper {

        /**
         * 表单内容类型
         */
        private static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";

        private final byte[] body;

        public CacheRequestBodyWrapper(HttpServletRequest request) throws IOException {
            super(request);
            String contentType = request.getContentType();
            boolean isForm = contentType != null && contentType.contains(FORM_CONTENT_TYPE);
            this.body = isForm ? null : StreamUtils.copyToByteArray(request.getInputStream());
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (body == null) {
                return super.getInputStream();
            }
            ByteArrayInputStream buffer = new ByteArrayInputStream(body);
            return new ServletInputStream() {

                @Override
                public boolean isFinished() {
                    return buffer.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public int read() {
                    return buffer.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() throws IOException {
            if (body == null) {
                return super.getReader();
            }
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
