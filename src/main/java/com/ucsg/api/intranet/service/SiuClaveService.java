package com.ucsg.api.intranet.service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.ucsg.api.intranet.repository.SiuClaveRepository;
import com.ucsg.api.intranet.repository.SiuClaveRepository.ResultadoEnvio;

@Service
public class SiuClaveService {

    private static final Logger log = LoggerFactory.getLogger(SiuClaveService.class);

    /** Tiempo mínimo entre solicitudes del mismo usuario (evita spam de correos). */
    private static final long COOLDOWN_MS = 60_000;

    private final Map<String, Long> ultimasSolicitudes = new ConcurrentHashMap<>();

    @Autowired
    private SiuClaveRepository repository;

    /**
     * Registra el intento y retorna true si el usuario debe esperar
     * antes de volver a solicitar un cambio de clave.
     */
    public boolean registrarIntentoEnCooldown(String usuario) {
        long ahora = System.currentTimeMillis();
        Long anterior = ultimasSolicitudes.put(usuario, ahora);
        return anterior != null && (ahora - anterior) < COOLDOWN_MS;
    }

    /**
     * Procesa el cambio/desbloqueo de clave. El procedimiento de la base hace
     * todo el trabajo; aquí interpretamos el resultado.
     *
     * @return los datos del envío si el usuario existe, o vacío si no se encontró.
     */
    public Optional<ResultadoEnvio> procesarCambioClave(String usuario) {
        ResultadoEnvio resultado;
        try {
            resultado = repository.enviarMailSiu(usuario);
        } catch (DataAccessException ex) {
            // El procedimiento revienta con ORA-29532 (NullPointerException) cuando el
            // usuario no existe, en lugar de devolver un mensaje limpio. Lo tratamos
            // como "no encontrado". (Pendiente: que Centro de Cómputo maneje el caso
            // dentro del procedimiento y devuelva PV_MENSAJE en vez de lanzar la excepción.)
            if (esUsuarioNoEncontrado(ex)) {
                log.info("Cambio de clave: usuario '{}' no encontrado (ORA-29532 en el procedimiento)", usuario);
                return Optional.empty();
            }
            throw ex;
        }

        // Sin correo de destino => el usuario no existe o no tiene correo institucional.
        if (resultado.getMail() == null || resultado.getMail().isBlank()) {
            log.info("Cambio de clave: usuario '{}' no encontrado. Mensaje BD: {}",
                usuario, resultado.getMensaje());
            return Optional.empty();
        }

        log.info("Cambio de clave: clave temporal de '{}' enviada a {}", usuario, resultado.getMail());
        return Optional.of(resultado);
    }

    private boolean esUsuarioNoEncontrado(DataAccessException ex) {
        String mensaje = ex.getMostSpecificCause().getMessage();
        return mensaje != null
            && (mensaje.contains("ORA-29532") || mensaje.contains("NullPointerException"));
    }
}
