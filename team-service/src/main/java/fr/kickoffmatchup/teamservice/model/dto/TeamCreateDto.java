package fr.kickoffmatchup.teamservice.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import jakarta.validation.constraints.Size;

@Data
public class TeamCreateDto {

    @NotBlank(message = "Team name cannot be blank")
    @Size(max = 100, message = "Team name cannot exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Team description cannot exceed 500 characters")
    private String description;

    @NotBlank(message = "City cannot be blank")
    @Size(max = 100, message = "City name cannot exceed 100 characters")
    private String city;

    @NotBlank(message = "Zip code cannot be blank")
    @Size(max = 20, message = "Zip code cannot exceed 20 characters")
    private String zipCode;

    @NotBlank(message = "Owner cannot be blank")
    private Long ownerId;
}
