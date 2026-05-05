package com.team22.eventticketing.booking.repository;

import com.team22.eventticketing.booking.model.UserNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserNodeRepository extends Neo4jRepository<UserNode, Long>, UserNodeRecommendationRepository {
    Optional<UserNode> findByUserId(Long userId);
// try to find the user using the id if found if not dont crash
}