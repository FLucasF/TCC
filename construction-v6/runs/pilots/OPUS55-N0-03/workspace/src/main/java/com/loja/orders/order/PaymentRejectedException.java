package com.loja.orders.order;

import org.springframework.http.HttpStatus;

import com.loja.orders.common.ApiException;
import com.loja.orders.common.ErrorCode;

/** Pagamento recusado que cancela o pedido: o cancelamento fica gravado mesmo com a recusa. */
public class PaymentRejectedException extends ApiException {

    public PaymentRejectedException(ErrorCode code) {
        super(HttpStatus.CONFLICT, code);
    }
}
