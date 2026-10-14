package reference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import reference.Purchase.Customer;
import reference.Purchase.Item;
import reference.Store.Failure;
import reference.Store.NotificationStatus;
import reference.Store.NotificationType;
import reference.Store.Order;
import reference.Store.Response;
import reference.Store.Status;
import reference.Store.Success;

/**
 * The order lifecycle, told as small stories: each test makes the calls in the order the website and
 * the bank would, and checks the answer, the stock, the credit balance and the notifications.
 */
class StoreTest {

    static final Instant NOW = Instant.parse("2026-10-14T15:00:00Z");
    static final String ANA = "12345678909";
    static final String BIA = "98765432100";

    /** A new store, with the initial catalog and a TransLog that always answers 33.10 in 41 hours. */
    Store store = new Store(Catalog.initial(), (zip, grams, service) -> new TransLog.Quote(3310, 41));

    /** A starting purchase: 2 shirts, store pickup, Pix, SILVER (earns 2% credit). */
    static Purchase purchase(String cpf, List<Item> items, String coupon) {
        return new Purchase(new Customer(cpf, cpf + "@exemplo.com"), items, "01310100", "SOUTHEAST", "STORE_PICKUP",
                coupon, "PIX", null, "SILVER");
    }

    static Purchase twoShirts(String cpf) {
        return purchase(cpf, List.of(new Item("CAM-001", 2)), null);
    }

    /** Registers the order and returns its id (the test fails if the registration is not a 201). */
    String createOrder(Purchase p) {
        Response<Order> response = store.createOrder(p, NOW);
        assertEquals(201, assertInstanceOf(Success.class, response).http());
        return ok(response).id;
    }

    /** Checks that the answer is a success and returns its body. */
    @SuppressWarnings("unchecked")
    static <T> T ok(Response<T> response) {
        Success<T> success = assertInstanceOf(Success.class, response, () -> "esperava sucesso, veio " + response);
        return success.body();
    }

    static void fails(int http, String error, Response<?> response) {
        Failure<?> failure = assertInstanceOf(Failure.class, response, () -> "esperava " + error + ", veio " + response);
        assertEquals(http, failure.http());
        assertEquals(error, failure.error());
    }

    int stock(String sku) {
        return ok(store.getProduct(sku)).stock();
    }

    BigDecimal balance(String cpf) {
        return ok(store.getCreditBalance(cpf));
    }

    List<NotificationType> notificationTypes(String cpf) {
        return ok(store.getNotifications(cpf)).stream().map(n -> n.type).toList();
    }

    static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    // ------------------------------------------------------------------ the happy path

    @Nested
    class HappyPath {
        @Test
        void createOrderChecksStockButDoesNotTakeIt() {
            String id = createOrder(twoShirts(ANA));
            Order order = ok(store.getOrder(id));
            assertEquals(Status.AWAITING_PAYMENT, order.status);
            assertEquals(50, stock("CAM-001"));                               // only taken on payment
            assertEquals(money("0.00"), balance(ANA));                         // the customer now exists, with a zero balance
            assertNull(order.trackingCode);
        }

        @Test
        void theOrderSummaryIsTheSameAsTheCheckoutSummary() {
            Purchase p = twoShirts(ANA);
            Result.Summary checkout = ok(store.checkoutSummary(p));
            assertEquals(checkout, ok(store.getOrder(createOrder(p))).summary);
        }

        @Test
        void paymentTakesTheStockAddsTheCreditAndNotifies() {
            String id = createOrder(twoShirts(ANA));
            Order order = ok(store.pay(id, "TX-1", NOW.plusSeconds(60)));
            assertEquals(Status.PAID, order.status);
            assertEquals(48, stock("CAM-001"));
            assertEquals(money("3.20"), balance(ANA));                         // 2% of 159.80 = 3.196, which becomes 3.20
            assertEquals(List.of(NotificationType.PAYMENT_APPROVED), notificationTypes(ANA));
        }

        @Test
        void shipAndDeliver() {
            String id = createOrder(twoShirts(ANA));
            ok(store.pay(id, "TX-1", NOW));
            Order shipped = ok(store.ship(id, "BR123456789"));
            assertEquals(Status.SHIPPED, shipped.status);
            assertEquals("BR123456789", shipped.trackingCode);
            assertEquals(Status.DELIVERED, ok(store.deliver(id)).status);
            // delivery creates no notification
            assertEquals(List.of(NotificationType.PAYMENT_APPROVED, NotificationType.ORDER_SHIPPED), notificationTypes(ANA));
        }
    }

