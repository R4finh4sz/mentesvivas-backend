package com.mentesvivas.backend.exception.handler;

import com.mentesvivas.backend.exception.auth.AutenticacaoException;
import com.mentesvivas.backend.exception.common.ConflitoException;
import com.mentesvivas.backend.exception.common.RecursoNaoEncontradoException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidacao(MethodArgumentNotValidException ex) {
        Map<String, String> erros = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(erro -> erros.putIfAbsent(erro.getField(), erro.getDefaultMessage()));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dados invalidos");
        problem.setProperty("erros", erros);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleCorpoIlegivel(HttpMessageNotReadableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Corpo da requisicao ausente ou mal formatado");
    }

    @ExceptionHandler(AutenticacaoException.class)
    ProblemDetail handleAutenticacao(AutenticacaoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAcessoNegado(AccessDeniedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ProblemDetail handleNaoEncontrado(RecursoNaoEncontradoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflitoException.class)
    ProblemDetail handleConflito(ConflitoException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleIntegridade(DataIntegrityViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Ja existe um cadastro com estes dados");
    }
}
