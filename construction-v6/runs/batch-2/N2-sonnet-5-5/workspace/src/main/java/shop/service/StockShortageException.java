package shop.service;

public class StockShortageException extends RuntimeException {
    public StockShortageException() {
        super("insufficient stock", null, false, false);
    }
}
