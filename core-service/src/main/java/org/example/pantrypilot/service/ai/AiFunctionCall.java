package org.example.pantrypilot.service.ai;

import java.util.Map;

public record AiFunctionCall(
        String name,
        Map<String, Object> args
) {

    public AiFunctionCall {
        args = args == null ? Map.of() : Map.copyOf(args);
    }
}