    // ------------------------------------------------------------------ the forbidden transitions

    @Nested
    class ForbiddenTransitions {
        @Test void shipBeforePaying() { fails(409, "OPERATION_NOT_ALLOWED", store.ship(createOrder(twoShirts(ANA)), "BR1")); }

        @Test void deliverBeforeShipping() {
            String id = createOrder(twoShirts(ANA));
            ok(store.pay(id, "TX-1", NOW));
            fails(409, "OPERATION_NOT_ALLOWED", store.deliver(id));
        }

        @Test void cancelAfterShipping() {
            String id = createOrder(twoShirts(ANA));
            ok(store.pay(id, "TX-1", NOW));
            ok(store.ship(id, "BR1"));
            fails(409, "OPERATION_NOT_ALLOWED", store.cancel(id));
        }

        @Test void cancelTwice() {
            String id = createOrder(twoShirts(ANA));
            ok(store.cancel(id));
            fails(409, "OPERATION_NOT_ALLOWED", store.cancel(id));
        }

        @Test void payACancelledOrder() {
            String id = createOrder(twoShirts(ANA));
            ok(store.cancel(id));
            fails(409, "OPERATION_NOT_ALLOWED", store.pay(id, "TX-1", NOW));
        }

        @Test void decision3_anotherTransactionOnAPaidOrder() {
            String id = createOrder(twoShirts(ANA));
            ok(store.pay(id, "TX-1", NOW));
            fails(409, "OPERATION_NOT_ALLOWED", store.pay(id, "TX-2", NOW));
            assertEquals(48, stock("CAM-001"));                               // nothing changed
        }
    }

    // ------------------------------------------------------------------ the cancellation

    @Nested
    class Cancellation {
        @Test
        void cancellingAPaidOrderGivesBackStockAndCredit() {
            String id = createOrder(twoShirts(ANA));
            ok(store.pay(id, "TX-1", NOW));
            assertEquals(Status.CANCELLED, ok(store.cancel(id)).status);
            assertEquals(50, stock("CAM-001"));
            assertEquals(money("0.00"), balance(ANA));
            assertEquals(List.of(NotificationType.PAYMENT_APPROVED, NotificationType.ORDER_CANCELLED), notificationTypes(ANA));
        }

        @Test
        void cancellingAnUnpaidOrderOnlyNotifies() {
            String id = createOrder(twoShirts(ANA));
            ok(store.cancel(id));
            assertEquals(50, stock("CAM-001"));
            assertEquals(money("0.00"), balance(ANA));
            assertEquals(List.of(NotificationType.ORDER_CANCELLED), notificationTypes(ANA));
        }
    }

    // ------------------------------------------------------------------ the 48 hours

    @Nested
    class PaymentDeadline {
        @Test void exactlyFortyEightHoursIsAccepted() {
            String id = createOrder(twoShirts(ANA));
            assertEquals(Status.PAID, ok(store.pay(id, "TX-1", NOW.plus(Duration.ofHours(48)))).status);
        }

        @Test void oneSecondLateExpiresAndCancels() {
            String id = createOrder(twoShirts(ANA));
            fails(409, "ORDER_EXPIRED", store.pay(id, "TX-1", NOW.plus(Duration.ofHours(48)).plusSeconds(1)));
            assertEquals(Status.CANCELLED, ok(store.getOrder(id)).status);
            assertEquals(50, stock("CAM-001"));
            assertEquals(List.of(NotificationType.ORDER_CANCELLED), notificationTypes(ANA));
        }

        @Test void decision4_withoutAPaymentTheOrderKeepsWaiting() {
            String id = createOrder(twoShirts(ANA));
            // no notice arrived: looking it up after the 48 hours changes nothing (the reference does not even look at the clock here)
            assertEquals(Status.AWAITING_PAYMENT, ok(store.getOrder(id)).status);
            ok(store.cancel(id));                                             // and the customer can still cancel
        }
    }

    // ------------------------------------------------------------------ the repeated notice (decision 2)

