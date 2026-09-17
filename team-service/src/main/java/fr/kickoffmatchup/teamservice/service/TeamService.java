package fr.kickoffmatchup.teamservice.service;

import fr.kickoffmatchup.teamservice.builder.TeamBuilder;
import fr.kickoffmatchup.teamservice.exception.ResourceAlreadyExistsException;
import fr.kickoffmatchup.teamservice.exception.ResourceNotFoundException;
import fr.kickoffmatchup.teamservice.model.Team;
import fr.kickoffmatchup.teamservice.model.UserReference;
import fr.kickoffmatchup.teamservice.model.dto.TeamCreateDto;
import fr.kickoffmatchup.teamservice.model.dto.TeamResponseDto;
import fr.kickoffmatchup.teamservice.repository.TeamRepository;
import fr.kickoffmatchup.teamservice.repository.UserReferenceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TeamService {

    private final TeamRepository teamRepository;

    private final UserReferenceRepository userReferenceRepository;

    private final TeamBuilder teamBuilder;

    public TeamService(TeamRepository teamRepository, TeamBuilder teamBuilder, UserReferenceRepository userReferenceRepository) {
        this.teamRepository = teamRepository;
        this.teamBuilder = teamBuilder;
        this.userReferenceRepository = userReferenceRepository;
    }

    public TeamResponseDto createTeam(TeamCreateDto teamDto) {
        Optional<Team> existingTeam = teamRepository.findByName(teamDto.getName());
        if (existingTeam.isPresent()) {
            throw new ResourceAlreadyExistsException("Team name already exists");
        }


       UserReference owner = userReferenceRepository.findById(teamDto.getOwnerId())
               .orElseThrow(() -> new ResourceNotFoundException("Owner not found"));

       Team team = teamBuilder.build(teamDto);
       team.setOwner(owner);

       Team savedTeam = teamRepository.save(team);

       return teamBuilder.buildResponse(savedTeam);
    }

    public List<TeamResponseDto> getAllTeams() {
        return teamRepository.findAll()
                .stream()
                .map(teamBuilder::buildResponse)
                .collect(Collectors.toList());
    }
}
