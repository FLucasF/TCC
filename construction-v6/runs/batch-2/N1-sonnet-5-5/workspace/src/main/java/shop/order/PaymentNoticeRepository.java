package shop.order;

import java.util.Optional;

public interface PaymentNoticeRepository {
    Optional<PaymentNotice> find(String orderId, String transactionId);

    void insert(PaymentNotice notice);
}
