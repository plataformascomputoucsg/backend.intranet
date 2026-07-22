package com.ucsg.api.intranet.model;

public class CambioClaveRequest {
    private String usuario;

    public CambioClaveRequest() {
    }

    public CambioClaveRequest(String usuario) {
        this.usuario = usuario;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }
}
