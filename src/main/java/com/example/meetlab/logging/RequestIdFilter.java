package com.example.meetlab.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청마다 request_id 를 정하고 MDC 에 넣는다. 이 요청을 처리하는 동안 찍히는 모든 로그에 request_id 가 붙으므로
 * Kibana 에서 request_id 하나로 "한 요청의 로그 전체"를 모아 볼 수 있다. 응답 헤더로도 돌려줘서 사용자가 문의할 때 쓸 수 있다.
 *
 * <p>클라이언트가 보낸 X-Request-Id 는 형식이 안전할 때만 쓴다. 검증 없이 쓰면 줄바꿈 등을 넣어
 * 로그를 위조(로그 인젝션)할 수 있다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "request_id";
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._-]{8,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = resolve(request.getHeader(HEADER));
        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            // 스레드는 재사용되므로 반드시 지운다. 안 지우면 다음 요청 로그에 이전 요청의 ID 가 붙는다.
            MDC.remove(MDC_KEY);
        }
    }

    static String resolve(String incoming) {
        if (incoming != null && SAFE_ID.matcher(incoming).matches()) {
            return incoming;
        }
        return UUID.randomUUID().toString();
    }
}
