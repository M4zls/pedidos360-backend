package com.pedidos360.identity.service;

public class AppUserNotFoundException extends RuntimeException {

    public AppUserNotFoundException(Long id) {
        super("No existe el usuario " + id);
    }
}
