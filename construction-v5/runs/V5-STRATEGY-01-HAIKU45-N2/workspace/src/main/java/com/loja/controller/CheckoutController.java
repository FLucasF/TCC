package com.loja.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.loja.dto.CheckoutRequest;
import com.loja.dto.CheckoutResponse;
import com.loja.dto.ErroResponse;
import com.loja.service.ResumoCheckoutService;

@RestController
public class CheckoutController {
  private final ResumoCheckoutService service = new ResumoCheckoutService();

  @PostMapping("/checkout/resumo")
  public ResponseEntity<?> resumoCheckout(@RequestBody CheckoutRequest request) {
    try {
      CheckoutResponse response = service.calcularResumo(request);
      return ResponseEntity.ok(response);
    } catch (IllegalArgumentException e) {
      String codigoErro = e.getMessage();
      return ResponseEntity.badRequest().body(new ErroResponse(codigoErro));
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(new ErroResponse("ERRO_DESCONHECIDO"));
    }
  }
}
