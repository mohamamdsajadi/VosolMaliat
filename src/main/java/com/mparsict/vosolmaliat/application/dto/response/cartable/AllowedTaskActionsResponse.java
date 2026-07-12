package com.mparsict.vosolmaliat.application.dto.response.cartable;

public record AllowedTaskActionsResponse(
        boolean claim,
        boolean unclaim,
        boolean complete,
        boolean assign,
        boolean delegate,
        boolean view
) {}
