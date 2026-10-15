package com.loja.checkout.controller;

import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.service.ResumoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {

  @Autowired
  private ResumoService resumoService;

  @PostMapping(value = "/checkout/resumo", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> calcularResumo(@RequestBody ResumoRequest request) {
    Object resultado = resumoService.calcularResumo(request);
    return ResponseEntity.ok(resultado);
  }
}
