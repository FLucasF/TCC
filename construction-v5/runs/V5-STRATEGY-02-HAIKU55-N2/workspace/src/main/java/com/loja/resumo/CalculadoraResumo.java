package com.loja.resumo;

import static com.loja.resumo.dominio.CodigoErro.CUPOM_INVALIDO;
import static com.loja.resumo.dominio.CodigoErro.CUPOM_NAO_APLICAVEL;
import static com.loja.resumo.dominio.CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL;
import static com.loja.resumo.dominio.CodigoErro.FORMA_PAGAMENTO_INVALIDA;
import static com.loja.resumo.dominio.CodigoErro.MODALIDADE_INDISPONIVEL;
import static com.loja.resumo.dominio.CodigoErro.MODALIDADE_INVALIDA;
import static com.loja.resumo.dominio.CodigoErro.NIVEL_CLUBE_INVALIDO;
import static com.loja.resumo.dominio.CodigoErro.PARCELAMENTO_INVALIDO;
import static com.loja.resumo.dominio.CodigoErro.PEDIDO_INVALIDO;
import static com.loja.resumo.dominio.CodigoErro.REGIAO_INVALIDA;
import static com.loja.resumo.dominio.Dinheiro.arredondar;

import com.loja.resumo.dominio.CodigoErro;
import com.loja.resumo.dominio.Cupom;
import com.loja.resumo.dominio.FormaPagamento;
import com.loja.resumo.dominio.Item;
import com.loja.resumo.dominio.ModalidadeEntrega;
import com.loja.resumo.dominio.NivelClube;
import com.loja.resumo.dominio.Pedido;
import com.loja.resumo.dominio.PedidoRecusadoException;
import com.loja.resumo.dominio.Regiao;
import com.loja.resumo.dominio.ResultadoPagamento;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CalculadoraResumo {

    public ResumoCompraResponse calcular(ResumoCompraRequest request) {
        Pedido pedido = pedido(request.itens());
        NivelClube nivel = opcao(NivelClube.class, request.nivelClube(), NIVEL_CLUBE_INVALIDO);
        Regiao regiao = opcao(Regiao.class, request.regiao(), REGIAO_INVALIDA);
        ModalidadeEntrega modalidade = opcao(ModalidadeEntrega.class, request.modalidadeEntrega(), MODALIDADE_INVALIDA);
        if (!modalidade.atende(pedido.pesoTotal())) {
            throw new PedidoRecusadoException(MODALIDADE_INDISPONIVEL);
        }
        Cupom cupom = request.cupom() == null ? null : opcao(Cupom.class, request.cupom(), CUPOM_INVALIDO);
        if (cupom != null && !cupom.aplicavel(pedido)) {
            throw new PedidoRecusadoException(CUPOM_NAO_APLICAVEL);
        }
        FormaPagamento pagamento = opcao(FormaPagamento.class, request.formaPagamento(), FORMA_PAGAMENTO_INVALIDA);
        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        if (!pagamento.aceitaParcelas(parcelas)) {
            throw new PedidoRecusadoException(PARCELAMENTO_INVALIDO);
        }

        BigDecimal subtotal = arredondar(pedido.subtotal());
        BigDecimal frete = arredondar(nivel.ajustarFrete(modalidade.custo(pedido.pesoTotal())));
        BigDecimal desconto = arredondar(cupom == null ? BigDecimal.ZERO : cupom.desconto(pedido, frete));
        BigDecimal seguro = arredondar(regiao.seguroSobre(subtotal));
        BigDecimal total = subtotal.subtract(desconto).add(frete).add(seguro);
        if (!pagamento.disponivel(total)) {
            throw new PedidoRecusadoException(FORMA_PAGAMENTO_INDISPONIVEL);
        }
        ResultadoPagamento resultado = pagamento.calcular(total, parcelas);

        return new ResumoCompraResponse(
                subtotal,
                desconto,
                frete,
                modalidade.prazoDias(),
                seguro,
                resultado.totalFinal().subtract(total),
                resultado.totalFinal(),
                resultado.parcelas(),
                resultado.valorParcela(),
                arredondar(nivel.creditoSobre(subtotal)),
                nivel.recebeBrinde(subtotal));
    }

    private static Pedido pedido(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new PedidoRecusadoException(PEDIDO_INVALIDO);
        }
        List<Item> resultado = new ArrayList<>();
        for (ItemRequest item : itens) {
            if (item == null || !positivo(item.precoUnitario()) || item.quantidade() == null
                    || item.quantidade() <= 0 || !positivo(item.pesoKg())) {
                throw new PedidoRecusadoException(PEDIDO_INVALIDO);
            }
            resultado.add(new Item(item.precoUnitario(), item.quantidade(), item.pesoKg()));
        }
        return new Pedido(resultado);
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    private static <E extends Enum<E>> E opcao(Class<E> tipo, String nome, CodigoErro codigo) {
        return Arrays.stream(tipo.getEnumConstants())
                .filter(constante -> constante.name().equals(nome))
                .findFirst()
                .orElseThrow(() -> new PedidoRecusadoException(codigo));
    }
}
