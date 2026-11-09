package com.mentesvivas.backend.support;

import com.mentesvivas.backend.email.EmailSender;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Primary
public class EmailCapturadoSender implements EmailSender {

    public record Enviado(String destinatario, String assunto, String corpo) {
    }

    private static final Pattern CODIGO = Pattern.compile("<strong>(\\d{6})</strong>");

    private final List<Enviado> enviados = new CopyOnWriteArrayList<>();

    @Override
    public void enviar(String destinatario, String assunto, String corpoHtml) {
        enviados.add(new Enviado(destinatario, assunto, corpoHtml));
    }

    public List<Enviado> enviados() {
        return enviados;
    }

    public String ultimoCodigo() {
        Matcher matcher = CODIGO.matcher(enviados.get(enviados.size() - 1).corpo());
        if (!matcher.find()) {
            throw new IllegalStateException("Ultimo e-mail nao contem codigo");
        }
        return matcher.group(1);
    }

    public void limpar() {
        enviados.clear();
    }
}
