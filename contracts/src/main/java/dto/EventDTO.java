package dto;
import java.time.LocalDateTime;

public record EventDTO(Long id, String name, String status, LocalDateTime eventDate) {}