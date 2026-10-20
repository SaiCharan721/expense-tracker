package com.saicharan.expensetracker.common;

import org.springframework.data.domain.Page;

import java.util.List;

/** Stable JSON shape for paged results (Spring's PageImpl JSON is not guaranteed to stay the same). */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
