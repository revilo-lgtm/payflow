package com.payflow.support;

import org.slf4j.MDC;

public final class CorrelationIdHolder {

	public static final String HEADER_NAME = "X-Correlation-Id";
	public static final String MDC_KEY = "correlationId";

	private CorrelationIdHolder() {
	}

	public static String get() {
		return MDC.get(MDC_KEY);
	}

}
