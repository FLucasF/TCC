package br.com.loja.payment;

import br.com.loja.common.ApiException;
import br.com.loja.common.ErrorCode;

/** Base das formas à vista (só 1 parcela). */
abstract class CashPayment implements PaymentOption {

    @Override
    public void validateInstallments(int installments) {
        if (installments != 1) {
            throw new ApiException(ErrorCode.INVALID_INSTALLMENTS);
        }
    }
}
