package com.team22.eventticketing.user.service;

import com.team22.eventticketing.user.entity.FavoriteVenue;
import com.team22.eventticketing.user.entity.User;
import com.team22.eventticketing.user.repository.FavoriteVenueRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FavoriteVenueService {

    private final FavoriteVenueRepository favoriteVenueRepository;
    private final UserService userService;
    private final CacheEvictionService cacheEviction;

    public FavoriteVenueService(FavoriteVenueRepository favoriteVenueRepository,
                                UserService userService,
                                CacheEvictionService cacheEviction) {
        this.favoriteVenueRepository = favoriteVenueRepository;
        this.userService = userService;
        this.cacheEviction = cacheEviction;
    }

    // ─── Create ──────────────────────────────────────────────────────────────────

    public FavoriteVenue create(Long userId, FavoriteVenue favoriteVenue) {
        User user = userService.findById(userId);
        favoriteVenue.setUser(user);
        return favoriteVenueRepository.save(favoriteVenue);
    }

    // ─── [CRUD] Read by ID ────────────────────────────────────────────────────────

    @Cacheable(cacheNames = "user-service::favorite-venue", key = "#id")
    public FavoriteVenue findById(Long id) {
        return favoriteVenueRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "FavoriteVenue not found with id: " + id));
    }

    // ─── Read all (NOT cached) ────────────────────────────────────────────────────

    public List<FavoriteVenue> findAll() {
        return favoriteVenueRepository.findAll();
    }

    public List<FavoriteVenue> findByUserId(Long userId) {
        userService.findById(userId);
        return favoriteVenueRepository.findByUserId(userId);
    }

    // ─── [CRUD] Update ────────────────────────────────────────────────────────────

    public FavoriteVenue update(Long id, FavoriteVenue patch) {
        FavoriteVenue existing = findById(id);

        if (patch.getLabel() != null)     existing.setLabel(patch.getLabel());
        if (patch.getVenueName() != null) existing.setVenueName(patch.getVenueName());
        if (patch.getLocation() != null)  existing.setLocation(patch.getLocation());
        if (patch.getCapacity() != null)  existing.setCapacity(patch.getCapacity());
        if (patch.getIsDefault() != null) existing.setIsDefault(patch.getIsDefault());
        if (patch.getMetadata() != null)  existing.setMetadata(patch.getMetadata());

        FavoriteVenue saved = favoriteVenueRepository.save(existing);
        cacheEviction.evictEntityAndFeatures("favorite-venue", id, "S1-F8", "S1-F9");
        return saved;
    }

    // ─── [CRUD] Delete ────────────────────────────────────────────────────────────

    public void delete(Long id) {
        if (!favoriteVenueRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "FavoriteVenue not found with id: " + id);
        }
        favoriteVenueRepository.deleteById(id);
        cacheEviction.evictEntityAndFeatures("favorite-venue", id, "S1-F8", "S1-F9");
    }

    // ─── [S1-F7] Set Default Venue ────────────────────────────────────────────────

    @Transactional
    public User setDefault(Long userId, Long venueId) {
        userService.findById(userId); // 404 if user does not exist

        FavoriteVenue venue = findById(venueId);

        if (!venue.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Venue " + venueId + " does not belong to user " + userId);
        }

        favoriteVenueRepository.clearDefaultsByUserId(userId);
        venue.setIsDefault(true);
        favoriteVenueRepository.save(venue);

        // Evict: venue detail, user profile (S1-F8 includes venues), category filter (S1-F9)
        cacheEviction.evictEntityAndFeatures("favorite-venue", venueId, "S1-F8", "S1-F9");
        cacheEviction.evictDetail("user", userId);

        return userService.findById(userId);
    }
}
