package fr.kickoffmatchup.teamservice.builder;

import fr.kickoffmatchup.teamservice.model.Team;
import fr.kickoffmatchup.teamservice.model.dto.TeamCreateDto;
import fr.kickoffmatchup.teamservice.model.dto.TeamResponseDto;
import org.springframework.stereotype.Component;

@Component
public class TeamBuilder {

    public Team build(TeamCreateDto teamCreateDto) {
        return Team.builder()
                .name(teamCreateDto.getName())
                .description(teamCreateDto.getDescription())
                .city(teamCreateDto.getCity())
                .zipCode(teamCreateDto.getZipCode())
                .build();

    }

    public TeamResponseDto buildResponse(Team savedTeam) {
        return TeamResponseDto.builder()
                .id(savedTeam.getId())
                .name(savedTeam.getName())
                .description(savedTeam.getDescription())
                .city(savedTeam.getCity())
                .zipCode(savedTeam.getZipCode())
                .owner(savedTeam.getOwner())
                .createdAt(savedTeam.getCreatedAt())
                .build();
    }
}