package com.greenink.api.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    @GetMapping("/review")
    public Map<String, Object> reviewGuard() {
        return Map.of(
                "message", "ROLE_ADMIN guard is active.",
                "note", "Content-management endpoints should be added only after the content schema/workflow is finalized."
        );
    }
}
