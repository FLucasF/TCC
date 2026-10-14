package com.loja.persistencia;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

/**
 * Cliente mínimo do protocolo do PostgreSQL (consultas simples, texto), sem depender de driver externo.
 * Aceita autenticação por senha em texto, md5 e SCRAM-SHA-256.
 */
public final class PgConexao implements AutoCloseable {

    public static class ErroSql extends RuntimeException {
        public ErroSql(String mensagem) {
            super(mensagem);
        }
    }

    private final String host;
    private final int porta;
    private final String banco;
    private final String usuario;
    private final String senha;

    private Socket socket;
    private DataInputStream entrada;
    private DataOutputStream saida;

    /** @param jdbcUrl no formato jdbc:postgresql://host:porta/banco */
    public PgConexao(String jdbcUrl, String usuario, String senha) {
        String url = jdbcUrl.strip();
        if (url.startsWith("jdbc:")) {
            url = url.substring(5);
        }
        URI uri = URI.create(url);
        this.host = uri.getHost() == null ? "localhost" : uri.getHost();
        this.porta = uri.getPort() < 0 ? 5432 : uri.getPort();
        String caminho = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");
        this.banco = caminho.isEmpty() ? usuario : caminho;
        this.usuario = usuario;
        this.senha = senha == null ? "" : senha;
    }

    /** Executa uma consulta (ou vários comandos separados por ;) e devolve as linhas, em texto. */
    public synchronized List<String[]> consultar(String sql) {
        try {
            return enviarComRetentativa(sql);
        } catch (IOException e) {
            fecharSilenciosamente();
            throw new UncheckedIOException(e);
        }
    }

    /** Executa os comandos numa única transação. */
    public synchronized void transacao(List<String> comandos) {
        if (comandos.isEmpty()) {
            return;
        }
        consultar("BEGIN;" + String.join(";", comandos) + ";COMMIT");
    }

    private List<String[]> enviarComRetentativa(String sql) throws IOException {
        if (socket == null) {
            conectar();
            return executar(sql);
        }
        try {
            return executar(sql);
        } catch (IOException e) {
            // a conexão pode ter caído desde o último uso: tenta de novo numa conexão nova
            fecharSilenciosamente();
            conectar();
            return executar(sql);
        }
    }

    private List<String[]> executar(String sql) throws IOException {
        byte[] texto = sql.getBytes(StandardCharsets.UTF_8);
        saida.writeByte('Q');
        saida.writeInt(4 + texto.length + 1);
        saida.write(texto);
        saida.writeByte(0);
        saida.flush();

        List<String[]> linhas = new ArrayList<>();
        String erro = null;
        while (true) {
            char tipo = (char) entrada.readUnsignedByte();
            byte[] corpo = lerCorpo();
            switch (tipo) {
                case 'D' -> linhas.add(lerLinha(corpo));
                case 'E' -> {
                    if (erro == null) {
                        erro = descreverErro(corpo);
                    }
                }
                case 'Z' -> {
                    if (corpo.length > 0 && corpo[0] != 'I') {
                        // transação ficou aberta/abortada por causa do erro
                        enviarSimples("ROLLBACK");
                    }
                    if (erro != null) {
                        throw new ErroSql(erro);
                    }
                    return linhas;
                }
                default -> {
                }
            }
        }
    }

    private void enviarSimples(String sql) throws IOException {
        byte[] texto = sql.getBytes(StandardCharsets.UTF_8);
        saida.writeByte('Q');
        saida.writeInt(4 + texto.length + 1);
        saida.write(texto);
        saida.writeByte(0);
        saida.flush();
        while (true) {
            char tipo = (char) entrada.readUnsignedByte();
            lerCorpo();
            if (tipo == 'Z') {
                return;
            }
        }
    }

    private byte[] lerCorpo() throws IOException {
        int tamanho = entrada.readInt() - 4;
        byte[] corpo = new byte[tamanho];
        entrada.readFully(corpo);
        return corpo;
    }

    private static String[] lerLinha(byte[] corpo) {
        int posicao = 0;
        int colunas = ((corpo[0] & 0xff) << 8) | (corpo[1] & 0xff);
        posicao += 2;
        String[] valores = new String[colunas];
        for (int i = 0; i < colunas; i++) {
            int tamanho = ((corpo[posicao] & 0xff) << 24) | ((corpo[posicao + 1] & 0xff) << 16)
                    | ((corpo[posicao + 2] & 0xff) << 8) | (corpo[posicao + 3] & 0xff);
            posicao += 4;
            if (tamanho >= 0) {
                valores[i] = new String(corpo, posicao, tamanho, StandardCharsets.UTF_8);
                posicao += tamanho;
            }
        }
        return valores;
    }

