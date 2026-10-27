package com.eventpass.logging;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void doFilter_whenNoHeaderProvided_generates8CharIdAndClearsMdcAfterward() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Capture what MDC.get("requestId") looks like WHILE the request is inside the controller/service
        AtomicReference<String> mdcInsideRequest = new AtomicReference<>();
        FilterChain filterChain = (req, res) -> mdcInsideRequest.set(MDC.get(RequestIdFilter.MDC_KEY));

        filter.doFilter(request, response, filterChain);

        // 1. Inside the request, MDC had an 8-character generated requestId
        String generatedId = mdcInsideRequest.get();
        assertThat(generatedId).isNotBlank().hasSize(8);

        // 2. Response header X-Request-Id matches the generated requestId
        assertThat(response.getHeader(RequestIdFilter.REQUEST_ID_HEADER)).isEqualTo(generatedId);

        // 3. After the request completes, MDC is cleaned up (no thread-pool leak!)
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void doFilter_whenClientProvidesHeader_reusesClientIdAndClearsMdcAfterward() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, "client-req-999");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<String> mdcInsideRequest = new AtomicReference<>();
        FilterChain filterChain = (req, res) -> mdcInsideRequest.set(MDC.get(RequestIdFilter.MDC_KEY));

        filter.doFilter(request, response, filterChain);

        assertThat(mdcInsideRequest.get()).isEqualTo("client-req-999");
        assertThat(response.getHeader(RequestIdFilter.REQUEST_ID_HEADER)).isEqualTo("client-req-999");
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }
}