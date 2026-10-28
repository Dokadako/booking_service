package com.dokadako.booking.common;

import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageRequests {

    private static final String ID = "id";

    private PageRequests() {
    }

    /**
     * Translates the client's sort fields (API names) into entity properties and rejects anything else,
     * so clients cannot sort by internal fields such as {@code user.passwordHash}.
     * Adds the id as a final sort key: without a unique key, rows with equal values may move
     * between pages and be returned twice or skipped.
     *
     * @param allowed API field name to entity property path
     */
    public static Pageable withAllowedSort(Pageable pageable, Map<String, String> allowed) {
        Sort sort = Sort.unsorted();
        for (Sort.Order order : pageable.getSort()) {
            String property = allowed.get(order.getProperty());
            if (property == null) {
                throw new BadRequestException("Unknown sort property '%s'".formatted(order.getProperty()));
            }
            sort = sort.and(Sort.by(order.withProperty(property)));
        }
        if (sort.getOrderFor(ID) == null) {
            sort = sort.and(Sort.by(ID));
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }
}
