package com.example.myapplication.Model;

public class Usuario {
    private String nombre;
    private String UID;

    public Usuario(String nombre, String UID) {
        this.nombre = nombre;
        this.UID = UID;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getUID() {
        return UID;
    }

    public void setUID(String UID) {
        this.UID = UID;
    }
}
