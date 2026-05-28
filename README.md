# Backend Intranet — API REST

API REST desarrollada con **Spring Boot 3** y **Java 17** que consume una base de datos **Oracle 12c**. Expone endpoints para el sistema de intranet de UCSG: comunicaciones, directorio de contactos y cumpleaños.

---

## Stack tecnológico

| Tecnología | Versión | Rol |
|---|---|---|
| Java | 17 | Lenguaje |
| Spring Boot | 3.5.10 | Framework principal |
| Spring Data JPA | — | Acceso a datos (ORM) |
| Spring Cache | Simple (RAM) | Caché en memoria |
| Oracle JDBC (`ojdbc11`) | Runtime | Driver de base de datos |
| Spring Actuator | — | Monitoreo |
| Maven | — | Gestor de dependencias |

---

## Estructura del proyecto

```
backend.intranet/
├── src/
│   └── main/
│       ├── java/com/ucsg/api/intranet/
│       │   ├── Application.java              # Entry point (@SpringBootApplication)
│       │   ├── controller/                   # Capa HTTP — recibe y responde requests
│       │   │   ├── ComunicacionesController.java
│       │   │   ├── CumpleanioController.java
│       │   │   └── DirectorioController.java
│       │   ├── model/                        # Entidades JPA y DTOs
│       │   │   ├── Comunicaciones.java       # Entidad → CCE_VW_COMUNICACIONES
│       │   │   ├── CumpleanioMes.java        # Entidad → TRA_VW_CUMPLE_MES
│       │   │   ├── DirectorioPersona.java    # Entidad → SCT_DIRECTORIO
│       │   │   ├── ContactoDTO.java          # DTO de respuesta de contactos
│       │   │   ├── SubunidadResponse.java    # DTO jerarquía (padre → hijas)
│       │   │   └── UnidadResponse.java       # DTO jerarquía (unidad → facultades)
│       │   ├── repository/                   # Capa de acceso a datos (Spring Data JPA)
│       │   │   ├── ComunicacionesRepository.java
│       │   │   ├── CumpleanioMesRepository.java
│       │   │   └── DirectorioRepository.java
│       │   └── service/                      # Lógica de negocio
│       │       ├── ComunicacionesService.java
│       │       ├── DirectorioService.java
│       │       └── CacheTaskService.java     # Tarea programada de limpieza de caché
│       └── resources/
│           └── application.properties        # Configuración de la app
├── Dockerfile
└── pom.xml
```

### Flujo de una petición

```
Request HTTP
    └─▶ Controller       (@RestController)
            └─▶ Service  (@Service)
                    └─▶ Repository  (@Repository / JpaRepository)
                                └─▶ Oracle DB
```

---

## Configuración

Las variables de entorno/configuración se definen en `src/main/resources/application.properties`.

```properties
# Puerto del servidor
server.port=8080

# Conexión Oracle
spring.datasource.url=jdbc:oracle:thin:@<HOST>:<PORT>:<SID>
spring.datasource.username=<USUARIO>
spring.datasource.password=<CONTRASEÑA>
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver

# JPA / Hibernate
spring.jpa.database-platform=org.hibernate.dialect.OracleDialect
spring.jpa.hibernate.ddl-auto=none
spring.jpa.open-in-view=false
spring.jpa.properties.hibernate.connection.handling_mode=DELAYED_ACQUISITION_AND_HOLD

# Caché en memoria
spring.cache.type=simple
```

> **Importante:** `ddl-auto=none` asegura que Hibernate **nunca toque el esquema** de Oracle. Todas las vistas/tablas son administradas externamente.

---

## Deploy

### Cómo funciona el Dockerfile (multi-stage build)

El proyecto usa una **imagen de dos etapas** para mantener la imagen final lo más liviana posible:

