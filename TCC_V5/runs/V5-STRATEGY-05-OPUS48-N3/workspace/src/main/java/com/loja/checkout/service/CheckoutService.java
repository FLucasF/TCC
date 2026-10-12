package com.loja.checkout.service;

import com.loja.checkout.domain.Cupom;
import com.loja.checkout.domain.Dinheiro;
import com.loja.checkout.domain.FormaPagamento;
import com.loja.checkout.domain.Item;
import com.loja.checkout.domain.ModalidadeEntrega;
import com.loja.checkout.domain.NivelClube;
import com.loja.checkout.domain.Regiao;
import com.loja.checkout.domain.ResultadoPagamento;
import com.loja.checkout.web.CheckoutException;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Calcula o resumo da compra. A sequência do cálculo e a ordem das validações
 * são iguais para todo pedido e moram aqui; o que varia por caso mora nos enums
 * de domínio. As checagens seguem a ordem do enunciado e param no primeiro erro.
 */
@Service
public class CheckoutService {

    public ResumoResponse calcular(ResumoRequest pedido) {
        // 1. Itens válidos → subtotal e peso do pedido.
        List<Item> itens = validarItens(pedido);
        BigDecimal subtotal = somarSubtotal(itens);
        BigDecimal pesoTotal = somarPeso(itens);

        // 2. Nível do clube. 3. Região. 4. Modalidade de entrega.
        NivelClube clube = exigir(NivelClube.class, pedido.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = exigir(Regiao.class, pedido.regiao(), "REGIAO_INVALIDA");
        ModalidadeEntrega modalidade =
                exigir(ModalidadeEntrega.class, pedido.modalidadeEntrega(), "MODALIDADE_INVALIDA");

        // 5. Modalidade atende o pedido (ex.: motoboy acima de 5 kg).
        if (!modalidade.disponivelPara(pesoTotal)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        // Frete: pela modalidade, zerado quando o clube não paga frete.
        BigDecimal frete = clube.freteGratis()
                ? BigDecimal.ZERO.setScale(2)
                : Dinheiro.centavos(modalidade.freteBase(pesoTotal));

        // 6. Cupom existe. 7. Cupom aplicável. (Pode não vir cupom.)
        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (pedido.cupom() != null) {
            Cupom cupom = exigir(Cupom.class, pedido.cupom(), "CUPOM_INVALIDO");
            if (!cupom.aplicavel(subtotal)) {
                throw new CheckoutException("CUPOM_NAO_APLICAVEL");
            }
            descontoCupom = cupom.desconto(itens, subtotal, frete);
        }

        // 8. Forma de pagamento. 9. Parcelamento permitido para a forma.
        FormaPagamento forma =
                exigir(FormaPagamento.class, pedido.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        if (!forma.parcelasValidas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        // Seguro e total do pedido.
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        // 10. Forma atende o pedido (ex.: boleto acima de R$ 1.000).
        if (!forma.disponivelPara(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        // Pagamento e ajuste (final − total).
        ResultadoPagamento pagamento = forma.calcular(totalPedido, parcelas);
        BigDecimal ajuste = pagamento.totalFinal().subtract(totalPedido);

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajuste,
                pagamento.totalFinal(),
                parcelas,
                pagamento.valorParcela(),
                clube.credito(subtotal),
                clube.temBrinde(subtotal));
    }

    private List<Item> validarItens(ResumoRequest pedido) {
        List<ResumoRequest.ItemRequest> enviados = pedido.itens();
        if (enviados == null || enviados.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        List<Item> itens = new ArrayList<>();
        for (ResumoRequest.ItemRequest item : enviados) {
            if (positivo(item.precoUnitario()) && item.quantidade() != null
                    && item.quantidade() > 0 && positivo(item.pesoKg())) {
                itens.add(new Item(item.nome(), item.precoUnitario(),
                        item.quantidade(), item.pesoKg()));
            } else {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
        return itens;
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal somarSubtotal(List<Item> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Item item : itens) {
            subtotal = subtotal.add(item.totalLinha());
        }
        return Dinheiro.centavos(subtotal);
    }

    private BigDecimal somarPeso(List<Item> itens) {
        BigDecimal peso = BigDecimal.ZERO;
        for (Item item : itens) {
            peso = peso.add(item.pesoLinha());
        }
        return peso;
    }

    private <E extends Enum<E>> E exigir(Class<E> tipo, String valor, String codigoErro) {
        if (valor == null) {
            throw new CheckoutException(codigoErro);
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException naoExiste) {
            throw new CheckoutException(codigoErro);
        }
    }
}
