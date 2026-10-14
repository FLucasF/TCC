package com.loja.pedidos.controller;

import com.loja.pedidos.dto.CheckoutRequestDTO;
import com.loja.pedidos.dto.ResumoDTO;
import com.loja.pedidos.dto.ItemDTO;
import com.loja.pedidos.repository.ProdutoRepository;
import com.loja.pedidos.service.ResumoPedidoService;
import com.loja.pedidos.service.ValidacaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {
    @Autowired
    private ResumoPedidoService resumoPedidoService;

    @Autowired
    private ValidacaoService validacaoService;

    @Autowired
    private ProdutoRepository produtoRepository;

    @PostMapping("/resumo")
    public ResponseEntity<ResumoDTO> calcularResumo(@RequestBody CheckoutRequestDTO req) {
        validacaoService.validarCheckout(req);
        BigDecimal pesoTotal = calcularPesoTotal(req.getItens());
        validacaoService.validarDisponibilidadeEntrega(req, pesoTotal);
        validacaoService.validarCupomAplicavel(req.getCupom(), req);
        ResumoDTO resumo = resumoPedidoService.calcularResumo(req);
        validacaoService.validarFormaPagamentoDisponivel(req.getFormaPagamento(), resumo.getTotalFinal());
        return ResponseEntity.ok(resumo);
    }

    private BigDecimal calcularPesoTotal(List<ItemDTO> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemDTO item : itens) {
            var produto = produtoRepository.findById(item.getSku());
            if (produto.isPresent()) {
                BigDecimal pesoUnitario = produto.get().getPesoKg();
                BigDecimal quantidade = new BigDecimal(item.getQuantidade());
                peso = peso.add(pesoUnitario.multiply(quantidade));
            }
        }
        return peso;
    }
}
