package com.loja.checkout.domain;

import com.loja.checkout.web.RequisicaoCheckout;
import com.loja.checkout.web.RequisicaoCheckout.ItemRequest;
import com.loja.checkout.web.RespostaResumo;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Orquestra o cálculo do resumo. O que é igual a todos os casos mora aqui: a
 * ordem do cálculo e a ordem das validações (devolvendo o primeiro problema).
 * O que muda de caso para caso delega para as famílias (entrega, cupom, clube,
 * pagamento) e para a região.
 */
@Service
public class CalculadoraResumo {

    public RespostaResumo calcular(RequisicaoCheckout req) {
        // 1. Carrinho
        Pedido pedido = construirPedido(req.itens());

        // 2. a 4. Dados obrigatórios do pedido, na ordem do enunciado
        NivelClube nivel = exigir(NivelClube.fromCodigo(req.nivelClube()), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = exigir(Regiao.fromCodigo(req.regiao()), "REGIAO_INVALIDA");
        ModalidadeEntrega modalidade = exigir(ModalidadeEntrega.fromCodigo(req.modalidadeEntrega()), "MODALIDADE_INVALIDA");

        // 5. Modalidade existe mas não atende o pedido
        if (!modalidade.atende(pedido.pesoTotal())) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        // 6. e 7. Cupom (opcional)
        Cupom cupom = resolverCupom(req.cupom(), pedido);

        // 8. e 9. Forma de pagamento e parcelas
        FormaPagamento forma = exigir(FormaPagamento.fromCodigo(req.formaPagamento()), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        if (!forma.parcelasPermitidas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        // Cálculo do total do pedido
        BigDecimal subtotal = pedido.subtotalProdutos();
        BigDecimal frete = nivel.freteGratis() ? Dinheiro.ZERO : modalidade.frete(pedido.pesoTotal());
        BigDecimal descontoCupom = cupom == null ? Dinheiro.ZERO : cupom.desconto(pedido, frete);
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        // 10. Forma existe mas não atende o pedido (ex.: boleto acima de R$ 1.000)
        if (!forma.atende(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento pagamento = forma.calcular(totalPedido, parcelas);
        BigDecimal ajuste = pagamento.totalFinal().subtract(totalPedido);

        return new RespostaResumo(
                subtotal,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                ajuste,
                pagamento.totalFinal(),
                parcelas,
                pagamento.valorParcela(),
                nivel.credito(subtotal),
                nivel.brinde(subtotal));
    }

    private Pedido construirPedido(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal peso = BigDecimal.ZERO;
        List<Pedido.Item> itensPedido = new ArrayList<>();
        for (ItemRequest item : itens) {
            if (item.precoUnitario() == null || item.quantidade() == null || item.pesoKg() == null
                    || item.precoUnitario().signum() <= 0 || item.quantidade() <= 0 || item.pesoKg().signum() <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
            BigDecimal qtd = BigDecimal.valueOf(item.quantidade());
            subtotal = subtotal.add(item.precoUnitario().multiply(qtd));
            peso = peso.add(item.pesoKg().multiply(qtd));
            itensPedido.add(new Pedido.Item(item.precoUnitario(), item.quantidade()));
        }
        return new Pedido(Dinheiro.arredonda(subtotal), peso, itensPedido);
    }

    private Cupom resolverCupom(String codigo, Pedido pedido) {
        if (codigo == null) {
            return null;
        }
        Cupom cupom = exigir(Cupom.fromCodigo(codigo), "CUPOM_INVALIDO");
        if (!cupom.aplicavel(pedido)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
        return cupom;
    }

    private static <T> T exigir(Optional<T> valor, String codigoErro) {
        return valor.orElseThrow(() -> new CheckoutException(codigoErro));
    }
}
