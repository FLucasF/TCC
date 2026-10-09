package com.loja.checkout.service;

import com.loja.checkout.domain.Cupom;
import com.loja.checkout.domain.Dinheiro;
import com.loja.checkout.domain.FormaPagamento;
import com.loja.checkout.domain.Item;
import com.loja.checkout.domain.ModalidadeEntrega;
import com.loja.checkout.domain.NivelClube;
import com.loja.checkout.domain.PagamentoResultado;
import com.loja.checkout.domain.Regiao;
import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.CheckoutResponse;
import com.loja.checkout.dto.ItemRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Calcula o resumo da compra. As validações seguem a ordem definida e o
 * primeiro problema encontrado recusa o pedido com seu código.
 */
@Service
public class CheckoutService {

    public CheckoutResponse calcular(CheckoutRequest req) {
        // 1. Itens
        List<Item> itens = validarItens(req == null ? null : req.itens());

        BigDecimal subtotal = Dinheiro.arredondar(somar(itens, Item::total));
        BigDecimal pesoTotal = somar(itens, Item::peso);

        // 2. Nível do clube
        NivelClube nivel = NivelClube.fromCodigo(req.nivelClube())
                .orElseThrow(() -> new CheckoutException("NIVEL_CLUBE_INVALIDO"));

        // 3. Região
        Regiao regiao = Regiao.fromCodigo(req.regiao())
                .orElseThrow(() -> new CheckoutException("REGIAO_INVALIDA"));

        // 4. Modalidade de entrega
        ModalidadeEntrega modalidade = ModalidadeEntrega.fromCodigo(req.modalidadeEntrega())
                .orElseThrow(() -> new CheckoutException("MODALIDADE_INVALIDA"));

        // 5. Modalidade atende o pedido?
        if (!modalidade.disponivel(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        // Frete: calculado pela modalidade; OURO nunca paga frete.
        BigDecimal frete = nivel.pagaFrete() ? modalidade.frete(pesoTotal) : Dinheiro.ZERO;

        // Seguro: porcentagem da região sobre os produtos.
        BigDecimal seguro = regiao.seguro(subtotal);

        // 6. e 7. Cupom
        BigDecimal descontoCupom = Dinheiro.ZERO;
        if (req.cupom() != null) {
            Cupom cupom = Cupom.fromCodigo(req.cupom())
                    .orElseThrow(() -> new CheckoutException("CUPOM_INVALIDO"));
            if (!cupom.aplicavel(subtotal)) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
            descontoCupom = cupom.desconto(subtotal, itens, frete);
        }

        // Total do pedido = produtos − desconto + frete + seguro.
        BigDecimal totalPedido = Dinheiro.arredondar(
                subtotal.subtract(descontoCupom).add(frete).add(seguro));

        // 8. Forma de pagamento
        FormaPagamento forma = FormaPagamento.fromCodigo(req.formaPagamento())
                .orElseThrow(() -> new CheckoutException("FORMA_PAGAMENTO_INVALIDA"));

        // 9. Parcelamento permitido? (ausente = 1)
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        if (!forma.parcelasValidas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        // 10. Forma atende o pedido?
        if (!forma.disponivel(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        PagamentoResultado pagamento = forma.calcular(totalPedido, parcelas);

        return new CheckoutResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                pagamento.ajuste(),
                pagamento.totalFinal(),
                pagamento.parcelas(),
                pagamento.valorParcela(),
                nivel.credito(subtotal),
                nivel.temBrinde(subtotal));
    }

    private List<Item> validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        List<Item> validos = new ArrayList<>(itens.size());
        for (ItemRequest item : itens) {
            if (item == null
                    || !positivo(item.precoUnitario())
                    || item.quantidade() == null || item.quantidade() <= 0
                    || !positivo(item.pesoKg())) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
            validos.add(new Item(item.precoUnitario(), item.quantidade(), item.pesoKg()));
        }
        return validos;
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private static BigDecimal somar(List<Item> itens, java.util.function.Function<Item, BigDecimal> campo) {
        BigDecimal total = BigDecimal.ZERO;
        for (Item item : itens) {
            total = total.add(campo.apply(item));
        }
        return total;
    }
}
