package com.ucsg.api.intranet.model;

public class ContactoDTO {
    private String extension;
    private String tituloProfesional;
    private String apellidos;
    private String nombres;
    private String foto;
    private String subunidadPadre;
    private String subunidad;
    private String cargo;
    private String correo;
    private String empleado;
    private Integer ordenSubunidad;
    private Integer codSubunidad;

    // Constructor, Getters y Setters
    public ContactoDTO(String extension, String tituloProfesional, String apellidos, 
                       String nombres, String foto, String subunidadPadre, String subunidad, 
                       String cargo, String correo, String empleado, Integer ordenSubunidad, Integer codSubunidad) {
        this.extension = extension;
        this.tituloProfesional = tituloProfesional;
        this.apellidos = apellidos;
        this.nombres = nombres;
        this.foto = foto;
        this.subunidadPadre = subunidadPadre;
        this.subunidad = subunidad;
        this.cargo = cargo;
        this.correo = correo;
        this.empleado = empleado;
        this.ordenSubunidad = ordenSubunidad;
        this.codSubunidad = codSubunidad;
    }

    // Getters y Setters
    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getTituloProfesional() {
        return tituloProfesional;
    }

    public void setTituloProfesional(String tituloProfesional) {
        this.tituloProfesional = tituloProfesional;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }

    public String getSubunidadPadre() {
        return subunidadPadre;
    }

    public void setSubunidadPadre(String subunidadPadre) {
        this.subunidadPadre = subunidadPadre;
    }

    public String getSubunidad() {
        return subunidad;
    }

    public void setSubunidad(String subunidad) {
        this.subunidad = subunidad;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getEmpleado() {
        return empleado;
    }

    public void setEmpleado(String empleado) {
        this.empleado = empleado;
    }

    public Integer getOrdenSubunidad() {
        return ordenSubunidad;
    }

    public void setOrdenSubunidad(Integer ordenSubunidad) {
        this.ordenSubunidad = ordenSubunidad;
    }

    public Integer getCodSubunidad() {
        return codSubunidad;
    }

    public void setCodSubunidad(Integer codSubunidad) {
        this.codSubunidad = codSubunidad;
    }
}
