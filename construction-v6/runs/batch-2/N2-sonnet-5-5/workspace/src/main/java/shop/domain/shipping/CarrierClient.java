package shop.domain.shipping;

/** Porta para a transportadora; trocar de transportadora é trocar a implementação. */
public interface CarrierClient {
    CarrierQuote quote(String zipCode, long weightGrams, String service);

    record CarrierQuote(long priceCents, int etaHours) {
    }

    class ZipNotServedException extends RuntimeException {
        public ZipNotServedException() {
            super("ZIP_NOT_SERVED", null, false, false);
        }
    }

    class CarrierDownException extends RuntimeException {
        public CarrierDownException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
