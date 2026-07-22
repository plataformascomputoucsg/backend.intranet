package com.ucsg.api.intranet.controller;

import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ucsg.api.intranet.model.CambioClaveRequest;
import com.ucsg.api.intranet.model.CambioClaveResponse;
import com.ucsg.api.intranet.repository.SiuClaveRepository.ResultadoEnvio;
import com.ucsg.api.intranet.service.SiuClaveService;

@RestController
@RequestMapping("/api/siu")
public class SiuController {

    /**
     * Solo se acepta el usuario del SIU: letras/números en segmentos separados
     * por punto (ej. nombre.apellido). NO se aceptan '@', espacios ni otros símbolos,
     * porque la gente suele equivocarse escribiendo su correo en vez del usuario.
     */
    private static final Pattern USUARIO_PATTERN =
        Pattern.compile("^[a-z0-9]+(\\.[a-z0-9]+)*$");

    @Autowired
    private SiuClaveService service;

    /**
     * POST /api/siu/cambio-clave
     * Body: { "usuario": "nombre.apellido" }
     *
     * 200 { empleado, email, mensaje } -> clave temporal enviada al correo institucional
     * 400                              -> usuario con formato inválido (símbolos, correo, vacío)
     * 404 { mensaje }                  -> usuario no existe o sin correo institucional
     * 429                              -> solicitudes repetidas muy seguidas del mismo usuario
     */
    @PostMapping("/cambio-clave")
    public ResponseEntity<CambioClaveResponse> cambioClave(@RequestBody CambioClaveRequest request) {
        String usuario = request.getUsuario() == null
                ? ""
                : request.getUsuario().trim().toLowerCase();

        if (!USUARIO_PATTERN.matcher(usuario).matches()) {
            return ResponseEntity.badRequest().build();
        }

        if (service.registrarIntentoEnCooldown(usuario)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        Optional<ResultadoEnvio> resultado = service.procesarCambioClave(usuario);

        if (resultado.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new CambioClaveResponse(null, null,
                    "No se encontró el usuario ingresado o no tiene un correo institucional registrado."));
        }

        ResultadoEnvio r = resultado.get();
        return ResponseEntity.ok(new CambioClaveResponse(r.getEmpleado(), r.getMail(), r.getMensaje()));
    }
}
