package com.loja.controller;

import com.loja.dto.CheckoutRequest;
import com.loja.dto.CheckoutResponse;
import com.loja.dto.ErrorResponse;
import com.loja.servico.ErroCheckout;
import com.loja.servico.ServicoCheckout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {
    @Autowired
    private ServicoCheckout servicoCheckout;

    @PostMapping("/checkout/resumo")
    public ResponseEntity<CheckoutResponse> calcularResumo(@RequestBody CheckoutRequest request) {
        CheckoutResponse resposta = servicoCheckout.calcularResumo(request);
        return ResponseEntity.ok(resposta);
    }

    @ExceptionHandler(ErroCheckout.class)
    public ResponseEntity<ErrorResponse> handleErroCheckout(ErroCheckout e) {
        ErrorResponse resposta = new ErrorResponse(e.getCodigo());
        return ResponseEntity.badRequest().body(resposta);
    }
}
