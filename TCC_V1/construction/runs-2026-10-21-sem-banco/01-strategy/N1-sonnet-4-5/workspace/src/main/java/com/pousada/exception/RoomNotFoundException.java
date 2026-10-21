package com.pousada.exception;

public class RoomNotFoundException extends RuntimeException {
    public RoomNotFoundException() {
        super("Quarto não encontrado");
    }
}
