package com.loja.checkout.service;

import com.loja.checkout.domain.*;
import com.loja.checkout.domain.clube.Bronze;
import com.loja.checkout.domain.clube.NivelClube;
import com.loja.checkout.domain.clube.Ouro;
import com.loja.checkout.domain.clube.Prata;
import com.loja.checkout.domain.cupom.*;
import com.loja.checkout.domain.entrega.Economica;
import com.loja.checkout.domain.entrega.Expressa;
import com.loja.checkout.domain.entrega.ModalidadeEntrega;
import com.loja.checkout.domain.entrega.Motoboy;
import com.loja.checkout.domain.entrega.RetiradaLoja;
import com.loja.checkout.domain.pagamento.Boleto;
import com.loja.checkout.domain.pagamento.Cartao;
import com.loja.checkout.domain.pagamento.FormaPagamento;
import com.loja.checkout.domain.pagamento.Pix;
import com.loja.checkout.util.Dinheiro;
import com.loja.checkout.validacao.ValidadorPedido;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Service
public class CalculadoraResumo {

    private final ValidadorPedido validador = new ValidadorPedido();

    private final Map<String, ModalidadeEntrega> modalidades = Map.of(
        "ECONOMICA", new Economica(),
        "EXPRESSA", new Expressa(),
        "RETIRADA_LOJA", new RetiradaLoja(),
        "MOTOBOY", new Motoboy()
    );

    private final Map<String, NivelClube> niveis = Map.of(
        "BRONZE", new Bronze(),
        "PRATA", new Prata(),
        "OURO", new Ouro()
    );

    private final Map<String, Cupom> cupons = Map.of(
        "BEMVINDO10", new BemVindo10(),
        "MENOS50", new Menos50(),
        "FRETEGRATIS", new FreteGratis(),
        "LEVE3PAGUE2", new Leve3Pague2()
    );

    private final Map<String, FormaPagamento> formasPagamento = Map.of(
        "PIX", new Pix(),
        "BOLETO", new Boleto(),
        "CARTAO", new Cartao()
    );

    public Object calcular(PedidoRequest request) {
        BigDecimal subtotalProdutos = calcularSubtotalProdutos(request);
        double pesoTotal = calcularPesoTotal(request);

        ModalidadeEntrega modalidade = modalidades.get(request.modalidadeEntrega());
        NivelClube nivel = niveis.get(request.nivelClube());
        Regiao regiao = parseRegiao(request.regiao());
        FormaPagamento formaPagamento = formasPagamento.get(request.formaPagamento());

        BigDecimal frete = calcularFrete(modalidade, nivel, pesoTotal);
        BigDecimal seguro = calcularSeguro(subtotalProdutos, regiao);

        Cupom cupom = null;
        if (request.cupom() != null && !request.cupom().isBlank()) {
            cupom = cupons.get(request.cupom());
        }

        ContextoCupom contextoCupom = new ContextoCupom(subtotalProdutos, request.itens(), frete);
        BigDecimal descontoCupom = aplicarCupom(cupom, contextoCupom);
        BigDecimal totalPedido = subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro);

        Optional<CodigoErro> erro = validador.validar(
            request, modalidade, nivel, regiao, cupom, formaPagamento,
            pesoTotal, subtotalProdutos, totalPedido, () -> contextoCupom
        );

        if (erro.isPresent()) {
            return new ErroResponse(erro.get().name());
        }

        int parcelas = request.parcelas() != null ? request.parcelas() : 1;
        BigDecimal ajustePagamento = formaPagamento.calcularAjuste(totalPedido, parcelas);
        BigDecimal totalFinal = Dinheiro.arredondar(totalPedido.add(ajustePagamento));

        BigDecimal valorParcela = formaPagamento.calcularValorParcela(totalPedido, totalFinal, parcelas);

        BigDecimal credito = nivel.calcularCredito(subtotalProdutos);
        boolean brinde = nivel.temBrinde(subtotalProdutos);

        int prazo = modalidade.obterPrazo();

        return new ResumoResponse(
            subtotalProdutos,
            descontoCupom,
            frete,
            prazo,
            seguro,
            ajustePagamento,
            totalFinal,
            parcelas,
            valorParcela,
            credito,
            brinde
        );
    }

    private BigDecimal calcularSubtotalProdutos(PedidoRequest request) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (var item : request.itens()) {
            BigDecimal precoItem = BigDecimal.valueOf(item.precoUnitario())
                .multiply(BigDecimal.valueOf(item.quantidade()));
            subtotal = subtotal.add(precoItem);
        }
        return Dinheiro.arredondar(subtotal);
    }

    private double calcularPesoTotal(PedidoRequest request) {
        double peso = 0.0;
        for (var item : request.itens()) {
            peso += item.pesoKg() * item.quantidade();
        }
        return peso;
    }

    private BigDecimal calcularFrete(ModalidadeEntrega modalidade, NivelClube nivel, double pesoTotal) {
        if (modalidade == null) {
            return Dinheiro.de(0.00);
        }

        if (nivel != null && nivel.temFreteGratis()) {
            return Dinheiro.de(0.00);
        }

        return modalidade.calcularFrete(pesoTotal);
    }

    private BigDecimal calcularSeguro(BigDecimal subtotalProdutos, Regiao regiao) {
        if (regiao == null) {
            return Dinheiro.de(0.00);
        }
        return Dinheiro.percentual(subtotalProdutos, regiao.getPorcentagemSeguro());
    }

    private BigDecimal aplicarCupom(Cupom cupom, ContextoCupom contexto) {
        if (cupom == null) {
            return Dinheiro.de(0.00);
        }

        return cupom.calcularDesconto(contexto);
    }

    private Regiao parseRegiao(String regiao) {
        if (regiao == null) return null;
        try {
            return Regiao.valueOf(regiao);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
