package com.ucsg.api.intranet.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cache")
public class CacheController {

    private static final Logger logger = LoggerFactory.getLogger(CacheController.class);

    @Autowired
    private CacheManager cacheManager;

    @Value("${app.cache.token}")
    private String cacheToken;

    // Limpia TODOS los cachés al instante (noticias, contactos,
    // cacheJerarquiaUnidades y cumpleanioMes).
    // Requiere el header X-Cache-Token con el valor de app.cache.token.
    @PostMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearAll(
            @RequestHeader(value = "X-Cache-Token", required = false) String token) {

        if (token == null || !token.equals(cacheToken)) {
            logger.warn("Intento de limpiar cache con token invalido");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("status", "error", "mensaje", "Token invalido"));
        }

        for (String name : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        }
        logger.info("Cache limpiado manualmente: {}", cacheManager.getCacheNames());
        return ResponseEntity.ok(Map.of(
            "status", "ok",
            "cachesLimpiados", cacheManager.getCacheNames()
        ));
    }
}
