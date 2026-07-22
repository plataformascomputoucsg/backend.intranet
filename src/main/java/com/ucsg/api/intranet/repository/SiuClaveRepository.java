package com.ucsg.api.intranet.repository;

import java.sql.CallableStatement;
import java.sql.Types;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Acceso a datos para el cambio/desbloqueo de clave del SIU.
 * Reemplaza al flujo PHP legacy (cambio.php) usando la base de claves (172.16.1.42).
 *
 * Toda la lógica (validar usuario, generar clave temporal, cambiarla y enviar el
 * correo) vive dentro del procedimiento SSB_KG_POLITICAS.SSB_PR_ENVIA_MAIL_SIU,
 * que corre con los privilegios de su dueño (SISEB). Por eso INTRANET solo
 * necesita EXECUTE sobre ese paquete y NO grants sobre las tablas internas.
 */
@Repository
public class SiuClaveRepository {

    @Autowired
    @Qualifier("claveJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    /** Resultado de los parámetros OUT del procedimiento. */
    public static class ResultadoEnvio {
        private final String empleado;
        private final String mail;
        private final String mensaje;

        public ResultadoEnvio(String empleado, String mail, String mensaje) {
            this.empleado = empleado;
            this.mail = mail;
            this.mensaje = mensaje;
        }

        public String getEmpleado() {
            return empleado;
        }

        public String getMail() {
            return mail;
        }

        public String getMensaje() {
            return mensaje;
        }
    }

    /**
     * Ejecuta el procedimiento de cambio/desbloqueo de clave para el usuario dado.
     *
     * SSB_KG_POLITICAS.SSB_PR_ENVIA_MAIL_SIU(
     *     PV_USUARIO  IN  VARCHAR2,   -- usuario del SIU (ej. nombre.apellido)
     *     PV_EMPLEADO OUT VARCHAR2,   -- nombre del empleado (para el saludo)
     *     PV_MAIL     OUT VARCHAR2,   -- correo institucional al que se envió la clave
     *     PV_MENSAJE  OUT VARCHAR2)   -- mensaje de estado / no encontrado
     */
    public ResultadoEnvio enviarMailSiu(String usuario) {
        return jdbcTemplate.execute(
            "{call SSB_KG_POLITICAS.SSB_PR_ENVIA_MAIL_SIU(?, ?, ?, ?)}",
            (CallableStatement cs) -> {
                cs.setString(1, usuario);
                cs.registerOutParameter(2, Types.VARCHAR);
                cs.registerOutParameter(3, Types.VARCHAR);
                cs.registerOutParameter(4, Types.VARCHAR);
                cs.execute();
                return new ResultadoEnvio(cs.getString(2), cs.getString(3), cs.getString(4));
            });
    }
}
