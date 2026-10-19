package br.com.marealta.dominio;

public enum CodigoErro {
    INVALID_REQUEST(400),
    ROOM_NOT_FOUND(404),
    RESERVATION_NOT_FOUND(404),
    ROOM_UNAVAILABLE(409),
    OPERATION_NOT_ALLOWED(409),
    CAPACITY_EXCEEDED(422);

    private final int statusHttp;

    CodigoErro(int statusHttp) {
        this.statusHttp = statusHttp;
    }

    public int statusHttp() {
        return statusHttp;
    }
}
