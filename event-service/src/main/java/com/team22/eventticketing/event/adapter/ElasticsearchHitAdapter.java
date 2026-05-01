package com.team22.eventticketing.event.adapter;

import com.team22.eventticketing.event.dto.TopEventDTO;
import org.elasticsearch.search.SearchHit;

import java.util.Map;

public class ElasticsearchHitAdapter {

    public TopEventDTO adaptToTopEventDTO(SearchHit hit) {

        Map<String, Object> source = hit.getSourceAsMap();

        return TopEventDTO.builder()
                .eventId(((Number) source.get("eventId")).longValue())
                .name((String) source.get("name"))
                .rating(source.get("rating") != null
                        ? ((Number) source.get("rating")).doubleValue()
                        : null)
                .totalBookings(source.get("totalBookings") != null
                        ? ((Number) source.get("totalBookings")).longValue()
                        : 0L)
                .build();
    }
    public TopEventDTO adapt(SearchHit hit) {
        return adaptToTopEventDTO(hit);
    }
}