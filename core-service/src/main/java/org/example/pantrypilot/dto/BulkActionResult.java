package org.example.pantrypilot.dto;

import java.util.List;

public record BulkActionResult(
        int succeeded,
        int failed,
        List<BulkActionFailure> failures
) {

    public record BulkActionFailure(Object target, String reason) {
    }
}
