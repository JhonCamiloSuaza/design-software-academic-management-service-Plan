package co.sena.academic.adapter.in.rest.dto;

import java.util.List;

public record TechNetworkPageResponse(
        List<TechNetworkResponse> content,
        long totalElements,
        int page,
        int size
) {
}