    @Nested
    class RepeatedPayment {
        @Test void sameTransactionTwiceChangesNothing() {
            String id = createOrder(twoShirts(ANA));
            ok(store.pay(id, "TX-1", NOW));
            Order again = ok(store.pay(id, "TX-1", NOW));
            assertEquals(Status.PAID, again.status);
            assertEquals(48, stock("CAM-001"));                               // taken only once
            assertEquals(money("3.20"), balance(ANA));                         // the credit came in only once
            assertEquals(List.of(NotificationType.PAYMENT_APPROVED), notificationTypes(ANA));
        }

        @Test void sameTransactionAfterShippingReturnsTheOrderAsItIsNow() {
            String id = createOrder(twoShirts(ANA));
            ok(store.pay(id, "TX-1", NOW));
            ok(store.ship(id, "BR1"));
            assertEquals(Status.SHIPPED, ok(store.pay(id, "TX-1", NOW)).status);
        }

        @Test void decision2_sameTransactionAfterAnExpirationGetsTheSameRefusal() {
            String id = createOrder(twoShirts(ANA));
            Instant late = NOW.plus(Duration.ofHours(49));
            fails(409, "ORDER_EXPIRED", store.pay(id, "TX-1", late));
            fails(409, "ORDER_EXPIRED", store.pay(id, "TX-1", late));          // the same refusal, not a 200
            assertEquals(List.of(NotificationType.ORDER_CANCELLED), notificationTypes(ANA));  // no new notification
        }

        @Test void anotherTransactionAfterAnExpirationIsNotAllowed() {
            String id = createOrder(twoShirts(ANA));
            fails(409, "ORDER_EXPIRED", store.pay(id, "TX-1", NOW.plus(Duration.ofHours(49))));
            fails(409, "OPERATION_NOT_ALLOWED", store.pay(id, "TX-2", NOW));
        }
    }

    // ------------------------------------------------------------------ the last unit

    @Nested
    class LastItem {
        @Test void twoOrdersForTheLastBackpackOnlyOneIsPaid() {
            Purchase backpack = purchase(ANA, List.of(new Item("MOC-001", 1)), null);
            String first = createOrder(backpack);
            String second = createOrder(purchase(BIA, List.of(new Item("MOC-001", 1)), null));   // both register: nothing is reserved
            ok(store.pay(first, "TX-1", NOW));
            fails(409, "INSUFFICIENT_STOCK", store.pay(second, "TX-2", NOW));
            assertEquals(0, stock("MOC-001"));                                // never negative
            assertEquals(Status.CANCELLED, ok(store.getOrder(second)).status);
            assertEquals(List.of(NotificationType.ORDER_CANCELLED), notificationTypes(BIA));
            assertEquals(money("0.00"), balance(BIA));                         // the loser earns no credit
        }

        @Test void everyItemNeedsStockNotJustOne() {
            // Ana wants a backpack + a shirt; Bia takes the last backpack first. The shirt has stock, but that is not enough.
            String ana = createOrder(purchase(ANA, List.of(new Item("MOC-001", 1), new Item("CAM-001", 1)), null));
            ok(store.pay(createOrder(purchase(BIA, List.of(new Item("MOC-001", 1)), null)), "TX-BIA", NOW));
            fails(409, "INSUFFICIENT_STOCK", store.pay(ana, "TX-ANA", NOW));
            assertEquals(50, stock("CAM-001"));                               // all or nothing: the shirt was not taken
        }

        @Test void afterTheLastBackpackIsSoldANewCheckoutIsRefused() {
            ok(store.pay(createOrder(purchase(ANA, List.of(new Item("MOC-001", 1)), null)), "TX-1", NOW));
            fails(422, "INSUFFICIENT_STOCK", store.checkoutSummary(purchase(BIA, List.of(new Item("MOC-001", 1)), null)));
        }
    }

    // ------------------------------------------------------------------ the first purchase (WELCOME10)

    @Nested
    class FirstPurchase {
        Purchase welcome(String cpf) {
            return purchase(cpf, List.of(new Item("CAM-001", 1)), "WELCOME10");
        }

        @Test void welcomeOnTheFirstPurchase() { ok(store.checkoutSummary(welcome(ANA))); }

        @Test void anUnpaidOrderStillCountsAsFirstPurchase() {
            createOrder(twoShirts(ANA));
            ok(store.checkoutSummary(welcome(ANA)));
        }

        @Test void afterAPaidOrderWelcomeDoesNotApply() {
            ok(store.pay(createOrder(twoShirts(ANA)), "TX-1", NOW));
            fails(422, "COUPON_NOT_APPLICABLE", store.checkoutSummary(welcome(ANA)));
        }

