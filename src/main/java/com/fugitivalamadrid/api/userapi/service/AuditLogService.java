package com.fugitivalamadrid.api.userapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    public void logCreated(String username) {
        log.info("[AUDIT] USER CREATED - username: {}", username);
    }

    public void logUpdated(Long id, String username) {
        log.info("[AUDIT] USER UPDATED - id: {}, username: {}", id, username);
    }

    public void logPartialUpdated(Long id) {
        log.info("[AUDIT] USER PARTIALLY UPDATED - id: {}", id);
    }

    public void logDeleted(Long id) {
        log.info("[AUDIT] USER DELETED - id: {}", id);
    }
}