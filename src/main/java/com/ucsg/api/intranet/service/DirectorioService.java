package com.ucsg.api.intranet.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ucsg.api.intranet.model.ContactoDTO;
import com.ucsg.api.intranet.model.DirectorioPersona;
import com.ucsg.api.intranet.model.SubunidadResponse;
import com.ucsg.api.intranet.model.UnidadResponse;
import com.ucsg.api.intranet.repository.DirectorioRepository;

@Service
public class DirectorioService {
    @Autowired
    private DirectorioRepository repository;

    @Cacheable(value = "cacheJerarquiaUnidades")
    public List<UnidadResponse> obtenerJerarquiaPorUnidad() {
        List<DirectorioPersona> listaPlana = repository.findAllPlano();

        // 1. Agrupar por Código de Unidad
        Map<Integer, List<DirectorioPersona>> porUnidad = listaPlana.stream()
            .collect(Collectors.groupingBy(DirectorioPersona::getCodUnidad));

        return porUnidad.entrySet().stream().map(unidadEntry -> {
            UnidadResponse unidadDto = new UnidadResponse(unidadEntry.getKey());

            // 2. Dentro de cada unidad, agrupar por Subunidad Padre
            Map<Integer, List<DirectorioPersona>> porPadre = unidadEntry.getValue().stream()
                .collect(Collectors.groupingBy(DirectorioPersona::getCodSubunidadPadre));

            List<SubunidadResponse> facultades = porPadre.entrySet().stream().map(padreEntry -> {
                String nombrePadre = padreEntry.getValue().get(0).getSubunidadPadre();
                SubunidadResponse padreDto = new SubunidadResponse(padreEntry.getKey(), nombrePadre);

                // 3. Eliminar duplicados de las hijas con Set
                Set<SubunidadResponse.HijaDTO> hijasUnicas = padreEntry.getValue().stream()
                    .map(p -> new SubunidadResponse.HijaDTO(p.getCodSubunidad(), p.getSubunidad()))
                    .collect(Collectors.toSet());

                // Opcional: Ordenar las hijas alfabéticamente
                List<SubunidadResponse.HijaDTO> hijasOrdenadas = new ArrayList<>(hijasUnicas);
                hijasOrdenadas.sort(Comparator.comparing(SubunidadResponse.HijaDTO::getSubunidad));

                padreDto.setSubunidadesHijas(hijasOrdenadas);
                return padreDto;
            }).collect(Collectors.toList());

            unidadDto.setFacultades(facultades);
            return unidadDto;
        }).collect(Collectors.toList());
    }


    // Se coloca aquí para que cachee el resultado final (la lista de DTOs)
    // @Cacheable(value = "contactos", key = "{#codUnidad, #codSubunidadPadre}")
    // public List<ContactoDTO> obtenerContactos(Integer codUnidad, Integer codSubunidadPadre) {
    //     List<Object[]> resultados = repository.findContactosNative(codUnidad, codSubunidadPadre);
        
    //     return resultados.stream().map(row -> new ContactoDTO(
    //         (String) row[0],  // EXTENSION
    //         (String) row[1],  // TITULO_PROFESIONAL
    //         (String) row[2],  // APELLIDOS
    //         (String) row[3],  // NOMBRES
    //         (String) row[4],  // SUBUNIDAD_PADRE
    //         (String) row[5],  // SUBUNIDAD
    //         (String) row[6],  // CARGO
    //         (String) row[7],  // CORREO
    //         (String) row[8],  // EMPLEADO
    //         row[9] != null ? ((Number) row[9]).intValue() : null // ORDEN_SUBUNIDAD
    //     )).collect(Collectors.toList());
    // }

    // @Cacheable(value = "contactos", key = "#unidad + '-' + #padre")
    // public List<ContactoDTO> obtenerContactos(String unidad, String padre) {
    // List<Object[]> resultados;

    // // Lógica para determinar si se traen todos o se filtra
    // boolean traerTodos = (unidad == null || unidad.equalsIgnoreCase("all")) && 
    //                      (padre == null || padre.equalsIgnoreCase("all"));

    // if (traerTodos) {
    //     System.out.println("Consultando a Oracle: Todos los registros");
    //     resultados = repository.findAllContactosNative();
    // } else {
    //     System.out.println("Filtrando Oracle por Unidad: " + unidad + " y Padre: " + padre);
    //     // Convertimos a Integer solo si vamos a filtrar
    //     resultados = repository.findContactosNative(Integer.parseInt(unidad), Integer.parseInt(padre));
    // }

    // return resultados.stream().map(row -> new ContactoDTO(
    //     (String) row[0], (String) row[1], (String) row[2], (String) row[3],
    //     (String) row[4], (String) row[5], (String) row[6], (String) row[7],
    //     (String) row[8], row[9] != null ? ((Number) row[9]).intValue() : null, row[10] != null ? ((Number) row[10]).intValue() : null
    // )).collect(Collectors.toList());
    // }


    private String procesarFoto(Object fotoObj) {
        if (fotoObj == null) return null;
        try {
            if (fotoObj instanceof byte[]) {
                return java.util.Base64.getEncoder().encodeToString((byte[]) fotoObj);
            } else if (fotoObj instanceof java.sql.Blob) {
                java.sql.Blob blob = (java.sql.Blob) fotoObj;
                byte[] bytes = blob.getBytes(1, (int) blob.length());
                return java.util.Base64.getEncoder().encodeToString(bytes);
            } else if (fotoObj instanceof String) {
                return (String) fotoObj;
            }
        } catch (Exception e) {
            System.err.println("Error procesando foto BLOB: " + e.getMessage());
        }
        return null;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "contactos", key = "#unidad + '-' + #padre + '-' + #hija")
    public List<ContactoDTO> obtenerContactos(String unidad, String padre, String hija) {
        List<Object[]> resultados;

        // Normalizamos los valores para manejar nulos como "all"
        String u = (unidad == null) ? "all" : unidad;
        String p = (padre == null) ? "all" : padre;
        String h = (hija == null) ? "all" : hija;

        // CASO 1: Todos los registros (all, all, all)
        if (u.equalsIgnoreCase("all") && p.equalsIgnoreCase("all") && h.equalsIgnoreCase("all")) {
            resultados = repository.findAllContactosNative();
        } 
        // CASO 2: Filtrado por Unidad y Padre solamente
        else if (!u.equalsIgnoreCase("all") && !p.equalsIgnoreCase("all") && h.equalsIgnoreCase("all")) {
            resultados = repository.findContactosNative(Integer.parseInt(u), Integer.parseInt(p));
        }
        // CASO 3: Filtrado por los tres campos (Unidad, Padre y Subunidad Hija)
        else if (!u.equalsIgnoreCase("all") && !p.equalsIgnoreCase("all") && !h.equalsIgnoreCase("all")) {
            resultados = repository.findContactosConHijaNative(Integer.parseInt(u), Integer.parseInt(p), Integer.parseInt(h));
        }
        // CASO POR DEFECTO: (Puedes agregar más combinaciones si las necesitas)
        else {
            resultados = repository.findAllContactosNative();
        }

        return resultados.stream().map(row -> new ContactoDTO(
            (String) row[0], (String) row[1], (String) row[2], (String) row[3],
            procesarFoto(row[4]), (String) row[5], (String) row[6], (String) row[7],
            (String) row[8], (String) row[9], row[10] != null ? ((Number) row[10]).intValue() : null, row[11] != null ? ((Number) row[11]).intValue() : null
        )).collect(Collectors.toList());
}
}
