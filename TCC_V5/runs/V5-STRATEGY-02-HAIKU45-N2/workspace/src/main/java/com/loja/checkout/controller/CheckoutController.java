package com.loja.checkout.controller;

import com.loja.checkout.model.request.CheckoutRequest;
import com.loja.checkout.model.response.CheckoutResponse;
import com.loja.checkout.model.response.ErrorResponse;
import com.loja.checkout.service.CheckoutService;
import com.loja.checkout.service.CheckoutService.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

  @Autowired
  private CheckoutService checkoutService;

  @PostMapping("/checkout/resumo")
  public ResponseEntity<?> resumo(@RequestBody CheckoutRequest request) {
    try {
      CheckoutResponse response = checkoutService.calcular(request);
      return ResponseEntity.ok(response);
    } catch (ValidationException e) {
      return ResponseEntity.badRequest().body(new ErrorResponse(e.getCodigo()));
    }
  }
}
