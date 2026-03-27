package com.ucsg.api.intranet.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ucsg.api.intranet.model.Comunicaciones;
import com.ucsg.api.intranet.repository.ComunicacionesRepository;

@Service
public class ComunicacionesService {

    @Autowired
    private ComunicacionesRepository repository;

    public List<Comunicaciones> getComunicacionesEspecificas(Integer tipoEvento, Integer seccion, Integer tipSitio) {
        return repository.findComunicacionesEspecificas(tipoEvento, seccion, tipSitio);
    }

    public List<String> getDirImagenByCodNoticia(Integer codNoticia) {
        return repository.findDirImagenByCodNoticia(codNoticia);
    }
}