```dockerfile
# ETAPA 1 — BUILD
# Imagen pesada con Maven + JDK completo, solo para compilar
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests   # genera el .jar en /app/target/

# ETAPA 2 — RUNTIME
# Imagen mínima: solo el JRE (sin Maven, sin fuentes, sin nada innecesario)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

ENV JAVA_OPTS="-Xms512m -Xmx512m"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

| Etapa | Imagen base | Peso aprox. | Rol |
|---|---|---|---|
| Build | `maven:3.9.6-eclipse-temurin-17` | ~500 MB | Compilar y empaquetar |
| Runtime | `eclipse-temurin:17-jre-alpine` | ~85 MB | Correr el JAR en producción |

La imagen final solo contiene el JRE de Alpine y el `.jar`. Los fuentes, Maven y el JDK completo **no se incluyen** en el artefacto desplegado.

---

### Desarrollo local

```bash
# Opción A — Maven directo (requiere JDK 17 instalado)
./mvnw spring-boot:run

# Opción B — Docker local
docker build -t backend-intranet .
docker run --rm -p 8080:8080 backend-intranet
```

---

### Deploy en servidor (producción)

#### 1. Construir la imagen

En tu máquina local (o en el servidor si tiene acceso al repo):

```bash
# Desde la raíz del proyecto
docker build -t backend-intranet:latest .

# Si querés taggear con versión específica
docker build -t backend-intranet:1.0.0 .
```

#### 2. Transferir la imagen al servidor

**Opción A — Guardar y copiar el tar (sin registry):**

```bash
# En tu máquina: exportar la imagen
docker save backend-intranet:latest | gzip > backend-intranet.tar.gz

# Copiar al servidor vía SCP
scp backend-intranet.tar.gz usuario@<IP-SERVIDOR>:/opt/intranet/

# En el servidor: cargar la imagen
docker load < /opt/intranet/backend-intranet.tar.gz
```

**Opción B — Docker Registry privado (si está disponible):**

```bash
# En tu máquina: taggear y subir
docker tag backend-intranet:latest <registry>/<repo>/backend-intranet:latest
docker push <registry>/<repo>/backend-intranet:latest

# En el servidor: bajar
docker pull <registry>/<repo>/backend-intranet:latest
```

#### 3. Levantar el contenedor en el servidor

```bash
docker run -d \
  --name backend-intranet \
  --restart unless-stopped \
  -p 8080:8080 \
  -e JAVA_OPTS="-Xms512m -Xmx512m" \
  backend-intranet:latest
```

| Flag | Descripción |
|---|---|
| `-d` | Corre en background (detached) |
| `--name backend-intranet` | Nombre fijo para poder referenciarlo fácilmente |
| `--restart unless-stopped` | Se reinicia automáticamente si el servidor se reinicia o el proceso muere |
| `-p 8080:8080` | Mapea el puerto del host al del contenedor |
| `-e JAVA_OPTS` | Permite ajustar la memoria de la JVM sin reconstruir la imagen |

#### 4. Verificar que levantó correctamente

```bash
# Ver que el contenedor está corriendo
docker ps

# Ver los logs en tiempo real
docker logs -f backend-intranet

# Verificar el health del Actuator
curl http://localhost:8080/actuator/health
```

Respuesta esperada de Actuator:
```json
{ "status": "UP" }
```

---

### Actualizar a una nueva versión

```bash
# 1. Construir/cargar la nueva imagen (repetir pasos 1 y 2 de arriba)

# 2. Detener y eliminar el contenedor actual
docker stop backend-intranet
docker rm backend-intranet

# 3. Levantar con la nueva imagen (mismo comando del paso 3)
docker run -d \
  --name backend-intranet \
  --restart unless-stopped \
  -p 8080:8080 \
  -e JAVA_OPTS="-Xms512m -Xmx512m" \
  backend-intranet:latest
```

> **Downtime:** este flujo implica un breve corte de servicio entre el `docker stop` y el nuevo `docker run`. Si necesitás zero-downtime, considerá usar `docker-compose` con un proxy nginx, o implementar blue/green deployment.

---

### Gestión del contenedor en producción

```bash
# Ver estado
docker ps -a --filter "name=backend-intranet"

