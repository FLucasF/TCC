package shop.order;

/** The outcome of a bank payment notice, kept so a repeated notice gets the same answer. */
public record PaymentNotice(String orderId, String transactionId, String refusalCode) {
}
