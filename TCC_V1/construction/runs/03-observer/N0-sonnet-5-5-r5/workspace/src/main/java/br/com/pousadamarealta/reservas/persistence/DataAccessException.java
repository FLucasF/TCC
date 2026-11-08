package br.com.pousadamarealta.reservas.persistence;

public class DataAccessException extends RuntimeException {
    public DataAccessException(Throwable cause) {
        super(cause);
    }
}
