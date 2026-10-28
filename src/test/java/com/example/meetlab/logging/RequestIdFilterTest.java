package com.example.meetlab.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    /** 필터 체인 안에서 보이는 MDC 값을 잡아 돌려준다. */
    private String run(MockHttpServletRequest request, MockHttpServletResponse response) throws Exception {
        AtomicReference<String> seenInChain = new AtomicReference<>();
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res)
                    throws java.io.IOException, jakarta.servlet.ServletException {
                seenInChain.set(MDC.get(RequestIdFilter.MDC_KEY));
                super.doFilter(req, res);
            }
        };
        filter.doFilter(request, response, chain);
        return seenInChain.get();
    }

    @Test
    void generatesIdWhenHeaderMissingAndExposesItInMdcAndResponse() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        String seen = run(new MockHttpServletRequest(), response);

        assertThat(seen).isNotBlank();
        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo(seen);
    }

    @Test
    void reusesSafeIncomingId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER, "client-req-12345");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String seen = run(request, response);

        assertThat(seen).isEqualTo("client-req-12345");
        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo("client-req-12345");
    }

    @Test
    void replacesUnsafeIncomingIdToPreventLogInjection() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER, "abc12345\n{\"log.level\":\"ERROR\"}");

        String seen = run(request, new MockHttpServletResponse());

        assertThat(seen).doesNotContain("\n").doesNotContain("{");
    }

    @Test
    void clearsMdcAfterRequestSoNextRequestDoesNotInheritIt() throws Exception {
        run(new MockHttpServletRequest(), new MockHttpServletResponse());

        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void resolveRejectsTooShortOrTooLongIds() {
        assertThat(RequestIdFilter.resolve("short")).isNotEqualTo("short");
        String tooLong = "a".repeat(65);
        assertThat(RequestIdFilter.resolve(tooLong)).isNotEqualTo(tooLong);
        assertThat(RequestIdFilter.resolve(null)).isNotBlank();
    }
}
