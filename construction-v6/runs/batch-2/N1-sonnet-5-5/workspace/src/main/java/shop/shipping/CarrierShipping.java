package shop.shipping;

/** Delivery priced by a carrier; regions far from the distribution center add dispatch days. */
public final class CarrierShipping implements ShippingOption {
    private final String code;
    private final String service;
    private final CarrierGateway gateway;

    public CarrierShipping(String code, String service, CarrierGateway gateway) {
        this.code = code;
        this.service = service;
        this.gateway = gateway;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public ShippingQuote quote(ShippingContext context) {
        CarrierQuote quote = gateway.quote(context.zipCode(), context.weightGrams(), service);
        int days = (quote.etaHours() + 23) / 24 + context.region().extraDispatchDays();
        return new ShippingQuote(quote.price(), days);
    }
}
