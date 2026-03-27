package com.ucsg.api.intranet.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ucsg.api.intranet.model.Comunicaciones;
import com.ucsg.api.intranet.service.ComunicacionesService;

@RestController
@RequestMapping("/api/comunicaciones")
public class ComunicacionesController {

    @Autowired
    private ComunicacionesService service;

    @GetMapping("/especificas")
    public ResponseEntity<List<Comunicaciones>> getComunicacionesEspecificas(
            @RequestParam("tipoEvento") Integer tipoEvento,
            @RequestParam("seccion") Integer seccion,
            @RequestParam("tipSitio") Integer tipSitio) {
        List<Comunicaciones> comunicaciones = service.getComunicacionesEspecificas(tipoEvento, seccion, tipSitio);
        return ResponseEntity.ok(comunicaciones);
    }

    @GetMapping("/imagen")
    public ResponseEntity<java.util.Map<String, List<String>>> getDirImagen(
            @RequestParam("codNoticia") Integer codNoticia) {
        List<String> dirImagenes = service.getDirImagenByCodNoticia(codNoticia);
        return ResponseEntity.ok(java.util.Collections.singletonMap("dirImagenes", dirImagenes));
    }
}
