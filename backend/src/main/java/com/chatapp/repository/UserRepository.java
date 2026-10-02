package com.chatapp.repository;

import com.chatapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByEmailIgnoreCase(String email);
    long countByActiveTrue();

    @Query("""
            select u from User u
            where u.active = true
              and (lower(u.username) like lower(concat('%', :query, '%'))
                   or lower(u.displayName) like lower(concat('%', :query, '%')))
            """)
    Page<User> searchActiveUsers(@Param("query") String query, Pageable pageable);

    @Query("""
            select u from User u
            where lower(u.username) like lower(concat('%', :query, '%'))
               or lower(u.displayName) like lower(concat('%', :query, '%'))
               or lower(u.email) like lower(concat('%', :query, '%'))
            """)
    Page<User> searchForAdmin(@Param("query") String query, Pageable pageable);
}