# Ver logs (últimas 100 líneas)
docker logs --tail 100 backend-intranet

# Seguir logs en tiempo real
docker logs -f backend-intranet

# Reiniciar sin recrear el contenedor
docker restart backend-intranet

# Detener (sin eliminar)
docker stop backend-intranet

# Iniciar nuevamente
docker start backend-intranet

# Eliminar completamente el contenedor (no la imagen)
docker rm -f backend-intranet
```

---

### Variables de entorno disponibles

Se pueden sobreescribir en el `docker run` con `-e` sin necesidad de reconstruir la imagen:

```bash
docker run -d \
  --name backend-intranet \
  --restart unless-stopped \
  -p 8080:8080 \
  -e JAVA_OPTS="-Xms256m -Xmx512m" \
  -e SPRING_DATASOURCE_URL="jdbc:oracle:thin:@<HOST>:<PORT>:<SID>" \
  -e SPRING_DATASOURCE_USERNAME="<USUARIO>" \
  -e SPRING_DATASOURCE_PASSWORD="<CONTRASEÑA>" \
  backend-intranet:latest
```

> **Importante:** Spring Boot mapea automáticamente variables de entorno a properties usando el formato `SPRING_DATASOURCE_URL` → `spring.datasource.url`. Esto permite externalizar credenciales del `application.properties` y pasarlas al momento de correr el contenedor — **nunca hardcodees credenciales en la imagen**.

---

### Troubleshooting de deploy

| Síntoma | Causa probable | Solución |
|---|---|---|
| `docker logs` muestra `Connection refused` a Oracle | El servidor no tiene acceso a la IP de Oracle | Verificar conectividad: `docker exec backend-intranet ping <IP-ORACLE>` |
| El contenedor se reinicia constantemente | Error en el startup (fallo de conexión a DB, port busy) | `docker logs backend-intranet` para ver el stacktrace completo |
| `ORA-12541: TNS: no listener` | Host o puerto de Oracle mal configurado | Revisar `SPRING_DATASOURCE_URL` en el `docker run` |
| `ORA-01017: invalid username/password` | Credenciales incorrectas | Revisar `-e SPRING_DATASOURCE_USERNAME` y `PASSWORD` |
| Puerto 8080 ya ocupado | Otro proceso usa el puerto | `netstat -tlnp \| grep 8080` y detener el proceso, o cambiar el puerto del host: `-p 9090:8080` |
| `java.lang.OutOfMemoryError` | Heap insuficiente | Aumentar `-Xmx`: `-e JAVA_OPTS="-Xms512m -Xmx1024m"` |

---

## Referencia de Endpoints

### 📢 Comunicaciones — `/api/comunicaciones`

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/comunicaciones/especificas` | Trae comunicaciones filtradas por tipo, sección y sitio |
| `GET` | `/api/comunicaciones/imagen` | Trae las URLs de imágenes de una noticia por código |

#### `GET /api/comunicaciones/especificas`

**Query params:**

| Parámetro | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `tipoEvento` | `Integer` | ✅ | Tipo de evento (ej: `1` = noticia) |
| `seccion` | `Integer` | ✅ | Sección de la comunicación |
| `tipSitio` | `Integer` | ✅ | Tipo de sitio donde aparece |

**Ejemplo:**
```
GET /api/comunicaciones/especificas?tipoEvento=1&seccion=2&tipSitio=1
```

**Respuesta `200 OK`:**
```json
[
  {
    "codigo": 123,
    "titulo": "Título de la noticia",
    "descripcion": "Descripción corta",
    "detalleDesc": "Texto completo...",
    "dirImagen": "/img/noticias/foto.jpg",
    "fecInicio": "2025-03-01T00:00:00.000+00:00",
    "categoria": "NOTICIAS",
    "highlight": "S",
    "tipoEvento": 1,
    "seccion": 2,
    "indice": 1,
    "tipSitio": 1,
    "destacado": "S",
    "subseccion": "side_news",
    "ubicacion": "Guayaquil",
    "descOrganiza": "Rectorado"
  }
]
```

