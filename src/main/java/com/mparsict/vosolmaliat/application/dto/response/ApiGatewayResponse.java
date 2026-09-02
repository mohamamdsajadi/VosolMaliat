package com.mparsict.vosolmaliat.application.dto.response;

import java.util.List;

public record ApiGatewayResponse<T>(
        T apiGatewayData,
        String apiGatewayTraceID,
        boolean apiGatewayIsSuccess,
        int apiGatewayStatusCode,
        List<String> apiGatewayMessages
) {
    public static <T> ApiGatewayResponse<T> success(T data, String traceId, String message) {
        return new ApiGatewayResponse<>(data, traceId, true, 0, List.of(message));
    }

    public static <T> ApiGatewayResponse<T> failure(
            T data,
            String traceId,
            boolean apiLayerSuccess,
            int statusCode,
            String message) {
        return new ApiGatewayResponse<>(data, traceId, apiLayerSuccess, statusCode, List.of(message));
    }
}
