package com.loja.checkout.web;

import com.loja.checkout.domain.CheckoutService;
import com.loja.checkout.domain.erro.CheckoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/checkout/resumo")
    public ResumoResponse resumir(@RequestBody ResumoRequest request) {
        return checkoutService.resumir(request);
    }

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErroResponse> tratarRecusa(CheckoutException ex) {
        return ResponseEntity.unprocessableEntity().body(new ErroResponse(ex.getErro().name()));
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarCorpoInvalido(
            org.springframework.http.converter.HttpMessageNotReadableException ex) {
        // Corpo ausente ou JSON malformado: nao da nem para comecar o calculo.
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErroResponse("PEDIDO_INVALIDO"));
    }
}
