package fr.kickoffmatchup.teamservice.controller;

import fr.kickoffmatchup.teamservice.model.dto.TeamCreateDto;
import fr.kickoffmatchup.teamservice.model.dto.TeamResponseDto;
import fr.kickoffmatchup.teamservice.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping()
    public ResponseEntity<List<TeamResponseDto>> getAllTeams() {
        List<TeamResponseDto> teams = teamService.getAllTeams();
        return ResponseEntity.ok(teams);
    }

    @PostMapping
    public ResponseEntity<TeamResponseDto> createTeam(@Valid @RequestBody TeamCreateDto teamDto) {
        TeamResponseDto teamResponseDto = teamService.createTeam(teamDto);
        return ResponseEntity.ok(teamResponseDto);
    }
}
