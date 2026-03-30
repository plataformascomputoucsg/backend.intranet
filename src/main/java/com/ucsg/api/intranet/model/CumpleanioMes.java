package com.ucsg.api.intranet.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;


@Entity
@Table(name = "TRA_VW_CUMPLE_MES")
public class CumpleanioMes {

    // El nombre de la columna en Oracle (opcional si son iguales)
    @Id 
    @Column(name = "NOMBRE") 
    private String nombre; // Lo usamos como clave primaria simple (asumiendo que es único)

    @Column(name = "DIA")
    private String dia;

    @Column(name = "MES")
    private String mes;

    @Column(name = "CORREO")
    private String correo;

    public CumpleanioMes() {}

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    
    public String getDia() {
        return dia;
    }

    public void setDia(String dia) {
        this.dia = dia;
    }

    public String getMes() {
        return mes;
    }

    public void setMes(String mes) {
        this.mes = mes;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }
}
