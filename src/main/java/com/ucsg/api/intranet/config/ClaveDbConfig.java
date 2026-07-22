package com.ucsg.api.intranet.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Conexión a la base de claves del SIU (172.16.1.42), la misma que usaba el
 * PHP legacy vía conectar_bdclave(). Es independiente del datasource principal
 * (UCSG66 en 172.16.1.66) que usan el resto de los endpoints.
 *
 * El DataSource no se expone como bean para no interferir con la
 * autoconfiguración de JPA del datasource principal.
 */
@Configuration
public class ClaveDbConfig {

    @Value("${app.datasource.clave.url}")
    private String url;

    @Value("${app.datasource.clave.username}")
    private String username;

    @Value("${app.datasource.clave.password}")
    private String password;

    @Bean(name = "claveJdbcTemplate")
    public JdbcTemplate claveJdbcTemplate() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, username, password);
        dataSource.setDriverClassName("oracle.jdbc.OracleDriver");

        // La base valida el nombre del programa cliente en un trigger de logon.
        // Nos identificamos con el nombre estándar del driver, autorizado para INTRANET.
        java.util.Properties props = new java.util.Properties();
        props.setProperty("v$session.program", "JDBC Thin Client");
        dataSource.setConnectionProperties(props);

        return new JdbcTemplate(dataSource);
    }
}
