package com.payflow.support;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String correlationId = resolveCorrelationId(request);
		MDC.put(CorrelationIdHolder.MDC_KEY, correlationId);
		response.setHeader(CorrelationIdHolder.HEADER_NAME, correlationId);
		try {
			filterChain.doFilter(request, response);
		} finally {
			MDC.remove(CorrelationIdHolder.MDC_KEY);
		}
	}

	private String resolveCorrelationId(HttpServletRequest request) {
		String headerValue = request.getHeader(CorrelationIdHolder.HEADER_NAME);
		return StringUtils.hasText(headerValue) ? headerValue.trim() : UUID.randomUUID().toString();
	}

}
