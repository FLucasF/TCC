package com.loja.checkout.service;

import com.loja.checkout.clube.*;
import com.loja.checkout.cupom.*;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.entrega.*;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.pagamento.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
public class ResumoService {

    private final Map<String, ModalidadeEntrega> modalidades = Map.of(
            "ECONOMICA", new Economica(),
            "EXPRESSA", new Expressa(),
            "RETIRADA_LOJA", new RetiradaLoja(),
            "MOTOBOY", new Motoboy()
    );

    private final Map<String, Cupom> cupons = Map.of(
            "BEMVINDO10", new BemVindo10(),
            "MENOS50", new Menos50(),
            "FRETEGRATIS", new FreteGratis(),
            "LEVE3PAGUE2", new Leve3Pague2()
    );

    private final Map<String, NivelClube> niveis = Map.of(
            "BRONZE", new Bronze(),
            "PRATA", new Prata(),
            "OURO", new Ouro()
    );

    private final Map<String, FormaPagamento> formasPagamento = Map.of(
            "PIX", new Pix(),
            "CARTAO", new Cartao(),
            "BOLETO", new Boleto()
    );

    private final Map<String, BigDecimal> taxasSeguro = Map.of(
            "SUDESTE", new BigDecimal("0.01"),
            "SUL", new BigDecimal("0.01"),
            "CENTRO_OESTE", new BigDecimal("0.015"),
            "NORTE", new BigDecimal("0.025"),
            "NORDESTE", new BigDecimal("0.02")
    );

    public ResumoResponse calcular(ResumoRequest req) {
        validarItens(req.itens());

        NivelClube nivel = buscar(niveis, req.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        BigDecimal taxaSeguro = buscarTaxa(req.regiao());
        ModalidadeEntrega entrega = buscar(modalidades, req.modalidadeEntrega(), "MODALIDADE_INVALIDA");

        BigDecimal pesoTotal = calcularPeso(req.itens());
        if (!entrega.disponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        Cupom cupom = null;
        if (req.cupom() != null && !req.cupom().isBlank()) {
            cupom = buscar(cupons, req.cupom(), "CUPOM_INVALIDO");
        }

        FormaPagamento pagamento = buscar(formasPagamento, req.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = req.parcelas() != null ? req.parcelas() : 1;
        if (!pagamento.parcelamentoValido(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal subtotal = calcularSubtotal(req.itens());

        if (cupom != null && !cupom.aplicavel(subtotal, req.itens())) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }

        BigDecimal frete = entrega.calcularFrete(pesoTotal);
        if (nivel.freteGratis()) {
            frete = BigDecimal.ZERO.setScale(2);
        }

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (cupom != null) {
            descontoCupom = cupom.calcularDesconto(subtotal, req.itens(), frete);
        }

        BigDecimal seguro = subtotal.multiply(taxaSeguro).setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro)
                .setScale(2, RoundingMode.HALF_EVEN);

        if (!pagamento.disponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento resultado = pagamento.calcular(totalPedido, parcelas);

        BigDecimal credito = nivel.calcularCredito(subtotal);
        boolean brinde = nivel.brinde(subtotal);

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                entrega.prazoDias(),
                seguro,
                resultado.ajuste(),
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela(),
                credito,
                brinde
        );
    }

    private void validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (ItemRequest item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().compareTo(BigDecimal.ZERO) <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private BigDecimal calcularSubtotal(List<ItemRequest> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calcularPeso(List<ItemRequest> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemRequest item : itens) {
            peso = peso.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return peso;
    }

    private BigDecimal buscarTaxa(String regiao) {
        if (regiao == null || !taxasSeguro.containsKey(regiao)) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }
        return taxasSeguro.get(regiao);
    }

    private <T> T buscar(Map<String, T> mapa, String chave, String codigoErro) {
        if (chave == null || !mapa.containsKey(chave)) {
            throw new CheckoutException(codigoErro);
        }
        return mapa.get(chave);
    }
}
