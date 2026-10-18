package shop.shipping;

/** Access to a carrier's quotes. Swapping carriers means a new implementation of this interface. */
public interface CarrierGateway {
    /**
     * @param service carrier-specific service code
     * @throws shop.ApiException SHIPPING_METHOD_UNAVAILABLE when the zip is not served,
     *                           CARRIER_UNAVAILABLE when the carrier is down
     */
    CarrierQuote quote(String zipCode, int weightGrams, String service);
}
