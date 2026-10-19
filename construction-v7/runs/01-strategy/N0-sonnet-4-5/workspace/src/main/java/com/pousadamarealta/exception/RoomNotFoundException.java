package com.pousadamarealta.exception;

public class RoomNotFoundException extends ApiException {
    public RoomNotFoundException() {
        super("ROOM_NOT_FOUND", "Quarto não encontrado", 404);
    }
}
