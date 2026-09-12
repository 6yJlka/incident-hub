package ru.donskikh.incidenthub.identity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("""
            select count(user) > 0
            from User user
            where lower(user.email) = lower(trim(:email))
            """)
    boolean existsByNormalizedEmail(@Param("email") String email);

    Page<User> findAllByActive(boolean active, Pageable pageable);
}
