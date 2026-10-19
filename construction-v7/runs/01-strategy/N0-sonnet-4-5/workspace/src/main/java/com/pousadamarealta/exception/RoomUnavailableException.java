package com.pousadamarealta.exception;

public class RoomUnavailableException extends ApiException {
    public RoomUnavailableException() {
        super("ROOM_UNAVAILABLE", "Quarto não está disponível nas datas selecionadas", 409);
    }
}
