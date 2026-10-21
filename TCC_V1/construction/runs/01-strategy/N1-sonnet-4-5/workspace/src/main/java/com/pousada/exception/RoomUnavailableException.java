package com.pousada.exception;

public class RoomUnavailableException extends RuntimeException {
    public RoomUnavailableException() {
        super("Quarto não disponível nas datas solicitadas");
    }
}
