package com.goodprod.project.repos;

import com.goodprod.project.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<Long, User> {
    Optional<User> findByLogin(String login);
}
