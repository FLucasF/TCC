package shop.domain.payment;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PaymentMethods {
    private final Map<String, PaymentMethod> byCode = new LinkedHashMap<>();

    public PaymentMethods(List<PaymentMethod> methods) {
        methods.forEach(m -> byCode.put(m.code(), m));
    }

    /** Formas de hoje; uma nova forma de pagamento entra nesta lista. */
    public static PaymentMethods standard() {
        return new PaymentMethods(List.of(new Pix(), new CreditCard(), new BankSlip()));
    }

    public Optional<PaymentMethod> find(String code) {
        return Optional.ofNullable(code).map(byCode::get);
    }
}
