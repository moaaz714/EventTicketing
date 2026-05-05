package com.team22.eventticketing.booking.repository;

import com.team22.eventticketing.booking.model.UserNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserNodeRepository extends Neo4jRepository<UserNode, Long> {
    @Query("""
    MATCH (u:UserNode {userId: $userId})-[r:ATTENDED]->(e:EventNode {eventId: $eventId})
    WHERE $bookingId IN r.recorded_booking_ids
    RETURN COUNT(r)
    """)
    Long countRecorded(Long userId, Long eventId, Long bookingId);
}