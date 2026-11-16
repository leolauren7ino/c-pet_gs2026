package br.com.cpet.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import br.com.cpet.servico.Erros;

@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(Erros.NaoEncontrado.class)
    ResponseEntity<Map<String, String>> naoEncontrado(Erros.NaoEncontrado e) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(Erros.NaoAutenticado.class)
    ResponseEntity<Map<String, String>> naoAutenticado(Erros.NaoAutenticado e) {
        return resposta(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(Erros.AcessoNegado.class)
    ResponseEntity<Map<String, String>> acessoNegado(Erros.AcessoNegado e) {
        return resposta(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(Erros.TransicaoInvalida.class)
    ResponseEntity<Map<String, String>> transicaoInvalida(Erros.TransicaoInvalida e) {
        return resposta(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(Erros.RegraDeNegocio.class)
    ResponseEntity<Map<String, String>> regraDeNegocio(Erros.RegraDeNegocio e) {
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<Map<String, String>> arquivoGrande(MaxUploadSizeExceededException e) {
        return resposta(HttpStatus.PAYLOAD_TOO_LARGE, "A foto passa de 5 MB. Escolha uma imagem menor.");
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
            HttpMediaTypeNotSupportedException.class})
    ResponseEntity<Map<String, String>> requisicaoInvalida(Exception e) {
        return resposta(HttpStatus.BAD_REQUEST, "Dados inválidos. Revise o formulário e tente de novo.");
    }

    private static ResponseEntity<Map<String, String>> resposta(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(Map.of("erro", mensagem));
    }
}
