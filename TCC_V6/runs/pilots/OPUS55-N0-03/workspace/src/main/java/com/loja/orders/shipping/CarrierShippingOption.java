package com.loja.orders.shipping;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.http.HttpStatus;

import com.loja.orders.common.ApiException;
import com.loja.orders.common.ErrorCode;
import com.loja.orders.shipping.carrier.Carrier;
import com.loja.orders.shipping.carrier.CarrierQuote;
import com.loja.orders.shipping.carrier.CarrierService;
import com.loja.orders.shipping.carrier.CarrierUnavailableException;
import com.loja.orders.shipping.carrier.ZipNotServedException;

/** Entrega feita por transportadora: o frete e o prazo vêm dela, mais o tempo de despacho da região. */
public abstract class CarrierShippingOption implements ShippingOption {

    private static final BigDecimal GRAMS_PER_KG = new BigDecimal("1000");
    private static final int HOURS_PER_DAY = 24;

    private final Carrier carrier;
    private final CarrierService service;

    protected CarrierShippingOption(Carrier carrier, CarrierService service) {
        this.carrier = carrier;
        this.service = service;
    }

    @Override
    public ShippingQuote quote(ShippingRequest request) {
        int weightGrams = request.weightKg().multiply(GRAMS_PER_KG).setScale(0, RoundingMode.CEILING).intValueExact();
        CarrierQuote quote;
        try {
            quote = carrier.quote(request.zipCode(), weightGrams, service);
        } catch (ZipNotServedException e) {
            throw ApiException.unprocessable(ErrorCode.SHIPPING_METHOD_UNAVAILABLE);
        } catch (CarrierUnavailableException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.CARRIER_UNAVAILABLE);
        }
        int days = Math.ceilDiv(quote.etaHours(), HOURS_PER_DAY) + request.region().carrierDispatchExtraDays();
        return new ShippingQuote(quote.price(), days);
    }
}
