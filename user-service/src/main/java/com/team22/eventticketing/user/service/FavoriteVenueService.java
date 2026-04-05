package com.team22.eventticketing.user.service;

import com.team22.eventticketing.user.entity.FavoriteVenue;
import com.team22.eventticketing.user.entity.User;
import com.team22.eventticketing.user.repository.FavoriteVenueRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class FavoriteVenueService {

    private final FavoriteVenueRepository favoriteVenueRepository;
    private final UserService userService;

    public FavoriteVenueService(FavoriteVenueRepository favoriteVenueRepository,
                                UserService userService) {
        this.favoriteVenueRepository = favoriteVenueRepository;
        this.userService = userService;
    }

    // ─── Create ──────────────────────────────────────────────────────────────────

    /**
     * Creates a FavoriteVenue linked to the given userId.
     * Validates the user exists (throws 404 via UserService) before saving.
     */
    public FavoriteVenue create(Long userId, FavoriteVenue favoriteVenue) {
        User user = userService.findById(userId); // throws 404 if user missing
        favoriteVenue.setUser(user);
        return favoriteVenueRepository.save(favoriteVenue);
    }

    // ─── Read by ID ───────────────────────────────────────────────────────────────

    public FavoriteVenue findById(Long id) {
        return favoriteVenueRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "FavoriteVenue not found with id: " + id));
    }

    // ─── Read all ─────────────────────────────────────────────────────────────────

    public List<FavoriteVenue> findAll() {
        return favoriteVenueRepository.findAll();
    }

    // ─── Read all by User ─────────────────────────────────────────────────────────

    public List<FavoriteVenue> findByUserId(Long userId) {
        // Ensure user exists first (throws 404 if not)
        userService.findById(userId);
        return favoriteVenueRepository.findByUserId(userId);
    }

    // ─── Update ───────────────────────────────────────────────────────────────────

    public FavoriteVenue update(Long id, FavoriteVenue patch) {
        FavoriteVenue existing = findById(id);

        if (patch.getLabel() != null)     existing.setLabel(patch.getLabel());
        if (patch.getVenueName() != null) existing.setVenueName(patch.getVenueName());
        if (patch.getLocation() != null)  existing.setLocation(patch.getLocation());
        if (patch.getCapacity() != null)  existing.setCapacity(patch.getCapacity());
        if (patch.getIsDefault() != null) existing.setIsDefault(patch.getIsDefault());
        if (patch.getMetadata() != null)  existing.setMetadata(patch.getMetadata());

        return favoriteVenueRepository.save(existing);
    }

    // ─── Delete ───────────────────────────────────────────────────────────────────

    public void delete(Long id) {
        if (!favoriteVenueRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "FavoriteVenue not found with id: " + id);
        }
        favoriteVenueRepository.deleteById(id);
    }
}
