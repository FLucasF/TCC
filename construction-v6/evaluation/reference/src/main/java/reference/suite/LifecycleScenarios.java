package reference.suite;

import java.time.Duration;
import java.util.List;

import reference.Purchase;
import reference.Purchase.Customer;
import reference.Purchase.Item;
import reference.TransLog;

/**
 * The order lifecycle, told as short stories. Each scenario makes the calls in the order the site and
 * the bank would, and the {@link Lockstep} compares every answer with the reference model.
 *
 * Every scenario uses its own customers, so they do not interfere with each other. They do share the
 * stock, so the scarce products are budgeted: the only backpack (MOC-001) is used once, and the jackets
 * (JAQ-001, 3 units) leave 2 for the concurrency checks that run afterwards.
 */
public final class LifecycleScenarios {

    static final Duration ONE_MINUTE = Duration.ofMinutes(1);
    static final Duration FORTY_EIGHT_HOURS = Duration.ofHours(48);
    static final TransLog.Answer QUOTE = new TransLog.Quote(3310, 41);

    private final Lockstep s;

    public LifecycleScenarios(Lockstep lockstep) {
        this.s = lockstep;
    }

    public void runAll() {
        payShipAndDeliver();
        forbiddenTransitions();
        cancellation();
        paymentDeadline();
        repeatedPayment();
        theLastBackpack();
        everyItemNeedsStock();
        firstPurchase();
        operationErrors();
        creditAndNotifications();
        theTransLogWhenCreatingAnOrder();
    }

    void payShipAndDeliver() {
        String ana = cpf(1);
        s.begin("lifecycle", "pay, ship and deliver");
        s.checkoutSummary("summary before ordering", pickup(ana, "CAM-001", 2), null);
        s.createOrder("a1", pickup(ana, "CAM-001", 2), null);
        s.getProduct("CAM-001");                                   // registering does not take the stock
        s.getCustomer(ana);                                         // the customer now exists, with zero balance
        s.pay("a1", "TX-1", ONE_MINUTE);
        s.getProduct("CAM-001");
        s.getCustomer(ana);
        s.ship("a1", "BR123456789");
        s.deliver("a1");
        s.getOrder("a1");
        s.getNotifications(ana);                                    // approved, shipped (delivery has no notification)
        s.end();
    }

    void forbiddenTransitions() {
        String bia = cpf(2);
        s.begin("lifecycle", "forbidden transitions");
        s.createOrder("b1", pickup(bia, "CAM-001", 1), null);
        s.ship("b1", "BR1");                                        // not paid yet
        s.deliver("b1");                                            // not shipped yet
        s.pay("b1", "TX-1", ONE_MINUTE);
        s.deliver("b1");                                            // paid, but not shipped
        s.pay("b1", "TX-2", ONE_MINUTE);                            // another transaction on a paid order
        s.ship("b1", "BR1");
        s.cancel("b1");                                             // shipped: too late to cancel
        s.createOrder("b2", pickup(bia, "CAM-001", 1), null);
        s.cancel("b2");
        s.cancel("b2");                                             // cancelling twice
        s.pay("b2", "TX-3", ONE_MINUTE);                            // paying a cancelled order
        s.getNotifications(bia);
        s.end();
    }

    void cancellation() {
        String caio = cpf(3);
        s.begin("lifecycle", "cancelling a paid and an unpaid order");
        s.createOrder("c1", pickup(caio, "CAM-001", 2), null);
        s.pay("c1", "TX-1", ONE_MINUTE);
        s.cancel("c1");                                             // gives the stock and the credit back
        s.getProduct("CAM-001");
        s.getCustomer(caio);
        s.createOrder("c2", pickup(caio, "CAM-001", 1), null);
        s.cancel("c2");                                             // nothing to give back, but still notifies
        s.getNotifications(caio);
        s.end();
    }

    void paymentDeadline() {
        String duda = cpf(4);
        s.begin("lifecycle", "the 48-hour deadline");
        s.createOrder("d1", pickup(duda, "CAM-001", 1), null);
        s.pay("d1", "TX-1", FORTY_EIGHT_HOURS);                     // exactly 48 hours: still accepted
        s.createOrder("d2", pickup(duda, "CAM-001", 1), null);
        s.pay("d2", "TX-2", FORTY_EIGHT_HOURS.plusSeconds(1));      // one second late: expired, cancelled
        s.getOrder("d2");
        s.pay("d2", "TX-2", FORTY_EIGHT_HOURS.plusSeconds(1));      // the same notice again: the same refusal
        s.pay("d2", "TX-9", ONE_MINUTE);                            // another transaction: not allowed
        s.createOrder("d3", pickup(duda, "CAM-001", 1), null);
        s.getOrder("d3");                                           // no payment arrived: still waiting
        s.cancel("d3");
        s.getNotifications(duda);
        s.end();
    }

    void repeatedPayment() {
        String eva = cpf(5);
        s.begin("lifecycle", "the same payment notice twice");
        s.createOrder("e1", pickup(eva, "CAM-001", 2), null);
        s.pay("e1", "TX-1", ONE_MINUTE);
        s.pay("e1", "TX-1", ONE_MINUTE);                            // nothing new: same stock, same credit
        s.getProduct("CAM-001");
        s.getCustomer(eva);
        s.ship("e1", "BR1");
        s.pay("e1", "TX-1", ONE_MINUTE);                            // still 200, with the order as it is now
        s.getNotifications(eva);
        s.end();
    }

