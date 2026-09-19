package com.hourlystore.dto;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private List<T> records;
    private long total;
    private long page;
    private long pageSize;
    private long totalPages;

    public static <T> PageResult<T> of(List<T> records, long total, long page, long pageSize) {
        PageResult<T> r = new PageResult<>();
        r.setRecords(records);
        r.setTotal(total);
        r.setPage(page);
        r.setPageSize(pageSize);
        r.setTotalPages(pageSize <= 0 ? 0 : (total + pageSize - 1) / pageSize);
        return r;
    }
}
