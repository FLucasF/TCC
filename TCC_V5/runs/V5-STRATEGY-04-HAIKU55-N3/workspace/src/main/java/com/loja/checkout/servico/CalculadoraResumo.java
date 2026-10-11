package com.loja.checkout.servico;

import com.loja.checkout.dominio.Codigos;
import com.loja.checkout.dominio.Cupom;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ErroPedidoException;
import com.loja.checkout.dominio.FormaPagamento;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.Modalidade;
import com.loja.checkout.dominio.NivelClube;
import com.loja.checkout.dominio.Pagamento;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.web.ResumoRequest;
import com.loja.checkout.web.ResumoRequest.ItemRequest;
import com.loja.checkout.web.ResumoResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class CalculadoraResumo {

    public ResumoResponse calcular(ResumoRequest req) {
        Pedido pedido = montarPedido(req.itens());
        NivelClube nivel = buscar(NivelClube.class, req.nivelClube(), "NIVEL_CLUBE_INVALIDO");
        Regiao regiao = buscar(Regiao.class, req.regiao(), "REGIAO_INVALIDA");
        Modalidade modalidade = buscar(Modalidade.class, req.modalidadeEntrega(), "MODALIDADE_INVALIDA");
        if (!modalidade.atende(pedido.peso())) {
            throw new ErroPedidoException("MODALIDADE_INDISPONIVEL");
        }
        Cupom cupom = cupomOpcional(req.cupom(), pedido);
        FormaPagamento forma = buscar(FormaPagamento.class, req.formaPagamento(), "FORMA_PAGAMENTO_INVALIDA");
        int parcelas = Optional.ofNullable(req.parcelas()).orElse(1);
        if (!forma.parcelasPermitidas(parcelas)) {
            throw new ErroPedidoException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal subtotal = pedido.subtotal();
        BigDecimal frete = Dinheiro.centavos(nivel.frete(modalidade.frete(pedido.peso())));
        BigDecimal desconto = cupom == null ? BigDecimal.ZERO : Dinheiro.centavos(cupom.desconto(pedido, frete));
        BigDecimal seguro = Dinheiro.centavos(subtotal.multiply(regiao.percentualSeguro()));
        BigDecimal total = subtotal.subtract(desconto).add(frete).add(seguro);
        if (!forma.disponivel(total)) {
            throw new ErroPedidoException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        Pagamento pagamento = forma.calcular(total, parcelas);

        return new ResumoResponse(
                subtotal,
                desconto,
                frete,
                modalidade.prazoDias(),
                seguro,
                pagamento.totalFinal().subtract(total),
                pagamento.totalFinal(),
                pagamento.parcelas(),
                pagamento.valorParcela(),
                Dinheiro.centavos(nivel.credito(subtotal)),
                nivel.temBrinde(subtotal));
    }

    private Pedido montarPedido(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new ErroPedidoException("PEDIDO_INVALIDO");
        }
        List<ItemPedido> resultado = new ArrayList<>();
        for (ItemRequest item : itens) {
            if (item == null
                    || item.precoUnitario() == null || item.precoUnitario().signum() <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg().signum() <= 0) {
                throw new ErroPedidoException("PEDIDO_INVALIDO");
            }
            resultado.add(new ItemPedido(item.precoUnitario(), item.quantidade(), item.pesoKg()));
        }
        return new Pedido(resultado);
    }

    private Cupom cupomOpcional(String codigo, Pedido pedido) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        Cupom cupom = buscar(Cupom.class, codigo, "CUPOM_INVALIDO");
        if (!cupom.aplicavel(pedido)) {
            throw new ErroPedidoException("CUPOM_NAO_APLICAVEL");
        }
        return cupom;
    }

    private static <E extends Enum<E>> E buscar(Class<E> tipo, String codigo, String erro) {
        return Codigos.buscar(tipo, codigo).orElseThrow(() -> new ErroPedidoException(erro));
    }
}
