package com.loja.checkout.service;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.model.CheckoutErro;
import com.loja.checkout.model.CheckoutException;
import com.loja.checkout.model.Regiao;
import com.loja.checkout.service.clube.BeneficioClube;
import com.loja.checkout.service.cupom.Cupom;
import com.loja.checkout.service.cupom.ContextoCupom;
import com.loja.checkout.service.cupom.ResultadoCupom;
import com.loja.checkout.service.entrega.ModalidadeEntrega;
import com.loja.checkout.service.entrega.ResultadoFrete;
import com.loja.checkout.service.pagamento.FormaPagamento;
import com.loja.checkout.service.pagamento.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CheckoutService {

    private final Map<String, ModalidadeEntrega> modalidades;
    private final Map<String, Cupom> cupons;
    private final Map<String, BeneficioClube> niveis;
    private final Map<String, FormaPagamento> formasPagamento;

    public CheckoutService(
            List<ModalidadeEntrega> modalidades,
            List<Cupom> cupons,
            List<BeneficioClube> niveis,
            List<FormaPagamento> formasPagamento) {
        this.modalidades = modalidades.stream().collect(Collectors.toMap(ModalidadeEntrega::getCodigo, Function.identity()));
        this.cupons = cupons.stream().collect(Collectors.toMap(Cupom::getCodigo, Function.identity()));
        this.niveis = niveis.stream().collect(Collectors.toMap(BeneficioClube::getNivel, Function.identity()));
        this.formasPagamento = formasPagamento.stream().collect(Collectors.toMap(FormaPagamento::getCodigo, Function.identity()));
    }

    public CheckoutResponse calcular(CheckoutRequest req) {
        // 1. Validar pedido
        validarPedido(req);

        // 2. Validar nível do clube
        BeneficioClube clube = resolverClube(req.nivelClube());

        // 3. Validar região
        Regiao regiao = resolverRegiao(req.regiao());

        // 4. Validar modalidade de entrega
        ModalidadeEntrega modalidade = resolverModalidade(req.modalidadeEntrega());

        // Calcular peso total
        double pesoTotal = req.itens().stream()
                .mapToDouble(i -> i.pesoKg() * i.quantidade())
                .sum();

        // Verificar se modalidade aceita o pedido
        if (!modalidade.aceita(pesoTotal)) {
            throw new CheckoutException(CheckoutErro.MODALIDADE_INDISPONIVEL);
        }

        // 5. Calcular subtotal dos produtos
        double subtotalProdutos = Arredondamento.arredondar(
                req.itens().stream().mapToDouble(i -> i.precoUnitario() * i.quantidade()).sum()
        );

        // 6. Calcular frete (considerando benefício OURO)
        ResultadoFrete resultadoFrete = modalidade.calcular(pesoTotal);
        double frete = clube.isFreteGratis() ? 0.00 : Arredondamento.arredondar(resultadoFrete.valor());
        int prazoEntregaDias = resultadoFrete.prazoEntregaDias();

        // 7. Validar e aplicar cupom
        double descontoCupom = 0.00;
        boolean freteGratisCupom = false;
        if (req.cupom() != null && !req.cupom().isBlank()) {
            Cupom cupom = resolverCupom(req.cupom());
            ContextoCupom ctx = new ContextoCupom(req.itens(), subtotalProdutos, frete);
            if (!cupom.aplicavel(ctx)) {
                throw new CheckoutException(CheckoutErro.CUPOM_NAO_APLICAVEL);
            }
            ResultadoCupom resultadoCupom = cupom.calcular(ctx);
            descontoCupom = resultadoCupom.desconto();
            freteGratisCupom = resultadoCupom.isFreteGratis();
        }

        // 8. Calcular seguro (sobre subtotalProdutos sem desconto e sem frete)
        double seguro = Arredondamento.arredondar(subtotalProdutos * regiao.taxaSeguro);

        // 9. Calcular total do pedido antes do pagamento
        double freteEfetivo = freteGratisCupom ? 0.00 : frete;
        double totalPedido = Arredondamento.arredondar(subtotalProdutos - descontoCupom + freteEfetivo + seguro);

        // 10. Validar forma de pagamento
        FormaPagamento formaPagamento = resolverFormaPagamento(req.formaPagamento());

        int parcelas = req.parcelas() != null ? req.parcelas() : 1;
        if (!formaPagamento.parcelasValidas(parcelas)) {
            throw new CheckoutException(CheckoutErro.PARCELAMENTO_INVALIDO);
        }
        if (!formaPagamento.aceitaTotal(totalPedido)) {
            throw new CheckoutException(CheckoutErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        // 11. Calcular ajuste do pagamento
        ResultadoPagamento resultadoPagamento = formaPagamento.calcular(totalPedido, parcelas);

        // 12. Crédito para próxima compra e brinde
        double credito = clube.calcularCredito(subtotalProdutos);
        boolean brinde = clube.temBrinde(subtotalProdutos);

        // Quando cupom é FRETEGRATIS, desconto = frete (já calculado), mas frete aparece normalmente no resumo
        double freteExibido = clube.isFreteGratis() ? 0.00 : frete;

        return new CheckoutResponse(
                subtotalProdutos,
                descontoCupom,
                freteExibido,
                prazoEntregaDias,
                seguro,
                resultadoPagamento.ajuste(),
                resultadoPagamento.totalFinal(),
                resultadoPagamento.parcelas(),
                resultadoPagamento.valorParcela(),
                credito,
                brinde
        );
    }

    private void validarPedido(CheckoutRequest req) {
        if (req.itens() == null || req.itens().isEmpty()) {
            throw new CheckoutException(CheckoutErro.PEDIDO_INVALIDO);
        }
        for (ItemRequest item : req.itens()) {
            if (item.precoUnitario() == null || item.precoUnitario() <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg() <= 0) {
                throw new CheckoutException(CheckoutErro.PEDIDO_INVALIDO);
            }
        }
    }

    private BeneficioClube resolverClube(String nivelClube) {
        if (nivelClube == null || nivelClube.isBlank()) {
            throw new CheckoutException(CheckoutErro.NIVEL_CLUBE_INVALIDO);
        }
        BeneficioClube clube = niveis.get(nivelClube);
        if (clube == null) {
            throw new CheckoutException(CheckoutErro.NIVEL_CLUBE_INVALIDO);
        }
        return clube;
    }

    private Regiao resolverRegiao(String regiao) {
        if (regiao == null || regiao.isBlank()) {
            throw new CheckoutException(CheckoutErro.REGIAO_INVALIDA);
        }
        try {
            return Regiao.valueOf(regiao);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(CheckoutErro.REGIAO_INVALIDA);
        }
    }

    private ModalidadeEntrega resolverModalidade(String modalidade) {
        if (modalidade == null || modalidade.isBlank()) {
            throw new CheckoutException(CheckoutErro.MODALIDADE_INVALIDA);
        }
        ModalidadeEntrega m = modalidades.get(modalidade);
        if (m == null) {
            throw new CheckoutException(CheckoutErro.MODALIDADE_INVALIDA);
        }
        return m;
    }

    private Cupom resolverCupom(String codigo) {
        Cupom c = cupons.get(codigo);
        if (c == null) {
            throw new CheckoutException(CheckoutErro.CUPOM_INVALIDO);
        }
        return c;
    }

    private FormaPagamento resolverFormaPagamento(String forma) {
        if (forma == null || forma.isBlank()) {
            throw new CheckoutException(CheckoutErro.FORMA_PAGAMENTO_INVALIDA);
        }
        FormaPagamento fp = formasPagamento.get(forma);
        if (fp == null) {
            throw new CheckoutException(CheckoutErro.FORMA_PAGAMENTO_INVALIDA);
        }
        return fp;
    }
}
