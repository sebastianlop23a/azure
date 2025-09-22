package SCRUM3.Bj_Byte.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    /**
     * 📧 Envía un correo electrónico a un solo destinatario.
     *
     * @param para   Dirección de correo del destinatario
     * @param asunto Asunto del correo
     * @param cuerpo Contenido del correo
     */
    public void enviarCorreo(String para, String asunto, String cuerpo) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom("noreply@miapp.com"); // remitente ficticio
        mensaje.setTo(para);
        mensaje.setSubject(asunto);
        mensaje.setText(cuerpo);

        try {
            mailSender.send(mensaje);
            System.out.println("✅ Correo enviado a: " + para);
        } catch (Exception e) {
            System.err.println("❌ Error al enviar correo a " + para + ": " + e.getMessage());
        }
    }

    /**
     * 📧 Envía el mismo correo a varios destinatarios (uno por uno).
     * Ideal para evitar que todos vean las direcciones de los demás.
     *
     * @param destinatarios Lista de correos
     * @param asunto        Asunto del correo
     * @param cuerpo        Contenido del correo
     */
    public void enviarCorreoMasivo(List<String> destinatarios, String asunto, String cuerpo) {
        for (String correo : destinatarios) {
            enviarCorreo(correo, asunto, cuerpo);
        }
    }

    /**
     * 📧 Envía un solo correo con todos los destinatarios en copia oculta (BCC).
     * Útil si quieres hacer un envío rápido (menos recomendado si quieres personalización).
     *
     * @param destinatarios Lista de correos
     * @param asunto        Asunto del correo
     * @param cuerpo        Contenido del correo
     */
    public void enviarCorreoMasivoBCC(List<String> destinatarios, String asunto, String cuerpo) {
        if (destinatarios == null || destinatarios.isEmpty()) {
            System.out.println("⚠️ No hay destinatarios para el correo masivo.");
            return;
        }

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom("noreply@miapp.com");
        mensaje.setBcc(destinatarios.toArray(new String[0])); // todos en BCC
        mensaje.setSubject(asunto);
        mensaje.setText(cuerpo);

        try {
            mailSender.send(mensaje);
            System.out.println("✅ Correo masivo enviado a " + destinatarios.size() + " destinatarios.");
        } catch (Exception e) {
            System.err.println("❌ Error en envío masivo: " + e.getMessage());
        }
    }
}
