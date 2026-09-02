package org.example.pantrypilot.dto;

import org.example.pantrypilot.model.ChatActionType;

public record ConfirmActionResponse(
        ChatActionType actionType,
        Object result,
        BulkActionResult bulkResult
) {

    public static ConfirmActionResponse single(ChatActionType type, Object result) {
        return new ConfirmActionResponse(type, result, null);
    }

    public static ConfirmActionResponse bulk(BulkActionResult bulkResult) {
        return new ConfirmActionResponse(ChatActionType.BULK_ACTION, null, bulkResult);
    }
}
