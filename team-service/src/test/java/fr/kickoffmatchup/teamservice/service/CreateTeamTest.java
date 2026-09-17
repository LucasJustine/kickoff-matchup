package fr.kickoffmatchup.teamservice.service;

import fr.kickoffmatchup.teamservice.builder.TeamBuilder;
import fr.kickoffmatchup.teamservice.model.Team;
import fr.kickoffmatchup.teamservice.model.UserReference;
import fr.kickoffmatchup.teamservice.model.dto.TeamCreateDto;
import fr.kickoffmatchup.teamservice.model.dto.TeamResponseDto;
import fr.kickoffmatchup.teamservice.repository.TeamRepository;
import fr.kickoffmatchup.teamservice.repository.UserReferenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CreateTeamTest {

    @Spy
    private TeamBuilder teamBuilder;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private UserReferenceRepository userRepository;

    @InjectMocks
    private TeamService teamService;


    @Test
    public void testCreateTeam() {
        TeamCreateDto teamCreateDto = new TeamCreateDto();
        teamCreateDto.setName("Team A");
        teamCreateDto.setCity("City A");
        teamCreateDto.setZipCode("12345");
        teamCreateDto.setOwnerId(1L);

        UserReference mockOwner = new UserReference();
        mockOwner.setId(1L);
        mockOwner.setUsername("Zidane98");

        Team team = new Team();
        team.setName("Team A");
        team.setCity("City A");
        team.setZipCode("12345");
        team.setOwner(mockOwner);
        team.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockOwner));
        when(teamRepository.save(any(Team.class))).thenReturn(team);

        TeamResponseDto response = teamService.createTeam(teamCreateDto);

        assertThat(response).isNotNull();

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Team A");
        assertThat(response.getCity()).isEqualTo("City A");
        assertThat(response.getZipCode()).isEqualTo("12345");

        assertThat(response.getOwner()).isNotNull();
        assertThat(response.getOwner().getId()).isEqualTo(1L);
        assertThat(response.getOwner().getUsername()).isEqualTo("Zidane98");
    }

    @Test
    public void testCreateTeamWithExisingName() {
        TeamCreateDto teamCreateDto = new TeamCreateDto();
        teamCreateDto.setName("Team A");
        teamCreateDto.setCity("City A");
        teamCreateDto.setZipCode("12345");
        teamCreateDto.setOwnerId(1L);

        UserReference mockOwner = new UserReference();
        mockOwner.setId(1L);
        mockOwner.setUsername("Zidane98");

        Team existingTeam = new Team();
        existingTeam.setName("Team A");
        existingTeam.setCity("City B");
        existingTeam.setZipCode("54321");
        existingTeam.setOwner(mockOwner);
        existingTeam.setId(2L);

        when(teamRepository.findByName("Team A")).thenReturn(Optional.of(existingTeam));

        assertThatThrownBy(() -> teamService.createTeam(teamCreateDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Team name already exists");

        verify(teamRepository, never()).save(any(Team.class));
    }

    @Test
    public void testCreateTeamWithoutExistingOwner() {
        TeamCreateDto teamCreateDto = new TeamCreateDto();
        teamCreateDto.setName("Team A");
        teamCreateDto.setCity("City A");
        teamCreateDto.setZipCode("12345");
        teamCreateDto.setOwnerId(1L);

        when(teamRepository.findByName("Team A")).thenReturn(Optional.empty());

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.createTeam(teamCreateDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Owner not found");

        verify(teamRepository, never()).save(any(Team.class));
    }
}
