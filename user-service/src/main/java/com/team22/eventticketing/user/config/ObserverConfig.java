package com.team22.eventticketing.user.config;

import com.team22.eventticketing.user.observer.MongoEventLogger;
import com.team22.eventticketing.user.service.FavoriteVenueService;
import com.team22.eventticketing.user.service.UserService;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObserverConfig {

    private final UserService userService;
    private final FavoriteVenueService favoriteVenueService;
    private final MongoEventLogger mongoEventLogger;

    public ObserverConfig(UserService userService,
                          FavoriteVenueService favoriteVenueService,
                          MongoEventLogger mongoEventLogger) {
        this.userService = userService;
        this.favoriteVenueService = favoriteVenueService;
        this.mongoEventLogger = mongoEventLogger;
    }

    @PostConstruct
    public void registerObservers() {
        userService.register(mongoEventLogger);
        favoriteVenueService.register(mongoEventLogger);
    }
}
