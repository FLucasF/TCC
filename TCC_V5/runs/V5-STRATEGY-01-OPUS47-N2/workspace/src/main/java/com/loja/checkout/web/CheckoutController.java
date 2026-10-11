package com.loja.checkout.web;

import com.loja.checkout.dominio.ErroPedido;
import com.loja.checkout.dominio.ResumoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

    private final ResumoService service;

    public CheckoutController(ResumoService service) {
        this.service = service;
    }

    @PostMapping("/checkout/resumo")
    public ResumoResponse resumo(@RequestBody PedidoRequest req) {
        return service.calcular(req);
    }

    @ExceptionHandler(ErroPedido.class)
    public ResponseEntity<ErroResponse> tratar(ErroPedido e) {
        return ResponseEntity.badRequest().body(new ErroResponse(e.codigo()));
    }
}
