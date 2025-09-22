package SCRUM3.Bj_Byte.util;

import SCRUM3.Bj_Byte.model.Venta;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.RoundingMode;
import java.util.List;

public class ExportarExcelVentas {

    private final List<Venta> listaVentas;
    private final XSSFWorkbook workbook;
    private final Sheet sheet;

    public ExportarExcelVentas(List<Venta> listaVentas) {
        this.listaVentas = listaVentas;
        workbook = new XSSFWorkbook();
        sheet = workbook.createSheet("Ventas");
    }

    private void escribirCabecera() {
        Row fila = sheet.createRow(0);
        CellStyle estilo = workbook.createCellStyle();
        Font fuente = workbook.createFont();
        fuente.setBold(true);
        estilo.setFont(fuente);

        String[] encabezados = { "ID", "Empleado", "Producto", "Cantidad", "Total", "Fecha" };

        for (int i = 0; i < encabezados.length; i++) {
            Cell celda = fila.createCell(i);
            celda.setCellValue(encabezados[i]);
            celda.setCellStyle(estilo);
        }
    }

    private void escribirDatos() {
        int filaNum = 1;

        for (Venta venta : listaVentas) {
            Row fila = sheet.createRow(filaNum++);

            fila.createCell(0).setCellValue(venta.getId());
            fila.createCell(1).setCellValue(venta.getNombreEmpleado());
            fila.createCell(2).setCellValue(venta.getNombreProducto());
            fila.createCell(3).setCellValue(venta.getCantidad());

            // Formatear totalVenta
            double totalRedondeado = venta.getTotalVenta()
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
            fila.createCell(4).setCellValue(totalRedondeado);

            fila.createCell(5).setCellValue(venta.getFecha().toString());
        }

        // Autoajustar tamaño de columnas
        for (int i = 0; i < 6; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    public void exportar(HttpServletResponse response) throws IOException {
        escribirCabecera();
        escribirDatos();

        try (ServletOutputStream out = response.getOutputStream()) {
            workbook.write(out);
            workbook.close();
        }
    }
}
