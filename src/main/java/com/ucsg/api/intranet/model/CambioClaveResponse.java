package com.ucsg.api.intranet.model;

public class CambioClaveResponse {
    private String empleado;
    private String email;
    private String mensaje;

    public CambioClaveResponse() {
    }

    public CambioClaveResponse(String empleado, String email, String mensaje) {
        this.empleado = empleado;
        this.email = email;
        this.mensaje = mensaje;
    }

    public String getEmpleado() {
        return empleado;
    }

    public void setEmpleado(String empleado) {
        this.empleado = empleado;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
