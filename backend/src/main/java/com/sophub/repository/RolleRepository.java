package com.sophub.repository;

import com.sophub.model.Rolle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RolleRepository extends JpaRepository<Rolle, Long> {
    // Suchfunktion, um eine Rolle anhand ihres Namens (z.B. "STUDENT") zu finden
    Optional<Rolle> findByName(String name);
}