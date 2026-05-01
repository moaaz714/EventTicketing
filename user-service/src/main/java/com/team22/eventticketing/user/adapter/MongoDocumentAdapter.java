package com.team22.eventticketing.user.adapter;

import com.team22.eventticketing.user.dto.*;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

public class MongoDocumentAdapter {


    public TopAttendeeDTO adaptTopAttendee(Document doc) {

        return TopAttendeeDTO.builder()
                .userId(doc.getLong("userId"))
                .name(doc.getString("name"))
                .totalSpent(doc.getDouble("totalSpent"))
                .bookingCount(doc.getLong("bookingCount")) // momken error yedraqb hena bsbab el naming convention
                .build();
    }
    public TopAttendeeDTO adapt(Document doc) {
        return adaptTopAttendee(doc);
    }

    public UserBookingSummaryDTO adapt(Document doc) {
        return adaptUserBookingSummary(doc);
    }


//    public UserBookingSummaryDTO adaptUserBookingSummary(Document doc) {
//
//        return new UserBookingSummaryDTO(
//                doc.getLong("userId"),
//                doc.getString("name"),
//                doc.getLong("totalBookings"),
//                doc.getLong("completedBookings"),
//                doc.getLong("cancelledBookings"),
//                doc.getDouble("totalSpent"),
//                doc.getDouble("averageBookingAmount")
//        );
//    }
    public UserBookingSummaryDTO adaptUserBookingSummary(Document doc) {

        return UserBookingSummaryDTO.builder()
                .userId(doc.getLong("userId"))
                .name(doc.getString("name"))
                .totalBookings(doc.getLong("totalBookings"))
                .completedBookings(doc.getLong("completedBookings"))
                .cancelledBookings(doc.getLong("cancelledBookings"))
                .totalSpent(doc.getDouble("totalSpent"))
                .averageBookingAmount(doc.getDouble("averageBookingAmount"))
                .build();
    }
// dool byego mn PostgreSQl msh mn mngo w handled by el factory method mn el entity
//    public UserProfileDTO adaptUserProfile(Document doc) {
//
//        UserProfileDTO dto = new UserProfileDTO();
//
//        dto.setUserId(doc.getLong("userId"));
//        dto.setName(doc.getString("name"));
//        dto.setEmail(doc.getString("email"));
//        dto.setPhone(doc.getString("phone"));
//
//        dto.setPreferences(
//                (Map<String, Object>) doc.get("preferences")
//        );
//
//
//        List<Document> venuesDocs = (List<Document>) doc.get("favoriteVenues");
//        List<UserProfileDTO.VenueDTO> venues = new ArrayList<>();
//
//        if (venuesDocs != null) {
//            for (Document v : venuesDocs) {
//                venues.add(adaptVenue(v));
//            }
//        }
//
//        dto.setFavoriteVenues(venues);
//        dto.setTotalFavoriteVenues(venues.size());
//
//        return dto;
//    }
//
//
//    public UserProfileDTO.VenueDTO adaptVenue(Document doc) {
//
//        UserProfileDTO.VenueDTO dto = new UserProfileDTO.VenueDTO();
//
//        dto.setLabel(doc.getString("label"));
//        dto.setVenueName(doc.getString("venueName"));
//        dto.setLocation(doc.getString("location"));
//        dto.setCapacity(doc.getInteger("capacity"));
//        dto.setIsDefault(doc.getBoolean("isDefault"));
//        dto.setMetadata((Map<String, Object>) doc.get("metadata"));
//
//        return dto;
//    }

    // Optional placeholder for future AuthEvent
    public void adaptAuthEvent(Document doc) {
        // will be implemented with Observer pattern later
    }
}