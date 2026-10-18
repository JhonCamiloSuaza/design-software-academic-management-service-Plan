package co.sena.academic.adapter.in.rest.dto;

import java.util.List;

/** Pagination envelope for GET /tech-lines. */
public record TechLinePageResponse(
        List<TechLineResponse> content,
        long totalElements,
        int page,
        int size
) {
}