package com.loja.checkout.service;

import com.loja.checkout.domain.ContextoCupom;
import com.loja.checkout.domain.Cupom;
import com.loja.checkout.domain.FormaPagamento;
import com.loja.checkout.domain.Item;
import com.loja.checkout.domain.ModalidadeEntrega;
import com.loja.checkout.domain.Money;
import com.loja.checkout.domain.NivelClube;
import com.loja.checkout.domain.PedidoInvalidoException;
import com.loja.checkout.domain.Regiao;
import com.loja.checkout.domain.ResultadoPagamento;
import com.loja.checkout.web.CheckoutRequest;
import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.ResumoResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Calcula o resumo da compra. Confere os problemas na ordem definida pelo
 * enunciado (devolvendo o primeiro encontrado) e só então monta o resumo.
 */
@Service
public class CalculadoraResumo {

    public ResumoResponse calcular(CheckoutRequest req) {
        // 1. Itens
        List<Item> itens = validarItens(req.itens());
        BigDecimal subtotal = Money.centavos(somar(itens, Item::subtotal));
        BigDecimal peso = somar(itens, Item::peso);

        // 2. Clube  3. Região  4. Modalidade
        NivelClube clube = exigir(NivelClube.class, req.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = exigir(Regiao.class, req.regiao(), "REGIAO_INVALIDA");
        ModalidadeEntrega modalidade = exigir(ModalidadeEntrega.class, req.modalidadeEntrega(), "MODALIDADE_INVALIDA");

        // 5. Modalidade atende o pedido?
        if (!modalidade.disponivel(peso)) {
            throw new PedidoInvalidoException("MODALIDADE_INDISPONIVEL");
        }
        BigDecimal frete = Money.centavos(clube.aplicarIsencao(modalidade.frete(peso)));

        // 6. Cupom existe?  7. Cupom aplicável?
        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (req.cupom() != null) {
            Cupom cupom = analisar(Cupom.class, req.cupom());
            if (cupom == null) {
                throw new PedidoInvalidoException("CUPOM_INVALIDO");
            }
            ContextoCupom ctx = new ContextoCupom(subtotal, frete, itens);
            if (!cupom.aplicavel(ctx)) {
                throw new PedidoInvalidoException("CUPOM_NAO_APLICAVEL");
            }
            descontoCupom = Money.centavos(cupom.desconto(ctx));
        }

        // 8. Pagamento existe?  9. Parcelas permitidas?
        FormaPagamento pagamento = exigir(FormaPagamento.class, req.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        if (!pagamento.parcelasValidas(parcelas)) {
            throw new PedidoInvalidoException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal seguro = Money.centavos(subtotal.multiply(regiao.percentualSeguro()));
        BigDecimal totalPedido = Money.centavos(
                subtotal.subtract(descontoCupom).add(frete).add(seguro));

        // 10. Pagamento atende o total?
        if (!pagamento.disponivel(totalPedido)) {
            throw new PedidoInvalidoException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento resultado = pagamento.calcula(totalPedido, parcelas);
        BigDecimal ajuste = resultado.totalFinal().subtract(totalPedido);

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajuste,
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela(),
                clube.credito(subtotal),
                clube.temBrinde(subtotal));
    }

    private List<Item> validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new PedidoInvalidoException("PEDIDO_INVALIDO");
        }
        return itens.stream().map(this::validarItem).toList();
    }

    private Item validarItem(ItemRequest i) {
        if (i == null || !positivo(i.precoUnitario()) || i.quantidade() == null
                || i.quantidade() <= 0 || !positivo(i.pesoKg())) {
            throw new PedidoInvalidoException("PEDIDO_INVALIDO");
        }
        return new Item(i.nome(), i.precoUnitario(), i.quantidade(), i.pesoKg());
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal somar(List<Item> itens, java.util.function.Function<Item, BigDecimal> parcela) {
        return itens.stream().map(parcela).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Converte texto no enum, ou lança o código informado se não existir/estiver ausente. */
    private <E extends Enum<E>> E exigir(Class<E> tipo, String valor, String codigoErro) {
        E resultado = analisar(tipo, valor);
        if (resultado == null) {
            throw new PedidoInvalidoException(codigoErro);
        }
        return resultado;
    }

    /** Converte texto no enum, ou devolve null se for nulo ou não existir. */
    private <E extends Enum<E>> E analisar(Class<E> tipo, String valor) {
        if (valor == null) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
