package br.com.loja.pedidos.shipping;

import java.math.BigDecimal;
import java.math.RoundingMode;

import br.com.loja.pedidos.common.DomainException;
import br.com.loja.pedidos.common.Money;

/** Entrega cotada na TransLog; cada serviço dela (STD, EXP) vira uma opção nossa. */
public class TransLogShipping implements ShippingOption {

    private final String code;
    private final String service;
    private final TransLogClient client;

    public TransLogShipping(String code, String service, TransLogClient client) {
        this.code = code;
        this.service = service;
        this.client = client;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public ShippingQuote quote(ShippingRequest request) {
        long grams = request.weightKg().movePointRight(3).setScale(0, RoundingMode.CEILING).longValueExact();
        TransLogClient.TransLogQuote quote = client.quote(request.zipCode(), grams, service)
                .orElseThrow(() -> DomainException.unprocessable("SHIPPING_METHOD_UNAVAILABLE"));
        BigDecimal cost = Money.round(BigDecimal.valueOf(quote.priceCents(), 2));
        int days = Math.ceilDiv(quote.etaHours(), 24) + request.region().carrierExtraDays();
        return new ShippingQuote(cost, days);
    }
}
