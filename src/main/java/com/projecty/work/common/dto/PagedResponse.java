package com.projecty.work.common.dto;

import java.util.List;

/**
 * One page of a list. {@code page} is zero-based.
 */
public record PagedResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {
}
