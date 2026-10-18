package shop.domain;

import java.math.BigDecimal;
import java.util.Optional;

/** A carrier that quotes freight and lead time. Swapping carriers means another implementation. */
public interface CarrierGateway {
    record Quote(BigDecimal cost, int days) {
    }

    /** Thrown when the carrier cannot be reached or answers something unexpected. */
    class UnavailableException extends RuntimeException {
        public UnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /** Empty when the carrier does not serve the zip code. */
    Optional<Quote> quote(String zipCode, int weightGrams, String service) throws UnavailableException;
}