        @Test void aCancelledPaidOrderDoesNotCount() {
            String id = createOrder(twoShirts(ANA));
            ok(store.pay(id, "TX-1", NOW));
            ok(store.cancel(id));
            ok(store.checkoutSummary(welcome(ANA)));
        }
    }

    // ------------------------------------------------------------------ the operations' errors

    @Nested
    class OperationErrors {
        @Test void unknownOrder() {
            fails(404, "ORDER_NOT_FOUND", store.getOrder("999"));
            fails(404, "ORDER_NOT_FOUND", store.pay("999", "TX-1", NOW));
            fails(404, "ORDER_NOT_FOUND", store.ship("999", "BR1"));
            fails(404, "ORDER_NOT_FOUND", store.deliver("999"));
            fails(404, "ORDER_NOT_FOUND", store.cancel("999"));
        }

        @Test void decision1_missingFieldsAreInvalidOrder() {
            String id = createOrder(twoShirts(ANA));
            fails(422, "INVALID_ORDER", store.pay(id, null, NOW));
            fails(422, "INVALID_ORDER", store.pay(id, " ", NOW));
            fails(422, "INVALID_ORDER", store.pay(id, "TX-1", null));
            fails(422, "INVALID_ORDER", store.ship(id, null));
        }

        @Test void decision1_missingFieldComesBeforeNotAllowed() {
            String id = createOrder(twoShirts(ANA));                         // awaiting payment: shipping is not allowed...
            fails(422, "INVALID_ORDER", store.ship(id, ""));                  // ...but the missing field comes first
        }

        @Test void aRefusedCheckoutDoesNotCreateTheCustomer() {
            fails(422, "INVALID_CLUB_TIER", store.createOrder(new Purchase(new Customer(ANA, "a@b.com"), List.of(new Item("CAM-001", 1)),
                    "01310100", "SOUTHEAST", "STORE_PICKUP", null, "PIX", null, "DIAMOND"), NOW));
            fails(404, "CUSTOMER_NOT_FOUND", store.getCreditBalance(ANA));
        }

        @Test void decision6_notificationsOfAnUnknownCustomer() {
            fails(404, "CUSTOMER_NOT_FOUND", store.getNotifications(ANA));
        }

        @Test void unknownProductAndNotification() {
            fails(404, "PRODUCT_NOT_FOUND", store.getProduct("XYZ-999"));
            fails(404, "NOTIFICATION_NOT_FOUND", store.markAsRead("999"));
        }
    }

    // ------------------------------------------------------------------ the credit (decision 5) and the notifications

    @Nested
    class CreditAndNotifications {
        @Test void decision5_theCreditIsTheOneInTheRegisteredSummary() {
            String id = createOrder(purchase(ANA, List.of(new Item("TEN-001", 2)), null));   // SILVER: 2% of 499.80
            BigDecimal registered = ok(store.getOrder(id)).summary.nextPurchaseCredit();
            ok(store.pay(id, "TX-1", NOW));
            assertEquals(registered, balance(ANA));
            assertEquals(money("10.00"), registered);                          // 9.996, which becomes 10.00
        }

        @Test void creditAddsUpAcrossOrders() {
            ok(store.pay(createOrder(twoShirts(ANA)), "TX-1", NOW));
            ok(store.pay(createOrder(twoShirts(ANA)), "TX-2", NOW));
            assertEquals(money("6.40"), balance(ANA));
        }

        @Test void notificationsAreKeptPerCustomerInOrder() {
            String id = createOrder(twoShirts(ANA));
            createOrder(twoShirts(BIA));
            ok(store.pay(id, "TX-1", NOW));
            ok(store.ship(id, "BR1"));
            assertEquals(List.of(NotificationType.PAYMENT_APPROVED, NotificationType.ORDER_SHIPPED), notificationTypes(ANA));
            assertEquals(List.of(), notificationTypes(BIA));
        }

        @Test void markAsReadTwiceStaysRead() {
            ok(store.pay(createOrder(twoShirts(ANA)), "TX-1", NOW));
            String notificationId = ok(store.getNotifications(ANA)).get(0).id;
            assertEquals(NotificationStatus.READ, ok(store.markAsRead(notificationId)).status);
            assertEquals(NotificationStatus.READ, ok(store.markAsRead(notificationId)).status);
        }
    }
}