---

#### `GET /api/comunicaciones/imagen`

**Query params:**

| Parámetro | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `codNoticia` | `Integer` | ✅ | Código único de la noticia |

**Ejemplo:**
```
GET /api/comunicaciones/imagen?codNoticia=456
```

**Respuesta `200 OK`:**
```json
{
  "dirImagenes": [
    "/img/noticias/foto1.jpg",
    "/img/noticias/foto2.jpg"
  ]
}
```

---

### 🎂 Cumpleaños — `/api/cumpleanios`

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/cumpleanios` | Trae todos los cumpleaños del mes actual |

#### `GET /api/cumpleanios`

Sin parámetros. Los datos se sirven desde caché (se invalida cada hora).

**Respuesta `200 OK`:**
```json
[
  {
    "nombre": "Juan Pérez",
    "dia": "15",
    "mes": "05",
    "correo": "jperez@ucsg.edu.ec"
  }
]
```

---

### 📋 Directorio — `/api/directorio`

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/api/directorio/unidades` | Devuelve la jerarquía completa de unidades y subunidades |
| `GET` | `/api/directorio/contactos` | Devuelve contactos con filtros opcionales |

#### `GET /api/directorio/unidades`

Sin parámetros. Devuelve el árbol completo: `Unidad → Facultad/Subunidad Padre → Subunidades Hijas`.

**Respuesta `200 OK`:**
```json
[
  {
    "codUnidad": 2,
    "facultades": [
      {
        "codSubunidadPadre": 184,
        "subunidadPadre": "Facultad de Ingeniería",
        "subunidadesHijas": [
          { "codSubunidad": 210, "subunidad": "Sistemas" },
          { "codSubunidad": 211, "subunidad": "Civil" }
        ]
      }
    ]
  }
]
```

---

#### `GET /api/directorio/contactos`

**Query params (todos opcionales):**

| Parámetro | Tipo | Descripción |
|---|---|---|
| `unidad` | `String` | Código de unidad. Si no se envía → todos |
| `padre` | `String` | Código de subunidad padre. Si no se envía → todos |
| `hija` | `String` | Código de subunidad hija. Si no se envía → todos |

**Casos de uso:**

| Escenario | Parámetros |
|---|---|
| Todos los contactos | Sin parámetros (o `unidad=all&padre=all`) |
| Por unidad y facultad | `?unidad=2&padre=184` |
| Por unidad, facultad y subunidad | `?unidad=2&padre=184&hija=210` |

**Ejemplo:**
```
GET /api/directorio/contactos?unidad=2&padre=184&hija=210
```

**Respuesta `200 OK`:**
```json
[
  {
    "extension": "1234",
    "tituloProfesional": "Ing.",
    "apellidos": "García López",
    "nombres": "María",
    "foto": "base64encodedstring...",
    "subunidadPadre": "Facultad de Ingeniería",
    "subunidad": "Sistemas",
    "cargo": "Docente",
    "correo": "mgarcia@ucsg.edu.ec",
    "empleado": "12345",
    "ordenSubunidad": 1,
    "codSubunidad": 210
  }
]
```

> **Nota:** El campo `foto` es un BLOB de Oracle convertido a **Base64**. Puede ser `null` si el empleado no tiene foto registrada.

---

## Caché

El proyecto usa Spring Cache en modo `simple` (HashMap en memoria RAM).

| Cache | Qué guarda | Invalidación |
|---|---|---|
| `cumpleanioMes` | Lista de cumpleaños del mes | Cada hora (tarea programada) |
| `cacheJerarquiaUnidades` | Árbol de unidades/subunidades | Al reiniciar la app |
| `contactos` | Lista de contactos por combinación de filtros | Al reiniciar la app |

La tarea de limpieza está en `CacheTaskService.java`:

