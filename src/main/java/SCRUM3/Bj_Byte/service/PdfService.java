package SCRUM3.Bj_Byte.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.chart.ChartUtils;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.awt.Color;
import java.io.*;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import SCRUM3.Bj_Byte.model.Venta;
import SCRUM3.Bj_Byte.model.Empleado;
import SCRUM3.Bj_Byte.repository.VentaRepository;
import SCRUM3.Bj_Byte.repository.EmpleadoRepository;

@Service
public class PdfService {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private EmpleadoRepository empleadoRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ------------------------
    // Helpers seguros (evitan NPEs)
    // ------------------------
    private String safeClientName(Venta v) {
        String c = v.getCliente();
        return (c != null && !c.trim().isEmpty()) ? c : "No registrado";
    }

    private String safeProductName(Venta v) {
        if (v.getInventario() != null && v.getInventario().getProducto() != null &&
            v.getInventario().getProducto().getNombre() != null) {
            return v.getInventario().getProducto().getNombre();
        }
        if (v.getNombreProducto() != null && !v.getNombreProducto().trim().isEmpty()) {
            return v.getNombreProducto();
        }
        return "N/A";
    }

    private BigDecimal safeTotal(Venta v) {
        return v.getTotalVenta() != null ? v.getTotalVenta() : BigDecimal.ZERO;
    }

    private String safeUnitPriceFormatted(Venta v) {
        if (v.getInventario() != null && v.getInventario().getProducto() != null &&
            v.getInventario().getProducto().getPrecio() != null) {
            return formatCurrency(v.getInventario().getProducto().getPrecio());
        }
        return "N/A";
    }

    private String safeDateFormatted(Venta v) {
        return v.getFecha() != null ? v.getFecha().format(DATE_FMT) : "N/A";
    }

    private String formatCurrency(BigDecimal amount) {
        return "$" + String.format("%,.2f", amount);
    }

