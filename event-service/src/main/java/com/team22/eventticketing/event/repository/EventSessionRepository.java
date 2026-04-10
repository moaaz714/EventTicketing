package com.team22.eventticketing.event.repository;

import com.team22.eventticketing.event.model.EventSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventSessionRepository extends JpaRepository<EventSession, Long> {
}