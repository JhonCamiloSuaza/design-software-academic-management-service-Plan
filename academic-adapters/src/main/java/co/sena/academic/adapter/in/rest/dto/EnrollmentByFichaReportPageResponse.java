package co.sena.academic.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record EnrollmentByFichaReportPageResponse(
        List<EnrollmentByFichaReportItemResponse> data,
        Pagination pagination) {

    public record Pagination(
            int page,
            @JsonProperty("page_size") int pageSize,
            @JsonProperty("total_items") long totalItems,
            @JsonProperty("total_pages") int totalPages) {
    }
}
