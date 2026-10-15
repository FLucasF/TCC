package com.loja.checkout.service;

import com.loja.checkout.dinheiro.Dinheiro;
import com.loja.checkout.domain.Cupom;
import com.loja.checkout.domain.CupomContexto;
import com.loja.checkout.domain.FormaPagamento;
import com.loja.checkout.domain.ModalidadeEntrega;
import com.loja.checkout.domain.NivelClube;
import com.loja.checkout.domain.Regiao;
import com.loja.checkout.domain.ResultadoPagamento;
import com.loja.checkout.web.CheckoutRequest;
import com.loja.checkout.web.ItemPedido;
import com.loja.checkout.web.ResumoCompra;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Monta o resumo da compra. A escolha de cada caso (modalidade, cupom, nível,
 * região, pagamento) é resolvida por nome para a constante correspondente, que
 * carrega o próprio comportamento; aqui fica só a ordem do cálculo e a ordem
 * das verificações de erro.
 */
@Service
public class CheckoutService {

    public ResumoCompra calcular(CheckoutRequest req) {
        validarItens(req.itens());

        NivelClube nivel = resolver(NivelClube.class, req.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = resolver(Regiao.class, req.regiao(), "REGIAO_INVALIDA");
        ModalidadeEntrega modalidade = resolver(ModalidadeEntrega.class, req.modalidadeEntrega(), "MODALIDADE_INVALIDA");

        BigDecimal peso = pesoTotal(req.itens());
        if (!modalidade.disponivel(peso)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotal = subtotal(req.itens());
        BigDecimal frete = nivel.frete(modalidade.frete(peso));

        Cupom cupom = req.cupom() == null ? null : resolver(Cupom.class, req.cupom(), "CUPOM_INVALIDO");
        CupomContexto ctx = new CupomContexto(subtotal, frete, req.itens());
        if (cupom != null && !cupom.aplicavel(ctx)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
        BigDecimal desconto = cupom == null ? Dinheiro.arredondar(BigDecimal.ZERO) : cupom.desconto(ctx);

        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal total = Dinheiro.arredondar(subtotal.subtract(desconto).add(frete).add(seguro));

        FormaPagamento forma = resolver(FormaPagamento.class, req.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        if (!forma.parcelasValida(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
        if (!forma.disponivel(total)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        ResultadoPagamento pagamento = forma.calcular(total, parcelas);

        return new ResumoCompra(
                subtotal,
                desconto,
                frete,
                modalidade.prazoDias(),
                seguro,
                pagamento.ajuste(),
                pagamento.totalFinal(),
                pagamento.parcelas(),
                pagamento.valorParcela(),
                nivel.credito(subtotal),
                nivel.brinde(subtotal));
    }

    private void validarItens(List<ItemPedido> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (ItemPedido item : itens) {
            if (item == null
                    || naoPositivo(item.precoUnitario())
                    || item.quantidade() == null || item.quantidade() <= 0
                    || naoPositivo(item.pesoKg())) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private boolean naoPositivo(BigDecimal valor) {
        return valor == null || valor.compareTo(BigDecimal.ZERO) <= 0;
    }

    private BigDecimal pesoTotal(List<ItemPedido> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            peso = peso.add(item.pesoKg().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return peso;
    }

    private BigDecimal subtotal(List<ItemPedido> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            subtotal = subtotal.add(item.precoUnitario().multiply(BigDecimal.valueOf(item.quantidade())));
        }
        return Dinheiro.arredondar(subtotal);
    }

    private static <E extends Enum<E>> E resolver(Class<E> tipo, String nome, String erro) {
        if (nome == null) {
            throw new CheckoutException(erro);
        }
        try {
            return Enum.valueOf(tipo, nome);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(erro);
        }
    }
}
