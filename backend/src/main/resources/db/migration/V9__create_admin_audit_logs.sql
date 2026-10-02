CREATE TABLE admin_audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    admin_user_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    target_user_id BIGINT NULL,
    details VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_admin_audit_logs PRIMARY KEY (id),
    CONSTRAINT fk_admin_audit_admin FOREIGN KEY (admin_user_id) REFERENCES users(id),
    CONSTRAINT fk_admin_audit_target FOREIGN KEY (target_user_id) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_admin_audit_created ON admin_audit_logs(created_at DESC, id DESC);
CREATE INDEX idx_admin_audit_admin ON admin_audit_logs(admin_user_id, created_at DESC);
