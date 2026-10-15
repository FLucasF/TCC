package com.loja.checkout.servico;

import com.loja.checkout.dominio.ContextoCupom;
import com.loja.checkout.dominio.Cupom;
import com.loja.checkout.dominio.FormaPagamento;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.ModalidadeEntrega;
import com.loja.checkout.dominio.NivelClube;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.dominio.ResultadoPagamento;
import com.loja.checkout.erro.PedidoException;
import com.loja.checkout.util.Dinheiro;
import com.loja.checkout.web.dto.ItemRequest;
import com.loja.checkout.web.dto.ResumoRequest;
import com.loja.checkout.web.dto.ResumoResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Único lugar que conhece a ordem de validação e a ordem de cálculo do
 * resumo. Cada decisão específica de um caso (modalidade, cupom, nível do
 * clube, forma de pagamento) é delegada ao enum correspondente.
 */
@Service
public class CalculadoraResumo {

    public ResumoResponse calcular(ResumoRequest pedido) {
        List<Item> itens = validarItens(pedido.itens());
        BigDecimal subtotalProdutos = Dinheiro.arredondar(somarSubtotal(itens));
        BigDecimal pesoTotal = somarPeso(itens);

        NivelClube nivelClube = parseEnum(NivelClube.class, pedido.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = parseEnum(Regiao.class, pedido.regiao(), "REGIAO_INVALIDA");
        BigDecimal seguro = regiao.calcularSeguro(subtotalProdutos);

        ModalidadeEntrega modalidade = parseEnum(ModalidadeEntrega.class, pedido.modalidadeEntrega(), "MODALIDADE_INVALIDA");
        modalidade.validarDisponibilidade(pesoTotal);
        BigDecimal frete = nivelClube.isentaFrete() ? BigDecimal.ZERO : modalidade.calcularFrete(pesoTotal);

        BigDecimal descontoCupom = calcularDescontoCupom(pedido.cupom(), subtotalProdutos, itens, frete);

        BigDecimal totalPedido = Dinheiro.arredondar(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        FormaPagamento formaPagamento = parseEnum(FormaPagamento.class, pedido.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = pedido.parcelas() == null ? 1 : pedido.parcelas();
        formaPagamento.validarParcelas(parcelas);
        formaPagamento.validarDisponibilidade(totalPedido);

        ResultadoPagamento resultadoPagamento = formaPagamento.calcular(totalPedido, parcelas);

        BigDecimal credito = nivelClube.calcularCredito(subtotalProdutos);
        boolean brinde = nivelClube.ganhaBrinde(subtotalProdutos);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                resultadoPagamento.ajuste(),
                resultadoPagamento.totalFinal(),
                parcelas,
                resultadoPagamento.valorParcela(),
                credito,
                brinde);
    }

    private List<Item> validarItens(List<ItemRequest> itensRequest) {
        if (itensRequest == null || itensRequest.isEmpty()) {
            throw new PedidoException("PEDIDO_INVALIDO");
        }
        List<Item> itens = itensRequest.stream()
                .map(itemRequest -> new Item(
                        itemRequest.nome(),
                        itemRequest.precoUnitario(),
                        itemRequest.quantidade() == null ? 0 : itemRequest.quantidade(),
                        itemRequest.pesoKg()))
                .toList();
        if (itens.stream().anyMatch(item -> !item.valido())) {
            throw new PedidoException("PEDIDO_INVALIDO");
        }
        return itens;
    }

    private BigDecimal somarSubtotal(List<Item> itens) {
        return itens.stream().map(Item::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal somarPeso(List<Item> itens) {
        return itens.stream().map(Item::pesoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private <T extends Enum<T>> T parseEnum(Class<T> tipo, String valor, String codigoErro) {
        if (valor == null) {
            throw new PedidoException(codigoErro);
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException excecao) {
            throw new PedidoException(codigoErro);
        }
    }

    private BigDecimal calcularDescontoCupom(String cupomCodigo, BigDecimal subtotalProdutos, List<Item> itens,
            BigDecimal frete) {
        if (cupomCodigo == null) {
            return BigDecimal.ZERO;
        }
        Cupom cupom = parseEnum(Cupom.class, cupomCodigo, "CUPOM_INVALIDO");
        ContextoCupom contexto = new ContextoCupom(subtotalProdutos, itens, frete);
        cupom.validarAplicavel(contexto);
        return cupom.calcularDesconto(contexto);
    }
}
