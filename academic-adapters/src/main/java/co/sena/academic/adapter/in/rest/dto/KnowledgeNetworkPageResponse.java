package co.sena.academic.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Matches the { data, pagination } envelope defined in
 * academic-management.yaml (GET /knowledge-networks response),
 * reusing _shared.yaml#/components/schemas/Pagination.
 * "page" here is 1-based per the contract; Spring's internal paging
 * is 0-based, translated at the mapper boundary.
 */
public record KnowledgeNetworkPageResponse(
        List<KnowledgeNetworkResponse> data,
        Pagination pagination
) {
    public record Pagination(
            int page,
            @JsonProperty("page_size") int pageSize,
            @JsonProperty("total_items") long totalItems,
            @JsonProperty("total_pages") int totalPages
    ) {
    }
}