package dev.languagelearning.core.repository;

import dev.languagelearning.core.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by display name.
     *
     * @param displayName the display name to search for
     * @return the user if found
     */
    Optional<User> findByDisplayName(String displayName);

    /**
     * Checks if a user exists with the given display name.
     *
     * @param displayName the display name to check
     * @return true if a user exists with this name
     */
    boolean existsByDisplayName(String displayName);
}