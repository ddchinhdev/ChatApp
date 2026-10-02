package com.chatapp.repository;

import com.chatapp.entity.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
    @Override
    @EntityGraph(attributePaths = {"admin", "targetUser"})
    Page<AdminAuditLog> findAll(Pageable pageable);
}
