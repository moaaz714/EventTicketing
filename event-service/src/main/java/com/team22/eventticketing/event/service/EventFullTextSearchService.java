package com.team22.eventticketing.event.service;

import com.team22.eventticketing.event.model.EventSearchDocument;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventFullTextSearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    public EventFullTextSearchService(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @Cacheable(
            cacheNames = "event-service::S2-F10",
            key = "T(String).valueOf(#query) + ':' + " +
                    "T(String).valueOf(#category) + ':' + " +
                    "T(String).valueOf(#venue) + ':' + " +
                    "T(String).valueOf(#status) + ':' + " +
                    "T(String).valueOf(#startDate) + ':' + " +
                    "T(String).valueOf(#endDate) + ':' + " +
                    "T(String).valueOf(#minRating) + ':' + " +
                    "T(String).valueOf(#maxRating)"
    )
    public List<EventSearchDocument> fullTextSearch(
            String query,
            String category,
            String venue,
            String status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Double minRating,
            Double maxRating
    ) {
        if (query == null || query.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Query is required");
        }

        Criteria criteria = new Criteria("name").matches(query)
                .or(new Criteria("description").matches(query))
                .or(new Criteria("venue").matches(query));

        if (category != null && !category.isBlank()) {
            criteria = criteria.and(new Criteria("category").is(category));
        }

        if (venue != null && !venue.isBlank()) {
            criteria = criteria.and(new Criteria("venue").matches(venue));
        }

        if (status != null && !status.isBlank()) {
            criteria = criteria.and(new Criteria("status").is(status));
        }

        if (startDate != null) {
            criteria = criteria.and(new Criteria("eventDate").greaterThanEqual(startDate));
        }

        if (endDate != null) {
            criteria = criteria.and(new Criteria("eventDate").lessThanEqual(endDate));
        }

        if (minRating != null) {
            criteria = criteria.and(new Criteria("rating").greaterThanEqual(minRating));
        }

        if (maxRating != null) {
            criteria = criteria.and(new Criteria("rating").lessThanEqual(maxRating));
        }

        CriteriaQuery searchQuery = new CriteriaQuery(criteria, PageRequest.of(0, 100));

        SearchHits<EventSearchDocument> hits =
                elasticsearchOperations.search(searchQuery, EventSearchDocument.class);

        return hits.stream()
                .map(SearchHit::getContent)
                .toList();
    }
}