    private static String descreverErro(byte[] corpo) {
        String mensagem = "erro do banco";
        String codigo = "";
        int i = 0;
        while (i < corpo.length && corpo[i] != 0) {
            char campo = (char) corpo[i++];
            int fim = i;
            while (corpo[fim] != 0) {
                fim++;
            }
            String valor = new String(corpo, i, fim - i, StandardCharsets.UTF_8);
            if (campo == 'M') {
                mensagem = valor;
            } else if (campo == 'C') {
                codigo = valor;
            }
            i = fim + 1;
        }
        return codigo + " " + mensagem;
    }

    // ---- conexão e autenticação ----

    private void conectar() throws IOException {
        Socket novo = new Socket();
        try {
            novo.connect(new InetSocketAddress(host, porta), 5_000);
            novo.setSoTimeout(30_000);
            novo.setTcpNoDelay(true);
            socket = novo;
            entrada = new DataInputStream(new BufferedInputStream(novo.getInputStream()));
            saida = new DataOutputStream(new BufferedOutputStream(novo.getOutputStream()));
            iniciar();
        } catch (IOException | RuntimeException e) {
            fecharSilenciosamente();
            try {
                novo.close();
            } catch (IOException ignorado) {
                // nada a fazer
            }
            throw e;
        }
    }

    private void iniciar() throws IOException {
        ByteArrayOutputStream parametros = new ByteArrayOutputStream();
        for (String s : new String[] {"user", usuario, "database", banco, "client_encoding", "UTF8"}) {
            parametros.write(s.getBytes(StandardCharsets.UTF_8));
            parametros.write(0);
        }
        parametros.write(0);
        saida.writeInt(4 + 4 + parametros.size());
        saida.writeInt(196608);
        saida.write(parametros.toByteArray());
        saida.flush();

        Scram scram = null;
        while (true) {
            char tipo = (char) entrada.readUnsignedByte();
            byte[] corpo = lerCorpo();
            switch (tipo) {
                case 'R' -> {
                    int codigo = ((corpo[0] & 0xff) << 24) | ((corpo[1] & 0xff) << 16)
                            | ((corpo[2] & 0xff) << 8) | (corpo[3] & 0xff);
                    switch (codigo) {
                        case 0 -> {
                        }
                        case 3 -> enviarSenha(senha.getBytes(StandardCharsets.UTF_8));
                        case 5 -> enviarSenha(senhaMd5(corpo).getBytes(StandardCharsets.UTF_8));
                        case 10 -> {
                            scram = new Scram("", senha, novoNonce());
                            byte[] inicial = scram.primeiraMensagem().getBytes(StandardCharsets.UTF_8);
                            ByteArrayOutputStream msg = new ByteArrayOutputStream();
                            msg.write("SCRAM-SHA-256".getBytes(StandardCharsets.UTF_8));
                            msg.write(0);
                            msg.write(new byte[] {(byte) (inicial.length >>> 24), (byte) (inicial.length >>> 16),
                                (byte) (inicial.length >>> 8), (byte) inicial.length});
                            msg.write(inicial);
                            enviarMensagemSenha(msg.toByteArray());
                        }
                        case 11 -> {
                            if (scram == null) {
                                throw new IOException("SCRAM fora de ordem");
                            }
                            String servidor = new String(corpo, 4, corpo.length - 4, StandardCharsets.UTF_8);
                            enviarMensagemSenha(scram.mensagemFinal(servidor).getBytes(StandardCharsets.UTF_8));
                        }
                        case 12 -> {
                        }
                        default -> throw new IOException("Autenticação não suportada: " + codigo);
                    }
                }
                case 'E' -> throw new ErroSql(descreverErro(corpo));
                case 'Z' -> {
                    return;
                }
                default -> {
                }
            }
        }
    }

    private void enviarSenha(byte[] senhaBytes) throws IOException {
        byte[] comZero = new byte[senhaBytes.length + 1];
        System.arraycopy(senhaBytes, 0, comZero, 0, senhaBytes.length);
        enviarMensagemSenha(comZero);
    }

    private void enviarMensagemSenha(byte[] corpo) throws IOException {
        saida.writeByte('p');
        saida.writeInt(4 + corpo.length);
        saida.write(corpo);
        saida.flush();
    }

    private String senhaMd5(byte[] corpo) throws IOException {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            HexFormat hex = HexFormat.of();
            String interno = hex.formatHex(md5.digest((senha + usuario).getBytes(StandardCharsets.UTF_8)));
            md5.reset();
            md5.update(interno.getBytes(StandardCharsets.UTF_8));
            md5.update(corpo, 4, 4);
            return "md5" + hex.formatHex(md5.digest());
        } catch (java.security.GeneralSecurityException e) {
            throw new IOException(e);
        }
    }

    private static String novoNonce() {
        byte[] bytes = new byte[18];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private void fecharSilenciosamente() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                // nada a fazer
            }
        }
        socket = null;
        entrada = null;
        saida = null;
    }

    @Override
    public synchronized void close() {
        if (saida != null) {
            try {
                saida.writeByte('X');
                saida.writeInt(4);
                saida.flush();
            } catch (IOException e) {
                // nada a fazer
            }
        }
        fecharSilenciosamente();
    }
}
