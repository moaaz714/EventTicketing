package com.team22.eventticketing.user.repository;

import com.team22.eventticketing.user.entity.FavoriteVenue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavoriteVenueRepository extends JpaRepository<FavoriteVenue, Long> {

    List<FavoriteVenue> findByUserId(Long userId);
}
