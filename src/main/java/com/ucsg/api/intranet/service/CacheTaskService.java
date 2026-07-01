package com.ucsg.api.intranet.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CacheTaskService {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheTaskService.class);
    
    @Scheduled(fixedRate = 60 * 60 * 1000) // Cada hora
    @CacheEvict(value = {"cumpleanioMes", "contactos", "cacheJerarquiaUnidades"}, allEntries = true)
    public void clearCache() {
        logger.info("Cache cleared");
    }
}
