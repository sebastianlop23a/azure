package SCRUM3.Bj_Byte.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.*;

@Service
public class ExchangeRateService {

    private static final String API_URL = "https://open.er-api.com/v6/latest/COP";
    private final RestTemplate restTemplate;

    public ExchangeRateService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Convierte desde COP a otra moneda (ej: USD, EUR).
     */
    public BigDecimal convertFromCOP(BigDecimal amount, String targetCurrency) {
        try {
            Map response = restTemplate.getForObject(API_URL, Map.class);

            if (response != null) {
                // ⚡ Obtenemos las tasas
                Map<String, Object> rates = (Map<String, Object>) response.get("rates");
                if (rates != null && rates.containsKey(targetCurrency)) {
                    double rate = ((Number) rates.get(targetCurrency)).doubleValue();
                    return amount.multiply(BigDecimal.valueOf(rate));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO; // En caso de error
    }

    /**
     * Obtiene la fecha de la última actualización en español.
     */
    public String getUltimaActualizacion() {
        try {
            Map response = restTemplate.getForObject(API_URL, Map.class);

            if (response != null && response.containsKey("time_last_update_utc")) {
                String fechaOriginal = (String) response.get("time_last_update_utc");

                // Ejemplo: "Sun, 21 Sep 2025 00:02:32 +0000"
                SimpleDateFormat formatoIngles = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
                Date fecha = formatoIngles.parse(fechaOriginal);

                // Formatear en español
                SimpleDateFormat formatoEspanol = new SimpleDateFormat("EEEE, d 'de' MMMM yyyy HH:mm:ss", new Locale("es", "ES"));
                return formatoEspanol.format(fecha);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return "Fecha no disponible";
    }
}
