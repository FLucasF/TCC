package com.loja.checkout.controller;

import com.loja.checkout.domain.CheckoutException;
import com.loja.checkout.dto.ErroResponse;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.service.CheckoutService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/resumo")
    public ResumoResponse calcularResumo(@RequestBody PedidoRequest request) {
        return checkoutService.calcular(request);
    }

    @ExceptionHandler(CheckoutException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErroResponse handleCheckoutException(CheckoutException ex) {
        return new ErroResponse(ex.getCodigo());
    }
}
