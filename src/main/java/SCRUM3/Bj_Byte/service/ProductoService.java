package SCRUM3.Bj_Byte.service;

import SCRUM3.Bj_Byte.factory.ProductoFactory;
import SCRUM3.Bj_Byte.model.Producto;
import SCRUM3.Bj_Byte.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;

@Service
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    public void cargarProductosDesdeCSV(InputStream inputStream) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String linea;

            while ((linea = br.readLine()) != null) {
                if (linea.trim().isEmpty()) continue; // Ignorar líneas vacías

                // Separar por punto (.)
                String[] campos = linea.split(",");

                if (campos.length >= 4) {
                    try {
                        String nombre = campos[1].trim();
                        String descripcion = campos[2].trim();
                        BigDecimal precio = new BigDecimal(campos[3].trim());

                        // Crear producto sin cantidad (la cantidad va en Inventario)
                        Producto producto = ProductoFactory.crearProducto(nombre, descripcion, precio, null);
                        productoRepository.save(producto);

                        System.out.println("Guardado en BD: " + producto.getNombre());

                    } catch (NumberFormatException e) {
                        System.err.println("Error parseando precio en línea: " + linea);
                    }
                } else {
                    System.err.println("Línea inválida, se ignora: " + linea);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error al procesar CSV", e);
        }
    }
}