    // =========================
    // 1) REPORTE PARA EMPLEADO (con gráficas)
    // =========================
    public ByteArrayInputStream generarReporteEmpleado(String nombreEmpleado, List<Venta> ventas) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4);
        PdfWriter.getInstance(documento, out);
        documento.open();

        // Título
        Paragraph titulo = new Paragraph("VENTAS - " + (nombreEmpleado != null ? nombreEmpleado : "EMPLEADO"),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Color.BLACK));
        titulo.setAlignment(Element.ALIGN_CENTER);
        titulo.setSpacingAfter(20);
        documento.add(titulo);

        // Dataset para gráfico (ventas por día)
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        Map<DayOfWeek, BigDecimal> ventasPorDiaSemana = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek d : DayOfWeek.values()) ventasPorDiaSemana.put(d, BigDecimal.ZERO);

        for (Venta v : ventas) {
            if (v == null) continue;
            if (v.getFecha() == null) continue; // saltar si no hay fecha
            BigDecimal valor = safeTotal(v);
            DayOfWeek dia = v.getFecha().getDayOfWeek();
            ventasPorDiaSemana.put(dia, ventasPorDiaSemana.getOrDefault(dia, BigDecimal.ZERO).add(valor));
        }

        for (DayOfWeek d : DayOfWeek.values()) {
            dataset.addValue(ventasPorDiaSemana.get(d), "Ventas", d.getDisplayName(TextStyle.SHORT, new Locale("es", "ES")));
        }

        // Crear gráfico
        JFreeChart chart = ChartFactory.createLineChart(
                "Ventas por Día de la Semana",
                "Día",
                "Total Ventas",
                dataset,
                PlotOrientation.VERTICAL,
                false, true, false
        );
        chart.setBackgroundPaint(Color.WHITE);
        if (chart.getPlot() != null) chart.getPlot().setBackgroundPaint(new Color(230, 230, 250));

        ByteArrayOutputStream chartOut = new ByteArrayOutputStream();
        ChartUtils.writeChartAsPNG(chartOut, chart, 500, 300);
        Image chartImage = Image.getInstance(chartOut.toByteArray());
        chartImage.setAlignment(Element.ALIGN_CENTER);
        chartImage.scaleToFit(500, 300);
        documento.add(chartImage);

        documento.add(new Paragraph(" "));

        // Tabla de ventas
        PdfPTable tabla = new PdfPTable(5);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{2f, 3f, 2f, 2f, 2f});

        Stream.of("Fecha", "Producto", "Cantidad", "Precio Unitario", "Total")
                .forEach(header -> {
                    PdfPCell cell = new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE)));
                    cell.setBackgroundColor(Color.DARK_GRAY);
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabla.addCell(cell);
                });

        BigDecimal totalVentas = BigDecimal.ZERO;
        for (Venta v : ventas) {
            if (v == null) continue;
            tabla.addCell(safeDateFormatted(v));
            tabla.addCell(safeProductName(v));
            tabla.addCell(String.valueOf(v.getCantidad()));
            String unit = safeUnitPriceFormatted(v);
            tabla.addCell(unit);
            BigDecimal total = safeTotal(v);
            tabla.addCell(formatCurrency(total));
            totalVentas = totalVentas.add(total);
        }

        documento.add(tabla);
        documento.add(new Paragraph(" "));

        Paragraph resumen = new Paragraph(
                "TOTAL DE VENTAS: " + formatCurrency(totalVentas),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLUE)
        );
        resumen.setAlignment(Element.ALIGN_CENTER);
        documento.add(resumen);

        documento.close();
        return new ByteArrayInputStream(out.toByteArray());
    }

    // =========================
    // 2) REPORTE PARA CLIENTE (detalle de compra)
    // =========================
    public ByteArrayInputStream generarReporteCliente(Venta venta) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4);
        PdfWriter.getInstance(documento, out);
        documento.open();

        // Título
        Paragraph titulo = new Paragraph("DETALLE DE COMPRA",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Color.BLACK));
        titulo.setAlignment(Element.ALIGN_CENTER);
        titulo.setSpacingAfter(20);
        documento.add(titulo);

        // Información general (USAR venta.getCliente() directamente: es String)
        documento.add(new Paragraph("Fecha: " + safeDateFormatted(venta)));
        documento.add(new Paragraph("Cliente: " + safeClientName(venta)));
        documento.add(new Paragraph("Empleado que atendió: " + (venta.getEmpleado() != null ? 
                (venta.getEmpleado().getNombre() != null ? venta.getEmpleado().getNombre() : "N/A") : "N/A")));
        documento.add(new Paragraph("Método de pago: " + (venta.getMetodoPago() != null ? venta.getMetodoPago() : "N/A")));
        documento.add(new Paragraph(" "));

        // Tabla de productos (aquí asumimos 1 producto por venta según tu modelo)
        PdfPTable tabla = new PdfPTable(4);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{3f, 2f, 2f, 2f});

        Stream.of("Producto", "Cantidad", "Precio Unitario", "Subtotal")
                .forEach(header -> {
                    PdfPCell cell = new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE)));
                    cell.setBackgroundColor(Color.DARK_GRAY);
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    tabla.addCell(cell);
                });

        tabla.addCell(safeProductName(venta));
        tabla.addCell(String.valueOf(venta.getCantidad()));
        tabla.addCell(safeUnitPriceFormatted(venta));
        tabla.addCell(formatCurrency(safeTotal(venta)));

        documento.add(tabla);
        documento.add(new Paragraph(" "));

        // Total
        Paragraph total = new Paragraph(
                "TOTAL PAGADO: " + formatCurrency(safeTotal(venta)),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLUE)
        );
        total.setAlignment(Element.ALIGN_RIGHT);
        documento.add(total);

        documento.close();
        return new ByteArrayInputStream(out.toByteArray());
    }

    // =========================
    // 3) EXPORTACIONES (métodos públicos que usa el controlador)
    // =========================
    public void exportarVentasPDF(HttpServletResponse response) throws Exception {
        List<Venta> listaVentas = ventaRepository.findAll();
        if (listaVentas == null || listaVentas.isEmpty()) {
            response.setContentType("text/plain");
            response.getWriter().write("No hay ventas para exportar.");
            return;
        }

        ByteArrayInputStream bis = generarReporteEmpleado("TODAS LAS VENTAS", listaVentas);
        writeStreamToResponse(bis, response, "ventas.pdf");
    }

    public void exportarVentaPDF(Long id, HttpServletResponse response) throws Exception {
        Optional<Venta> opt = ventaRepository.findById(id);
        if (opt.isEmpty()) {
            response.setContentType("text/plain");
            response.getWriter().write("Venta no encontrada.");
            return;
        }

        Venta venta = opt.get();
        ByteArrayInputStream bis = generarReporteCliente(venta);
        writeStreamToResponse(bis, response, "venta_" + id + ".pdf");
    }

    public void exportarMisVentasPDF(HttpServletResponse response, HttpSession session) throws Exception {
        Empleado empleado = (Empleado) session.getAttribute("empleadoLogueado");
        if (empleado == null) {
            response.setContentType("text/plain");
            response.getWriter().write("Debe iniciar sesión para exportar sus ventas.");
            return;
        }

        List<Venta> ventasEmpleado = ventaRepository.findAll().stream()
                .filter(v -> v.getEmpleado() != null && v.getEmpleado().getId().equals(empleado.getId()))
                .collect(Collectors.toList());

        if (ventasEmpleado.isEmpty()) {
            response.setContentType("text/plain");
            response.getWriter().write("No hay ventas del empleado para exportar.");
            return;
        }

        ByteArrayInputStream bis = generarReporteEmpleado(empleado.getNombre(), ventasEmpleado);
        writeStreamToResponse(bis, response, "mis_ventas_" + empleado.getId() + ".pdf");
    }

    // Helper para escribir el PDF en la respuesta
    private void writeStreamToResponse(ByteArrayInputStream bis, HttpServletResponse response, String filename) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);

        try (OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = bis.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            os.flush();
        } finally {
            bis.close();
        }
    }
}
