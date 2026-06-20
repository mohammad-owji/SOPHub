package com.sophub.repository;

import com.sophub.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Suchfunktion, um zu prüfen, ob die Hochschul-Kennung schon existiert
    Optional<User> findByKennung(String kennung);
}