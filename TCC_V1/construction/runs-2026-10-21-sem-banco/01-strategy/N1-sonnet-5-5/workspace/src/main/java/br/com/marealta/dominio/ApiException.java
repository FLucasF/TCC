package br.com.marealta.dominio;

public class ApiException extends RuntimeException {

    public enum Codigo {
        INVALID_REQUEST(400),
        ROOM_NOT_FOUND(404),
        RESERVATION_NOT_FOUND(404),
        ROOM_UNAVAILABLE(409),
        OPERATION_NOT_ALLOWED(409),
        CAPACITY_EXCEEDED(422);

        public final int status;

        Codigo(int status) {
            this.status = status;
        }
    }

    private final Codigo codigo;

    public ApiException(Codigo codigo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
    }

    public Codigo codigo() {
        return codigo;
    }

    public static ApiException invalida(String mensagem) {
        return new ApiException(Codigo.INVALID_REQUEST, mensagem);
    }
}