```java
@Scheduled(fixedRate = 60 * 60 * 1000) // Cada hora
@CacheEvict(value = "cumpleanioMes", allEntries = true)
public void clearCache() { ... }
```

---

## Convenciones del proyecto

### Nomenclatura de archivos y paquetes

| Tipo | Sufijo | Ejemplo |
|---|---|---|
| Entidad JPA | *(ninguno)* | `Comunicaciones.java` |
| DTO | `DTO` | `ContactoDTO.java` |
| Response objeto complejo | `Response` | `UnidadResponse.java` |
| Repository | `Repository` | `ComunicacionesRepository.java` |
| Service | `Service` | `ComunicacionesService.java` |
| Controller | `Controller` | `ComunicacionesController.java` |

### Vistas y tablas de Oracle usadas

| Entidad | Vista/Tabla en Oracle | Schema |
|---|---|---|
| `Comunicaciones` | `CCE_VW_COMUNICACIONES` | Default |
| `Comunicaciones` (imágenes) | `CCE_VW_DETALLE_NOTICIAS` | Default |
| `CumpleanioMes` | `TRA_VW_CUMPLE_MES` | Default |
| `DirectorioPersona` | `SCT_DIRECTORIO` | `CALLCENTER` |

---

## Guía para agregar un nuevo módulo

Seguí este orden para no saltarte nada. Tomá como referencia el módulo de `Comunicaciones`.

### 1. Modelo (`model/`)

Si el dato viene directo de una tabla/vista de Oracle, creá una **entidad JPA**:

```java
package com.ucsg.api.intranet.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "NOMBRE_VISTA_O_TABLA")
public class MiNuevoModelo {

    @Id
    @Column(name = "COLUMNA_PK")
    private Integer id;

    @Column(name = "NOMBRE_COLUMNA_ORACLE")
    private String miCampo;

    // Constructor vacío obligatorio para JPA
    public MiNuevoModelo() {}

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getMiCampo() { return miCampo; }
    public void setMiCampo(String miCampo) { this.miCampo = miCampo; }
}
```

> **Regla:** siempre mapeá el nombre de columna exacto de Oracle con `@Column(name = "...")`. Oracle es sensible a mayúsculas/minúsculas en los nombres de columna.

---

### 2. DTO (`model/`)

Si necesitás transformar o aplanar el dato antes de enviarlo al cliente, creá un **DTO** (sin `@Entity`):

```java
package com.ucsg.api.intranet.model;

public class MiNuevoModeloDTO {
    private String campoUno;
    private Integer campoDos;

    // Constructor con todos los campos
    public MiNuevoModeloDTO(String campoUno, Integer campoDos) {
        this.campoUno = campoUno;
        this.campoDos = campoDos;
    }

    // Getters y Setters
    public String getCampoUno() { return campoUno; }
    public void setCampoUno(String campoUno) { this.campoUno = campoUno; }
    public Integer getCampoDos() { return campoDos; }
    public void setCampoDos(Integer campoDos) { this.campoDos = campoDos; }
}
```

> **Cuándo usar DTO vs Entidad:** Si la respuesta es exactamente la misma estructura que la vista Oracle → usá la entidad directamente. Si necesitás combinar campos, renombrarlos, transformar BLOBs, o armar estructuras jerárquicas → creá un DTO.

---

### 3. Repository (`repository/`)

```java
package com.ucsg.api.intranet.repository;

import com.ucsg.api.intranet.model.MiNuevoModelo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MiNuevoModeloRepository extends JpaRepository<MiNuevoModelo, Integer> {

    // Método derivado (Spring genera la query automáticamente)
    List<MiNuevoModelo> findByMiCampo(String miCampo);

    // Query nativa para casos complejos o con vistas Oracle
    @Query(value = "SELECT COLUMNA_PK, NOMBRE_COLUMNA_ORACLE FROM NOMBRE_VISTA_O_TABLA WHERE CONDICION = ?1",
           nativeQuery = true)
    List<MiNuevoModelo> findByCondicion(String condicion);

    // Query que retorna columnas sueltas (sin mapear a entidad)
    @Query(value = "SELECT SOLO_ESTA_COLUMNA FROM OTRA_VISTA WHERE COD = ?1", nativeQuery = true)
    List<String> findSoloEstaColumna(Integer cod);
}
```

