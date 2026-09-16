package fr.kickoffmatchup.teamservice.repository;

import fr.kickoffmatchup.teamservice.model.UserReference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserReferenceRepository extends JpaRepository<UserReference, Long> {
}
