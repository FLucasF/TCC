package com.loja.checkout.api;

import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.ErroCheckout;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final ResumoCompraService service;

    public CheckoutController(ResumoCompraService service) {
        this.service = service;
    }

    @PostMapping("/resumo")
    public ResumoResponse resumo(@RequestBody(required = false) ResumoRequest request) {
        return service.calcular(request);
    }

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErroResponse> erroDeNegocio(CheckoutException excecao) {
        return ResponseEntity.badRequest().body(new ErroResponse(excecao.getErro().name()));
    }

    /** Corpo ausente ou fora do formato combinado: tratado como pedido invalido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoInvalido(HttpMessageNotReadableException excecao) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErroResponse(ErroCheckout.PEDIDO_INVALIDO.name()));
    }
}