> **Regla:** usá `nativeQuery = true` siempre que trabajes con vistas Oracle o queries complejas con `ROW_NUMBER()`, `PARTITION BY`, etc. Spring Data JPQL no soporta esas funciones de Oracle.

---

### 4. Service (`service/`)

```java
package com.ucsg.api.intranet.service;

import com.ucsg.api.intranet.model.MiNuevoModelo;
import com.ucsg.api.intranet.repository.MiNuevoModeloRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MiNuevoModeloService {

    @Autowired
    private MiNuevoModeloRepository repository;

    // Sin caché: dato en tiempo real
    public List<MiNuevoModelo> obtenerPorCondicion(String condicion) {
        return repository.findByCondicion(condicion);
    }

    // Con caché: útil para datos que no cambian frecuentemente
    @Cacheable(value = "miNuevoModeloCache", key = "#condicion")
    public List<MiNuevoModelo> obtenerConCache(String condicion) {
        return repository.findByCondicion(condicion);
    }
}
```

> Si agregás una nueva caché (`value = "miNuevoModeloCache"`), recordá agregarla al `CacheTaskService` para programar su limpieza.

---

### 5. Controller (`controller/`)

```java
package com.ucsg.api.intranet.controller;

import com.ucsg.api.intranet.model.MiNuevoModelo;
import com.ucsg.api.intranet.service.MiNuevoModeloService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mi-nuevo-modulo")
public class MiNuevoModeloController {

    @Autowired
    private MiNuevoModeloService service;

    // GET con query param obligatorio
    @GetMapping("/filtrado")
    public ResponseEntity<List<MiNuevoModelo>> getPorCondicion(
            @RequestParam("condicion") String condicion) {
        return ResponseEntity.ok(service.obtenerPorCondicion(condicion));
    }

    // GET con query param opcional
    @GetMapping("/todos")
    public ResponseEntity<List<MiNuevoModelo>> getTodos(
            @RequestParam(value = "filtro", required = false) String filtro) {
        return ResponseEntity.ok(service.obtenerPorCondicion(filtro));
    }

    // GET con path variable
    @GetMapping("/{id}")
    public ResponseEntity<MiNuevoModelo> getPorId(@PathVariable Integer id) {
        // Implementar lógica según necesidad
        return ResponseEntity.notFound().build();
    }
}
```

> **Regla:** el controller **nunca** contiene lógica de negocio. Solo recibe el request, llama al service, y devuelve la respuesta. Si te encontrás escribiendo `if/else` o queries en el controller, eso va en el service.

---

## Decisiones de diseño

### Por qué `nativeQuery = true` en los repositories

Las vistas de Oracle usan funciones específicas de ese motor (`ROW_NUMBER() OVER(PARTITION BY ...)`) que JPQL de Hibernate no soporta. Por eso todos los queries complejos usan SQL nativo.

### Por qué `ddl-auto=none`

La base de datos Oracle es administrada por el equipo de DBA. Hibernate **nunca debe intentar crear ni modificar** las tablas o vistas. Cualquier cambio de esquema se hace directamente en Oracle.

### Por qué `DELAYED_ACQUISITION_AND_HOLD`

Es necesario para leer BLOBs (como las fotos en `SCT_DIRECTORIO`) de Oracle. Sin esta configuración, la conexión se libera antes de que Hibernate termine de leer el stream del BLOB, causando errores.

### Fotos como Base64

Los campos `FOTO` en Oracle son de tipo `BLOB`. El service los convierte a Base64 antes de devolverlos al cliente para que el frontend los consuma directamente en un `<img src="data:image/...">`.
