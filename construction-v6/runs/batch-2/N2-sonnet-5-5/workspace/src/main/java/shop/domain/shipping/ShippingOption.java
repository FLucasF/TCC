package shop.domain.shipping;

/** Uma forma de entrega. Uma nova opção é uma nova implementação registrada em {@link ShippingOptions}. */
public interface ShippingOption {
    String code();

    /** Lança DomainException se a opção não atende o pedido. */
    ShippingQuote quote(ShippingRequest request);

    /** Se existe frete a cobrar (a retirada na loja não tem). */
    boolean chargesFreight();

    /** Se o benefício de frete grátis do clube vale nesta opção. */
    boolean clubWaivable();
}
