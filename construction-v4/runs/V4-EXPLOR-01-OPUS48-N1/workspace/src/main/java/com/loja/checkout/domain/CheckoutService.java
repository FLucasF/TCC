package com.loja.checkout.domain;

import static com.loja.checkout.domain.Dinheiro.centavos;

import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Calcula o resumo da compra.
 *
 * O método segue a ordem do enunciado: primeiro as conferências, na ordem
 * exata em que o problema recusa o pedido (a primeira que falha devolve o
 * código); depois o cálculo, na ordem em que o valor final é montado. O que
 * varia entre casos (entrega, cupom, clube, região, pagamento) está em cada
 * enum; aqui só se orquestra a sequência.
 */
@Service
public class CheckoutService {

    public ResumoResponse calcular(ResumoRequest req) {
        // 1. Itens válidos.
        List<Item> itens = validarItens(req.itens());

        // 2. Nível do clube. 3. Região. 4. Modalidade de entrega.
        NivelClube nivel = parse(NivelClube.class, req.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = parse(Regiao.class, req.regiao(), "REGIAO_INVALIDA");
        ModalidadeEntrega modalidade = parse(ModalidadeEntrega.class, req.modalidadeEntrega(), "MODALIDADE_INVALIDA");

        // 5. Modalidade atende o pedido?
        BigDecimal pesoKg = ModalidadeEntrega.pesoTotal(itens);
        if (!modalidade.disponivel(pesoKg)) {
            throw new PedidoRecusadoException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotalProdutos = centavos(somaProdutos(itens));
        BigDecimal frete = nivel.freteDevido(modalidade.frete(pesoKg));

        // 6. Cupom existe? 7. Cupom se aplica?
        Cupom cupom = parseCupom(req.cupom());
        Cupom.Contexto ctxCupom = new Cupom.Contexto(subtotalProdutos, frete, itens);
        if (cupom != null && !cupom.aplicavel(ctxCupom)) {
            throw new PedidoRecusadoException("CUPOM_NAO_APLICAVEL");
        }
        BigDecimal descontoCupom = cupom == null ? centavos(BigDecimal.ZERO) : cupom.desconto(ctxCupom);

        BigDecimal seguro = regiao.seguro(subtotalProdutos);
        BigDecimal totalPedido = subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro);

        // 8. Forma de pagamento. 9. Parcelamento permitido. 10. Forma atende o pedido?
        FormaPagamento pagamento = parse(FormaPagamento.class, req.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        pagamento.validarParcelas(parcelas);
        pagamento.validarDisponibilidade(totalPedido);

        FormaPagamento.Resultado resultado = pagamento.resultado(totalPedido, parcelas);
        BigDecimal ajustePagamento = centavos(resultado.totalFinal().subtract(totalPedido));

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajustePagamento,
                resultado.totalFinal(),
                parcelas,
                resultado.valorParcela(),
                nivel.creditoProximaCompra(subtotalProdutos),
                nivel.ganhaBrinde(subtotalProdutos));
    }

    private List<Item> validarItens(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new PedidoRecusadoException("PEDIDO_INVALIDO");
        }
        return itens.stream().map(this::validarItem).toList();
    }

    private Item validarItem(ItemRequest item) {
        if (item == null
                || naoPositivo(item.precoUnitario())
                || item.quantidade() == null || item.quantidade() <= 0
                || naoPositivo(item.pesoKg())) {
            throw new PedidoRecusadoException("PEDIDO_INVALIDO");
        }
        return new Item(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private boolean naoPositivo(BigDecimal valor) {
        return valor == null || valor.compareTo(BigDecimal.ZERO) <= 0;
    }

    private BigDecimal somaProdutos(List<Item> itens) {
        return itens.stream()
                .map(Item::totalLinha)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Cupom parseCupom(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        return parse(Cupom.class, codigo, "CUPOM_INVALIDO");
    }

    private static <E extends Enum<E>> E parse(Class<E> tipo, String valor, String erro) {
        if (valor == null) {
            throw new PedidoRecusadoException(erro);
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            throw new PedidoRecusadoException(erro);
        }
    }
}
