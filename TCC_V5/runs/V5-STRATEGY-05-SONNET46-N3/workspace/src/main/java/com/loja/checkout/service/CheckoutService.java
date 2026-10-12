package com.loja.checkout.service;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.clube.NivelClubeRegistry;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.CupomRegistry;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.entrega.ModalidadeEntregaRegistry;
import com.loja.checkout.exception.CheckoutException;
import com.loja.checkout.exception.ErroCodigo;
import com.loja.checkout.model.ItemCarrinho;
import com.loja.checkout.model.PedidoRequest;
import com.loja.checkout.model.ResumoResponse;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.FormaPagamentoRegistry;
import com.loja.checkout.regiao.SeguroRegiao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CheckoutService {

    private final ModalidadeEntregaRegistry entregaRegistry;
    private final CupomRegistry cupomRegistry;
    private final NivelClubeRegistry clubeRegistry;
    private final FormaPagamentoRegistry pagamentoRegistry;

    public CheckoutService(ModalidadeEntregaRegistry entregaRegistry,
                           CupomRegistry cupomRegistry,
                           NivelClubeRegistry clubeRegistry,
                           FormaPagamentoRegistry pagamentoRegistry) {
        this.entregaRegistry = entregaRegistry;
        this.cupomRegistry = cupomRegistry;
        this.clubeRegistry = clubeRegistry;
        this.pagamentoRegistry = pagamentoRegistry;
    }

    public ResumoResponse calcular(PedidoRequest req) {
        // 1. Validate items
        validarItens(req.itens());

        // 2. Validate nivel clube
        NivelClube nivelClube = req.nivelClube() != null ? clubeRegistry.buscar(req.nivelClube()) : null;
        if (nivelClube == null) {
            throw new CheckoutException(ErroCodigo.NIVEL_CLUBE_INVALIDO);
        }

        // 3. Validate regiao
        SeguroRegiao regiao = resolveRegiao(req.regiao());

        // 4. Validate modalidade
        ModalidadeEntrega modalidade = req.modalidadeEntrega() != null ? entregaRegistry.buscar(req.modalidadeEntrega()) : null;
        if (modalidade == null) {
            throw new CheckoutException(ErroCodigo.MODALIDADE_INVALIDA);
        }

        // 5. Validate modalidade disponibilidade
        double pesoTotal = calcularPesoTotal(req.itens());
        modalidade.validarDisponibilidade(pesoTotal);

        // 6. Validate cupom exists (if provided)
        Cupom cupom = null;
        if (req.cupom() != null && !req.cupom().isBlank()) {
            cupom = cupomRegistry.buscar(req.cupom());
            if (cupom == null) {
                throw new CheckoutException(ErroCodigo.CUPOM_INVALIDO);
            }
        }

        // Calculate subtotal before cupom validation
        BigDecimal subtotalProdutos = calcularSubtotal(req.itens());

        // 7. Validate cupom aplicabilidade
        if (cupom != null) {
            cupom.validarAplicabilidade(subtotalProdutos);
        }

        // 8. Validate forma pagamento
        if (req.formaPagamento() == null || pagamentoRegistry.buscar(req.formaPagamento()) == null) {
            throw new CheckoutException(ErroCodigo.FORMA_PAGAMENTO_INVALIDA);
        }
        FormaPagamento formaPagamento = pagamentoRegistry.buscar(req.formaPagamento());

        // 9. Validate parcelas
        int parcelas = req.parcelas() != null ? req.parcelas() : 1;
        formaPagamento.validarParcelas(parcelas);

        // Calculate frete
        BigDecimal frete = modalidade.calcularFrete(pesoTotal);
        if (nivelClube.isFreteGratis()) {
            frete = BigDecimal.ZERO.setScale(2);
        }

        // Calculate cupom desconto
        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (cupom != null) {
            descontoCupom = cupom.calcularDesconto(subtotalProdutos, frete, req.itens());
        }

        // Calculate seguro (sobre subtotalProdutos, sem desconto e sem frete)
        BigDecimal seguro = regiao.calcularSeguro(subtotalProdutos);

        // Total do pedido
        BigDecimal totalPedido = subtotalProdutos
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro);

        // 10. Validate forma pagamento disponibilidade
        formaPagamento.validarDisponibilidade(totalPedido);

        // Calculate total final and parcela
        BigDecimal totalFinal = formaPagamento.calcularTotalFinal(totalPedido, parcelas);
        BigDecimal valorParcela = formaPagamento.calcularParcela(totalFinal, parcelas);
        BigDecimal ajustePagamento = totalFinal.subtract(totalPedido);

        // Club benefits
        BigDecimal credito = nivelClube.calcularCredito(subtotalProdutos);
        boolean brinde = nivelClube.ganharBrinde(subtotalProdutos);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.getPrazo(),
                seguro,
                ajustePagamento,
                totalFinal,
                parcelas,
                valorParcela,
                credito,
                brinde
        );
    }

    private void validarItens(List<ItemCarrinho> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException(ErroCodigo.PEDIDO_INVALIDO);
        }
        for (ItemCarrinho item : itens) {
            if (item.precoUnitario() == null
                    || item.precoUnitario().compareTo(BigDecimal.ZERO) <= 0
                    || item.quantidade() == null
                    || item.quantidade() <= 0
                    || item.pesoKg() == null
                    || item.pesoKg() <= 0) {
                throw new CheckoutException(ErroCodigo.PEDIDO_INVALIDO);
            }
        }
    }

    private SeguroRegiao resolveRegiao(String codigo) {
        if (codigo == null) {
            throw new CheckoutException(ErroCodigo.REGIAO_INVALIDA);
        }
        try {
            return SeguroRegiao.valueOf(codigo);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(ErroCodigo.REGIAO_INVALIDA);
        }
    }

    private double calcularPesoTotal(List<ItemCarrinho> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            peso = peso.add(BigDecimal.valueOf(item.pesoKg()).multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return peso.doubleValue();
    }

    private BigDecimal calcularSubtotal(List<ItemCarrinho> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            subtotal = subtotal.add(
                    item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade()))
            );
        }
        return subtotal.setScale(2, RoundingMode.HALF_EVEN);
    }
}
