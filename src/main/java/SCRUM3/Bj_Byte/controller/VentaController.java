package SCRUM3.Bj_Byte.controller;

import SCRUM3.Bj_Byte.model.Empleado;
import SCRUM3.Bj_Byte.model.Inventario;
import SCRUM3.Bj_Byte.model.Producto;
import SCRUM3.Bj_Byte.model.Venta;
import SCRUM3.Bj_Byte.repository.InventarioRepository;
import SCRUM3.Bj_Byte.repository.VentaRepository;
import SCRUM3.Bj_Byte.repository.EmpleadoRepository;
import SCRUM3.Bj_Byte.service.PdfService;
import SCRUM3.Bj_Byte.service.ExchangeRateService;
import SCRUM3.Bj_Byte.util.ExportarExcelVentas;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/ventas")
public class VentaController {

    @Autowired
    private InventarioRepository inventarioRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private EmpleadoRepository empleadoRepository;

    @Autowired
    private PdfService pdfService;

    @Autowired
    private ExchangeRateService exchangeRateService;

    private Empleado getEmpleadoLogueado(HttpSession session) {
        return (Empleado) session.getAttribute("empleadoLogueado");
    }

    // 👉 FORMATOS
    private static final DecimalFormat COP_FORMAT;
    private static final DecimalFormat DECIMAL_FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("es", "CO"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');

        COP_FORMAT = new DecimalFormat("#,##0.00", symbols);
        DECIMAL_FORMAT = new DecimalFormat("#0.00");
    }

    // ---------------------- FORMULARIOS ----------------------

