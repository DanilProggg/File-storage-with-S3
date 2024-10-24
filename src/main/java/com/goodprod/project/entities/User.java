package com.goodprod.project.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "usr")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Column(nullable = false)
    private String name;
    private String surname;
    private String lastname;
    @Column(nullable = false)
    private String login;
    @Column(nullable = false)
    private String password;
    private String group_name;

    @ElementCollection(targetClass = Role.class,fetch = FetchType.EAGER)
    @CollectionTable(name = "usr_roles", joinColumns = @JoinColumn(name = "usr_id"))
    @Enumerated(EnumType.STRING)
    private Set<Role> roles;
}
