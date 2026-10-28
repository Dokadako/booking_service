package com.dokadako.booking.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class PageRequestsTest {

    private static final Map<String, String> ALLOWED = Map.of("startsAt", "startsAt", "roomName", "room.name");

    @Test
    void shouldMapApiFieldToEntityPropertyAndAddIdTiebreaker() {
        Pageable pageable = PageRequest.of(2, 10, Sort.by(Sort.Direction.DESC, "roomName"));

        Pageable result = PageRequests.withAllowedSort(pageable, ALLOWED);

        assertThat(result.getPageNumber()).isEqualTo(2);
        assertThat(result.getPageSize()).isEqualTo(10);
        assertThat(result.getSort()).containsExactly(Sort.Order.desc("room.name"), Sort.Order.asc("id"));
    }

    @Test
    void shouldSortByIdWhenClientGaveNoSort() {
        Pageable result = PageRequests.withAllowedSort(PageRequest.of(0, 20), ALLOWED);

        assertThat(result.getSort()).containsExactly(Sort.Order.asc("id"));
    }

    @Test
    void shouldNotAddSecondIdOrderWhenClientAlreadySortsById() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "id"));

        Pageable result = PageRequests.withAllowedSort(pageable, Map.of("id", "id"));

        assertThat(result.getSort()).containsExactly(Sort.Order.desc("id"));
    }

    @Test
    void shouldRejectFieldThatIsNotAllowed() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("user.passwordHash"));

        assertThatThrownBy(() -> PageRequests.withAllowedSort(pageable, ALLOWED))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Unknown sort property 'user.passwordHash'");
    }
}