    @GetMapping("/registrar")
    public String mostrarFormularioVenta(Model model) {
        model.addAttribute("inventarios", inventarioRepository.findAll());
        return "ventas/registrar_venta";
    }

    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Long id, HttpSession session, Model model) {
        if (getEmpleadoLogueado(session) == null) return "redirect:/empleados/login";

        Optional<Venta> ventaOpt = ventaRepository.findById(id);
        if (ventaOpt.isEmpty()) return "redirect:/ventas/lista";

        model.addAttribute("venta", ventaOpt.get());
        model.addAttribute("inventarios", inventarioRepository.findAll());
        return "ventas/editar_venta";
    }

    // ---------------------- REGISTRAR VENTA ----------------------

    @PostMapping("/registrar")
    public String procesarVenta(@RequestParam Long inventarioId,
                                @RequestParam int cantidad,
                                @RequestParam String cliente,
                                @RequestParam String metodoPago,
                                @RequestParam(required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaVenta,
                                HttpSession session,
                                Model model) {

        Empleado empleado = getEmpleadoLogueado(session);
        if (empleado == null) return "redirect:/empleados/login";

        Optional<Inventario> optInventario = inventarioRepository.findById(inventarioId);
        if (optInventario.isEmpty() || cantidad <= 0) {
            model.addAttribute("error", "Inventario no válido o cantidad incorrecta.");
            model.addAttribute("inventarios", inventarioRepository.findAll());
            return "ventas/registrar_venta";
        }

        Inventario inventario = optInventario.get();

        if (inventario.getCantidad() < cantidad) {
            model.addAttribute("error", "No hay suficiente stock en inventario.");
            model.addAttribute("inventarios", inventarioRepository.findAll());
            return "ventas/registrar_venta";
        }

        Producto producto = inventario.getProducto();
        if (producto == null) {
            model.addAttribute("error", "El inventario seleccionado no tiene producto asociado.");
            model.addAttribute("inventarios", inventarioRepository.findAll());
            return "ventas/registrar_venta";
        }

        // Actualizar inventario
        inventario.setCantidad(inventario.getCantidad() - cantidad);
        inventarioRepository.save(inventario);

        BigDecimal total = producto.getPrecio().multiply(BigDecimal.valueOf(cantidad));

        // Crear venta
        Venta venta = new Venta();
        venta.setInventario(inventario);
        venta.setEmpleado(empleado);
        venta.setCantidad(cantidad);
        venta.setFecha(fechaVenta != null ? fechaVenta : LocalDateTime.now());
        venta.setNombreProducto(producto.getNombre());
        venta.setNombreEmpleado(empleado.getNombre());
        venta.setTotalVenta(total);
        venta.setCliente(cliente);
        venta.setMetodoPago(metodoPago);

        ventaRepository.save(venta);
        return "redirect:/ventas/mis-ventas";
    }

    // ---------------------- EDITAR VENTA ----------------------

    @PostMapping("/editar/{id}")
    public String procesarEdicionVenta(@PathVariable Long id,
                                       @RequestParam Long inventarioId,
                                       @RequestParam int cantidad,
                                       @RequestParam String cliente,
                                       @RequestParam String metodoPago,
                                       HttpSession session,
                                       Model model) {
        if (getEmpleadoLogueado(session) == null) return "redirect:/empleados/login";

        Optional<Venta> ventaOpt = ventaRepository.findById(id);
        Optional<Inventario> inventarioOpt = inventarioRepository.findById(inventarioId);

        if (ventaOpt.isEmpty() || inventarioOpt.isEmpty() || cantidad <= 0) {
            return "redirect:/ventas/lista";
        }

        Venta venta = ventaOpt.get();
        Inventario nuevoInventario = inventarioOpt.get();
        Inventario inventarioAnterior = venta.getInventario();

        // Devolver stock del inventario anterior
        if (inventarioAnterior != null) {
            inventarioAnterior.setCantidad(inventarioAnterior.getCantidad() + venta.getCantidad());
            inventarioRepository.save(inventarioAnterior);
        }

        if (nuevoInventario.getCantidad() < cantidad) {
            model.addAttribute("error", "No hay suficiente stock en inventario.");
            model.addAttribute("venta", venta);
            model.addAttribute("inventarios", inventarioRepository.findAll());
            return "ventas/editar_venta";
        }

        Producto producto = nuevoInventario.getProducto();
        if (producto == null) {
            model.addAttribute("error", "El inventario seleccionado no tiene producto asociado.");
            model.addAttribute("venta", venta);
            model.addAttribute("inventarios", inventarioRepository.findAll());
            return "ventas/editar_venta";
        }

        nuevoInventario.setCantidad(nuevoInventario.getCantidad() - cantidad);
        inventarioRepository.save(nuevoInventario);

        venta.setInventario(nuevoInventario);
        venta.setCantidad(cantidad);
        venta.setNombreProducto(producto.getNombre());
        venta.setTotalVenta(producto.getPrecio().multiply(BigDecimal.valueOf(cantidad)));
        venta.setCliente(cliente);
        venta.setMetodoPago(metodoPago);

        ventaRepository.save(venta);
        return "redirect:/ventas/lista";
    }

    // ---------------------- ELIMINAR VENTA ----------------------

    @GetMapping("/eliminar/{id}")
    public String eliminarVenta(@PathVariable Long id, HttpSession session) {
        if (getEmpleadoLogueado(session) == null) return "redirect:/empleados/login";

        Optional<Venta> ventaOpt = ventaRepository.findById(id);
        if (ventaOpt.isPresent()) {
            Venta venta = ventaOpt.get();
            Inventario inventario = venta.getInventario();
            if (inventario != null) {
                inventario.setCantidad(inventario.getCantidad() + venta.getCantidad());
                inventarioRepository.save(inventario);
            }
            ventaRepository.deleteById(id);
        }
        return "redirect:/ventas/lista";
    }

    // ---------------------- LISTADOS ----------------------

    @GetMapping("/lista")
    public String listarVentas(HttpSession session, Model model) {
        Empleado empleado = getEmpleadoLogueado(session);
        if (empleado == null) return "redirect:/empleados/login";

        if (empleado.getRolId() == null || empleado.getRolId().intValue() != 1) {
            return "redirect:/ventas/mis-ventas";
        }

        List<Venta> ventas = ventaRepository.findAll();

        model.addAttribute("ventas", ventas);
        model.addAttribute("productos", ventas.stream()
                .map(v -> v.getInventario() != null && v.getInventario().getProducto() != null ? v.getInventario().getProducto().getNombre() : "N/A")
                .distinct().collect(Collectors.toList()));
        model.addAttribute("empleados", ventas.stream()
                .map(v -> v.getEmpleado() != null ? v.getEmpleado().getNombre() : "N/A")
                .distinct().collect(Collectors.toList()));
        model.addAttribute("fechas", ventas.stream()
                .map(v -> v.getFecha() != null ? v.getFecha().toLocalDate().toString() : "N/A")
                .distinct().collect(Collectors.toList()));

        return "ventas/listar_ventas";
    }

    @GetMapping("/mis-ventas")
    public String misVentas(HttpSession session, Model model) {
        Empleado empleado = getEmpleadoLogueado(session);
        if (empleado == null) return "redirect:/empleados/login";

        List<Venta> ventasEmpleado = ventaRepository.findByEmpleadoId(empleado.getId());
        model.addAttribute("ventas", ventasEmpleado);
        return "ventas/ventas_empleado";
    }

    // ---------------------- RESUMEN VENTAS ----------------------

    @GetMapping("/resumen")
    public String resumenVentas(HttpSession session, Model model) {
        Empleado empleado = getEmpleadoLogueado(session);
        if (empleado == null) return "redirect:/empleados/login";

        Long empleadoId = empleado.getId();

        BigDecimal totalHistorico = Optional.ofNullable(ventaRepository.obtenerTotalVendidoPorEmpleado(empleadoId))
                .orElse(BigDecimal.ZERO);

        BigDecimal totalHoy = Optional.ofNullable(
                ventaRepository.obtenerTotalVendidoPorEmpleadoEntreFechas(
                        empleadoId, LocalDate.now().atStartOfDay(), LocalDateTime.now()
                )).orElse(BigDecimal.ZERO);

        // Conversiones
        BigDecimal totalHoyUSD = exchangeRateService.convertFromCOP(totalHoy, "USD");
        BigDecimal totalHoyEUR = exchangeRateService.convertFromCOP(totalHoy, "EUR");
        BigDecimal totalHistoricoUSD = exchangeRateService.convertFromCOP(totalHistorico, "USD");
        BigDecimal totalHistoricoEUR = exchangeRateService.convertFromCOP(totalHistorico, "EUR");

        // Ventas por día de la semana
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(java.time.DayOfWeek.MONDAY);
        BigDecimal[] ventasPorDia = new BigDecimal[7];
        for (int i = 0; i < 7; i++) {
            LocalDate dia = monday.plusDays(i);
            BigDecimal totalDia = ventaRepository.obtenerTotalVendidoPorEmpleadoEntreFechas(
                    empleadoId,
                    dia.atStartOfDay(),
                    dia.atTime(23, 59, 59)
            );
            ventasPorDia[i] = Optional.ofNullable(totalDia).orElse(BigDecimal.ZERO);
        }

        model.addAttribute("ventasPorDia", ventasPorDia);

        // Totales
        model.addAttribute("empleado", empleado);
        model.addAttribute("totalHistorico", totalHistorico);
        model.addAttribute("totalHoy", totalHoy);
        model.addAttribute("totalHoyUSD", totalHoyUSD);
        model.addAttribute("totalHoyEUR", totalHoyEUR);
        model.addAttribute("totalHistoricoUSD", totalHistoricoUSD);
        model.addAttribute("totalHistoricoEUR", totalHistoricoEUR);

        // Última actualización de la API
        model.addAttribute("ultimaActualizacion", exchangeRateService.getUltimaActualizacion());

        return "ventas/resumen_ventas";
    }

    // ---------------------- EXPORTACIONES ----------------------

    @GetMapping("/exportar")
    public void exportarVentas(HttpServletResponse response) throws IOException {
        try {
            List<Venta> listaVentas = ventaRepository.findAll();
            if (listaVentas.isEmpty()) {
                response.setContentType("text/plain");
                response.getWriter().write("No hay ventas para exportar.");
                return;
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=ventas.xlsx");

            ExportarExcelVentas exportador = new ExportarExcelVentas(listaVentas);
            exportador.exportar(response);
        } catch (Exception e) {
            response.reset();
            response.setContentType("text/plain");
            response.getWriter().write("Error al exportar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @GetMapping("/exportar-pdf")
    public void exportarVentasPDF(HttpServletResponse response, HttpSession session) throws IOException {
        try {
            Empleado empleado = getEmpleadoLogueado(session);
            if (empleado == null || empleado.getRolId() == null || empleado.getRolId() != 1) {
                response.setContentType("text/plain");
                response.getWriter().write("Solo los administradores pueden exportar todas las ventas.");
                return;
            }
            pdfService.exportarVentasPDF(response);
        } catch (Exception e) {
            response.reset();
            response.setContentType("text/plain");
            response.getWriter().write("Error al exportar PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @GetMapping("/exportar-pdf/{id}")
    public void exportarVentaPDF(@PathVariable Long id, HttpServletResponse response) throws IOException {
        try {
            pdfService.exportarVentaPDF(id, response);
        } catch (Exception e) {
            response.reset();
            response.setContentType("text/plain");
            response.getWriter().write("Error al exportar PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @GetMapping("/exportar-mis-ventas-pdf")
    public void exportarMisVentasPDF(HttpServletResponse response, HttpSession session) throws IOException {
        try {
            pdfService.exportarMisVentasPDF(response, session);
        } catch (Exception e) {
            response.reset();
            response.setContentType("text/plain");
            response.getWriter().write("Error al exportar PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
