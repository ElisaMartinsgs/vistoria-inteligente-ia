package br.com.saborecia.vistoria.api;

import br.com.saborecia.vistoria.excecao.ArquivoInvalidoException;
import br.com.saborecia.vistoria.excecao.RecursoNaoEncontradoException;
import br.com.saborecia.vistoria.excecao.RegraDeNegocioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.stream.Collectors;

/**
 * Converte exceções em respostas padronizadas (RFC 7807 — application/problem+json),
 * sempre com uma "mensagem" em linguagem simples para o front-end exibir.
 * <p>
 * Falhas da IA NÃO aparecem aqui: são tratadas no ExtracaoService (fallback + revisão manual).
 */
@RestControllerAdvice
public class TratadorDeErros {

    private static final Logger log = LoggerFactory.getLogger(TratadorDeErros.class);

    @ExceptionHandler(ArquivoInvalidoException.class)
    ProblemDetail arquivoInvalido(ArquivoInvalidoException e) {
        return problema(HttpStatus.BAD_REQUEST, "ARQUIVO_INVALIDO", e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail arquivoGrande(MaxUploadSizeExceededException e) {
        return problema(HttpStatus.PAYLOAD_TOO_LARGE, "ARQUIVO_MUITO_GRANDE", "O arquivo passa do limite de 10 MB.");
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ProblemDetail naoEncontrado(RecursoNaoEncontradoException e) {
        return problema(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", e.getMessage());
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    ProblemDetail regra(RegraDeNegocioException e) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "OPERACAO_INVALIDA", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validacao(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return problema(HttpStatus.BAD_REQUEST, "DADOS_INVALIDOS", msg);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    ProblemDetail faltando(Exception e) {
        return problema(HttpStatus.BAD_REQUEST, "CAMPO_OBRIGATORIO", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail tipo(MethodArgumentTypeMismatchException e) {
        return problema(HttpStatus.BAD_REQUEST, "VALOR_INVALIDO",
                "Valor inválido para '" + e.getName() + "': " + e.getValue());
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    ProblemDetail rotaInexistente(Exception e) {
        return problema(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", "Endereço não encontrado.");
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    ProblemDetail metodo(Exception e) {
        return problema(HttpStatus.METHOD_NOT_ALLOWED, "METODO_NAO_SUPORTADO", e.getMessage());
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    ProblemDetail midia(Exception e) {
        return problema(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "FORMATO_NAO_SUPORTADO",
                "Use multipart/form-data para enviar o arquivo e application/json para os demais dados.");
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    ProblemDetail corpo(Exception e) {
        return problema(HttpStatus.BAD_REQUEST, "JSON_INVALIDO", "O corpo da requisição não é um JSON válido.");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail inesperado(Exception e) {
        log.error("Erro inesperado", e);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO",
                "Não foi possível processar agora. Tente novamente em instantes.");
    }

    private static ProblemDetail problema(HttpStatus status, String codigo, String mensagem) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, mensagem);
        p.setTitle(status.getReasonPhrase());
        p.setProperty("codigo", codigo);
        p.setProperty("mensagem", mensagem);
        return p;
    }
}
