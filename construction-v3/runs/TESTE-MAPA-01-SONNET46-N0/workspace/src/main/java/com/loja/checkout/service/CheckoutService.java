package com.loja.checkout.service;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemCarrinho;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.service.clube.EstrategiaClube;
import com.loja.checkout.service.cupom.EstrategiaCupom;
import com.loja.checkout.service.entrega.EstrategiaEntrega;
import com.loja.checkout.service.pagamento.EstrategiaPagamento;
import com.loja.checkout.service.pagamento.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CheckoutService {

    private final Map<String, EstrategiaEntrega> entregaMap;
    private final Map<String, EstrategiaCupom> cupomMap;
    private final Map<String, EstrategiaClube> clubeMap;
    private final Map<String, EstrategiaPagamento> pagamentoMap;

    // Taxas por região
    private static final Map<String, BigDecimal> TAXAS_REGIAO = new HashMap<>();

    static {
        TAXAS_REGIAO.put("SUDESTE", new BigDecimal("0.01"));
        TAXAS_REGIAO.put("SUL", new BigDecimal("0.01"));
        TAXAS_REGIAO.put("CENTRO_OESTE", new BigDecimal("0.015"));
        TAXAS_REGIAO.put("NORTE", new BigDecimal("0.025"));
        TAXAS_REGIAO.put("NORDESTE", new BigDecimal("0.02"));
    }

    public CheckoutService(
            List<EstrategiaEntrega> estrategiasEntrega,
            List<EstrategiaCupom> estrategiasCupom,
            List<EstrategiaClube> estrategiasClube,
            List<EstrategiaPagamento> estrategiasPagamento) {

        entregaMap = new HashMap<>();
        for (EstrategiaEntrega e : estrategiasEntrega) {
            entregaMap.put(e.getCodigo(), e);
        }

        cupomMap = new HashMap<>();
        for (EstrategiaCupom c : estrategiasCupom) {
            cupomMap.put(c.getCodigo(), c);
        }

        clubeMap = new HashMap<>();
        for (EstrategiaClube c : estrategiasClube) {
            clubeMap.put(c.getCodigo(), c);
        }

        pagamentoMap = new HashMap<>();
        for (EstrategiaPagamento p : estrategiasPagamento) {
            pagamentoMap.put(p.getCodigo(), p);
        }
    }

    public CheckoutResponse calcular(CheckoutRequest request) {
        // 1. Validar itens
        List<ItemCarrinho> itens = request.getItens();
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (ItemCarrinho item : itens) {
            if (item.getPrecoUnitario() == null || item.getPrecoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.getQuantidade() == null || item.getQuantidade() <= 0
                    || item.getPesoKg() == null || item.getPesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }

        // 2. Validar nível clube
        String nivelClubeStr = request.getNivelClube();
        if (nivelClubeStr == null || !clubeMap.containsKey(nivelClubeStr)) {
            throw new CheckoutException("NIVEL_CLUBE_INVALIDO");
        }
        EstrategiaClube clube = clubeMap.get(nivelClubeStr);

        // 3. Validar região
        String regiaoStr = request.getRegiao();
        if (regiaoStr == null || !TAXAS_REGIAO.containsKey(regiaoStr)) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
        BigDecimal taxaRegiao = TAXAS_REGIAO.get(regiaoStr);

        // 4. Validar modalidade de entrega (reconhecida)
        String modalidadeStr = request.getModalidadeEntrega();
        if (modalidadeStr == null || !entregaMap.containsKey(modalidadeStr)) {
            throw new CheckoutException("MODALIDADE_INVALIDA");
        }
        EstrategiaEntrega entrega = entregaMap.get(modalidadeStr);

        // Calcular peso total
        BigDecimal pesoTotal = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            pesoTotal = pesoTotal.add(item.getPesoKg().multiply(BigDecimal.valueOf(item.getQuantidade())));
        }

        // 5. Validar se a modalidade atende o pedido
        if (!entrega.atendePedido(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        // 6. Validar cupom (se informado e não reconhecido)
        String cupomStr = request.getCupom();
        EstrategiaCupom cupom = null;
        if (cupomStr != null && !cupomStr.isBlank()) {
            if (!cupomMap.containsKey(cupomStr)) {
                throw new CheckoutException("CUPOM_INVALIDO");
            }
            cupom = cupomMap.get(cupomStr);
        }

        // 8. Validar forma de pagamento (antes de calcular, mas precisamos verificar validações 7-10)
        String formaPagamentoStr = request.getFormaPagamento();
        if (formaPagamentoStr == null || !pagamentoMap.containsKey(formaPagamentoStr)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INVALIDA");
        }
        EstrategiaPagamento pagamento = pagamentoMap.get(formaPagamentoStr);

        // Calcular subtotal
        BigDecimal subtotalProdutos = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            subtotalProdutos = subtotalProdutos.add(
                    item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade()))
            );
        }
        subtotalProdutos = subtotalProdutos.setScale(2, RoundingMode.HALF_EVEN);

        // Calcular frete (levando em conta clube OURO)
        BigDecimal freteCalculado = entrega.calcularFrete(pesoTotal);
        BigDecimal frete;
        if (clube.freteGratis()) {
            frete = BigDecimal.ZERO.setScale(2);
        } else {
            frete = freteCalculado;
        }

        // 7. Validar se cupom é aplicável
        if (cupom != null && !cupom.aplicavel(itens, subtotalProdutos, frete)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }

        // Calcular desconto do cupom
        BigDecimal descontoCupom;
        if (cupom != null) {
            descontoCupom = cupom.calcularDesconto(itens, subtotalProdutos, frete);
        } else {
            descontoCupom = BigDecimal.ZERO.setScale(2);
        }

        // Calcular seguro
        BigDecimal seguro = subtotalProdutos.multiply(taxaRegiao).setScale(2, RoundingMode.HALF_EVEN);

        // totalAntesPagamento = subtotal - desconto + frete + seguro
        BigDecimal totalAntesPagamento = subtotalProdutos
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro)
                .setScale(2, RoundingMode.HALF_EVEN);

        // 9. Validar parcelas
        int parcelas = request.getParcelas() != null ? request.getParcelas() : 1;
        if (!pagamento.aceitaParcelamento(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        // 10. Validar se forma de pagamento atende o pedido
        if (!pagamento.atendePedido(totalAntesPagamento)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        // Calcular pagamento
        ResultadoPagamento resultado = pagamento.calcular(totalAntesPagamento, parcelas);

        BigDecimal totalFinal = resultado.totalFinal();
        BigDecimal ajustePagamento = totalFinal.subtract(totalAntesPagamento).setScale(2, RoundingMode.HALF_EVEN);

        // Crédito próxima compra
        BigDecimal creditoProximaCompra = clube.calcularCredito(subtotalProdutos);

        // Brinde
        boolean brinde = clube.temBrinde(subtotalProdutos);

        // Montar response
        CheckoutResponse response = new CheckoutResponse();
        response.setSubtotalProdutos(subtotalProdutos);
        response.setDescontoCupom(descontoCupom);
        response.setFrete(frete);
        response.setPrazoEntregaDias(entrega.prazoEmDias());
        response.setSeguro(seguro);
        response.setAjustePagamento(ajustePagamento);
        response.setTotalFinal(totalFinal);
        response.setParcelas(resultado.parcelas());
        response.setValorParcela(resultado.valorParcela());
        response.setCreditoProximaCompra(creditoProximaCompra);
        response.setBrinde(brinde);

        return response;
    }
}
