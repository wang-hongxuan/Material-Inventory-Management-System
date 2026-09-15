package com.inventory.common;

import java.util.List;

/**
 * 统一分页返回结构。
 *
 * @param items 当前页数据
 * @param total 满足条件的总记录数
 * @param page 当前页码，从 1 开始
 * @param pageSize 每页大小
 */
public record PageResult<T>(List<T> items, long total, int page, int pageSize) {
}
