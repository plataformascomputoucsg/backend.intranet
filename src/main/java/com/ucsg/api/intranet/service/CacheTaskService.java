package com.ucsg.api.intranet.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CacheTaskService {

    private static final Logger logger = LoggerFactory.getLogger(CacheTaskService.class);

    // Noticias: refresco cada 30 minutos
    @Scheduled(fixedRate = 30 * 60 * 1000)
    @CacheEvict(value = "noticias", allEntries = true)
    public void clearNoticias() {
        logger.info("Cache de noticias limpiado");
    }

    // Directorio de contactos: refresco cada 4 horas
    @Scheduled(fixedRate = 4 * 60 * 60 * 1000)
    @CacheEvict(value = {"contactos", "cacheJerarquiaUnidades"}, allEntries = true)
    public void clearDirectorio() {
        logger.info("Cache de directorio limpiado");
    }

    // Cumpleaños: refresco cada hora
    @Scheduled(fixedRate = 60 * 60 * 1000)
    @CacheEvict(value = "cumpleanioMes", allEntries = true)
    public void clearCumpleanios() {
        logger.info("Cache de cumpleanios limpiado");
    }
}
