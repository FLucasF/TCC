package shop.persistence;

import java.util.Optional;

/** Registro dos avisos de pagamento já processados, para tratar repetições. */
public interface PaymentRepository {
    Optional<PaymentRecord> find(String orderId, String transactionId);

    void record(String orderId, String transactionId, String errorCode);

    /** errorCode é null quando o primeiro aviso deu certo. */
    record PaymentRecord(String errorCode) {
    }
}
