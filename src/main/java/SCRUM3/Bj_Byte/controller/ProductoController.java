package SCRUM3.Bj_Byte.controller;

import SCRUM3.Bj_Byte.model.Empleado;
import SCRUM3.Bj_Byte.model.Inventario;
import SCRUM3.Bj_Byte.model.Producto;
import SCRUM3.Bj_Byte.repository.ProductoRepository;
import SCRUM3.Bj_Byte.service.InventarioService;
import SCRUM3.Bj_Byte.service.ProductoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequestMapping("/producto")
public class ProductoController {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private InventarioService inventarioService;

    // Mostrar lista de productos con opción de búsqueda
    @GetMapping
    public String verProductos(@RequestParam(required = false) String filtro, HttpSession session, Model model) {
        Empleado empleado = (Empleado) session.getAttribute("empleadoLogueado");
        if (empleado == null) return "redirect:/empleados/login";

        List<Producto> productos;
        if (filtro != null && !filtro.isEmpty()) {
            productos = productoRepository.findByNombreContainingIgnoreCase(filtro);
        } else {
            productos = productoRepository.findAll();
        }

        model.addAttribute("productos", productos);
        model.addAttribute("esAdmin", empleado.getRolId() == 1); // 1 = Admin
        model.addAttribute("filtro", filtro);
        return "producto";
    }

    // Mostrar formulario para agregar producto
    @GetMapping("/agregar")
    public String mostrarFormularioAgregar(HttpSession session, Model model) {
        Empleado empleado = (Empleado) session.getAttribute("empleadoLogueado");
        if (empleado == null || empleado.getRolId() != 1) {
            return "redirect:/empleados/login";
        }
        model.addAttribute("producto", new Producto());
        return "agregar_producto";
    }

    // Guardar producto nuevo
    @PostMapping("/agregar")
    public String guardarProducto(@ModelAttribute Producto producto, HttpSession session) {
        Empleado empleado = (Empleado) session.getAttribute("empleadoLogueado");
        if (empleado == null || empleado.getRolId() != 1) return "redirect:/empleados/login";
        productoRepository.save(producto);
        return "redirect:/producto";
    }

    // Eliminar producto
    @PostMapping("/eliminar/{id}")
    public String eliminarProducto(@PathVariable Long id, HttpSession session) {
        Empleado empleado = (Empleado) session.getAttribute("empleadoLogueado");
        if (empleado == null || empleado.getRolId() != 1) return "redirect:/empleados/login";
        productoRepository.deleteById(id);
        return "redirect:/producto";
    }

    // Mostrar formulario de modificación
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEditar(@PathVariable Long id, Model model, HttpSession session) {
        Empleado empleado = (Empleado) session.getAttribute("empleadoLogueado");
        if (empleado == null || empleado.getRolId() != 1) return "redirect:/empleados/login";
        Producto producto = productoRepository.findById(id).orElse(null);
        if (producto == null) return "redirect:/producto";
        model.addAttribute("producto", producto);
        return "editar_producto";
    }

    // Guardar cambios del producto modificado
    @PostMapping("/modificar")
    public String modificarProducto(@ModelAttribute Producto producto, HttpSession session) {
        Empleado empleado = (Empleado) session.getAttribute("empleadoLogueado");
        if (empleado == null || empleado.getRolId() != 1) return "redirect:/empleados/login";
        productoRepository.save(producto);
        return "redirect:/producto";
    }

    // Cargar productos desde archivo CSV
    @PostMapping("/cargar-csv")
    public String cargarCSV(@RequestParam("archivo") MultipartFile archivo, HttpSession session) {
        Empleado empleado = (Empleado) session.getAttribute("empleadoLogueado");
        if (empleado == null || empleado.getRolId() != 1) return "redirect:/empleados/login";
        try {
            productoService.cargarProductosDesdeCSV(archivo.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/producto";
    }

    // Mostrar inventario (cantidades y estado)
    @GetMapping("/inventario")
    public String verInventario(Model model) {
        List<Inventario> inventarios = inventarioService.listar();
        List<Producto> productos = productoRepository.findAll();
        model.addAttribute("inventarios", inventarios);
        model.addAttribute("productos", productos); 
        return "inventario";
    }
}
