package fr.kickoffmatchup.teamservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "user_references")
public class UserReference {
    @Id
    private Long id;

    @Column(nullable = false)
    private String username;
}
