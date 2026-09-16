package fr.kickoffmatchup.teamservice.model.dto;

import fr.kickoffmatchup.teamservice.model.UserReference;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
public class TeamResponseDto {
    @NonNull
    private Long id;
    private String name;
    private String description;
    private String city;
    private String zipCode;
    @NonNull
    private UserReference owner;
    private LocalDateTime createdAt;
}