    void theLastBackpack() {
        String fabio = cpf(6), gabi = cpf(7), hugo = cpf(8);
        s.begin("lifecycle", "two orders for the last backpack");
        s.createOrder("f1", pickup(fabio, "MOC-001", 1), null);
        s.createOrder("f2", pickup(gabi, "MOC-001", 1), null);     // both register: registering does not reserve
        s.pay("f1", "TX-1", ONE_MINUTE);
        s.pay("f2", "TX-2", ONE_MINUTE);                            // no stock left: refused and cancelled
        s.getProduct("MOC-001");
        s.getOrder("f2");
        s.getCustomer(gabi);
        s.getNotifications(gabi);
        s.checkoutSummary("a third customer tries", pickup(hugo, "MOC-001", 1), null);
        s.end();
    }

    void everyItemNeedsStock() {
        String ivo = cpf(9), julia = cpf(10);
        s.begin("lifecycle", "every item needs stock, not just one");
        s.createOrder("g1", pickup(ivo, "JAQ-001", 3, "CAM-001", 1), null);
        s.createOrder("g2", pickup(julia, "JAQ-001", 1), null);
        s.pay("g2", "TX-1", ONE_MINUTE);                            // now only 2 jackets are left
        s.pay("g1", "TX-2", ONE_MINUTE);                            // the shirt is there, the 3 jackets are not
        s.getProduct("JAQ-001");
        s.getProduct("CAM-001");                                    // all or nothing: the shirt was not taken
        s.end();
    }

    void firstPurchase() {
        String karen = cpf(11);
        Purchase welcome = new Purchase(new Customer(karen, karen + "@exemplo.com"), List.of(new Item("CAM-001", 1)),
                "01310100", "SOUTHEAST", "STORE_PICKUP", "WELCOME10", "PIX", null, "SILVER");
        s.begin("lifecycle", "WELCOME10 only on the first purchase");
        s.checkoutSummary("before any order", welcome, null);
        s.createOrder("h1", pickup(karen, "CAM-001", 1), null);
        s.checkoutSummary("with an unpaid order", welcome, null);   // an unpaid order does not count
        s.pay("h1", "TX-1", ONE_MINUTE);
        s.checkoutSummary("after a paid order", welcome, null);     // not applicable any more
        s.cancel("h1");
        s.checkoutSummary("after the paid order was cancelled", welcome, null);  // a cancelled order does not count
        s.end();
    }

    void operationErrors() {
        String leo = cpf(12), nobody = cpf(13), refused = cpf(14);
        s.begin("lifecycle", "errors of the order operations");
        s.unknownOrder();
        s.createOrder("i1", pickup(leo, "CAM-001", 1), null);
        s.pay("i1", null, ONE_MINUTE);                              // no transactionId
        s.pay("i1", " ", ONE_MINUTE);                               // a blank transactionId
        s.pay("i1", "TX-1", null);                                  // no paidAt
        s.ship("i1", "");                                           // no trackingCode: 422 comes before the 409
        s.createOrder("i2", new Purchase(new Customer(refused, refused + "@exemplo.com"), List.of(new Item("CAM-001", 1)),
                "01310100", "SOUTHEAST", "STORE_PICKUP", null, "PIX", null, "DIAMOND"), null);
        s.getCustomer(refused);                                     // a refused order does not create the customer
        s.getNotifications(nobody);                                 // a CPF without orders: 404
        s.getProduct("XYZ-999");
        s.unknownNotification();
        s.end();
    }

    void creditAndNotifications() {
        String mia = cpf(15);
        s.begin("lifecycle", "the credit adds up and notifications are read");
        s.createOrder("j1", pickup(mia, "CAM-001", 2), null);
        s.createOrder("j2", pickup(mia, "TEN-001", 2), null);
        s.pay("j1", "TX-1", ONE_MINUTE);
        s.pay("j2", "TX-2", ONE_MINUTE);
        s.getCustomer(mia);                                         // 3.20 + 10.00
        s.markAsRead(mia, 0);
        s.markAsRead(mia, 0);                                       // reading again changes nothing
        s.getNotifications(mia);
        s.end();
    }

    void theTransLogWhenCreatingAnOrder() {
        String nina = cpf(16), otto = cpf(17);
        Purchase express = new Purchase(new Customer(nina, nina + "@exemplo.com"), List.of(new Item("CAM-001", 2), new Item("TEN-001", 1)),
                "01310100", "NORTH", "EXPRESS", null, "CREDIT_CARD", 6, "SILVER");
        s.begin("lifecycle", "the TransLog when creating an order");
        s.createOrder("k1", express, QUOTE);                        // the summary with the TransLog quote, saved in the order
        s.getOrder("k1");
        Purchase down = new Purchase(new Customer(otto, otto + "@exemplo.com"), List.of(new Item("CAM-001", 1)),
                "01310100", "SOUTH", "ECONOMY", null, "PIX", null, "BRONZE");
        s.createOrder("k2", down, new TransLog.Unavailable());       // 503, and no order
        s.getCustomer(otto);
        s.end();
    }

    // ------------------------------------------------------------------ small pieces

    /** A distinct CPF for each customer of the scenarios. */
    static String cpf(int n) {
        return String.format("2%010d", n);
    }

    /** A store pickup paid with Pix, for a SILVER customer (2% credit): no TransLog involved. */
    static Purchase pickup(String cpf, Object... skuAndQuantity) {
        return new Purchase(new Customer(cpf, cpf + "@exemplo.com"), Lockstep.items(skuAndQuantity), "01310100", "SOUTHEAST",
                "STORE_PICKUP", null, "PIX", null, "SILVER");
    }
}
