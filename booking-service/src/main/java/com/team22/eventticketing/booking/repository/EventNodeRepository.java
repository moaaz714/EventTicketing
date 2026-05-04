package com.team22.eventticketing.booking.repository;

import com.team22.eventticketing.booking.model.EventNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventNodeRepository extends Neo4jRepository<EventNode, Long> {
}