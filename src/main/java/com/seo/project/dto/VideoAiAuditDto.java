package com.seo.project.dto;

import java.util.List;

public record VideoAiAuditDto(
    int seoScore,
    List<AuditItem> audits
) {
    public record AuditItem(boolean passed, String message) {}
}
