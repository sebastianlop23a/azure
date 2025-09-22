package SCRUM3.Bj_Byte.controller;

import SCRUM3.Bj_Byte.model.Empleado;
import SCRUM3.Bj_Byte.repository.EmpleadoRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/empleado")
public class EmpleadoController {

    @Autowired
    private EmpleadoRepository empleadoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder; // 👈 inyectamos el PasswordEncoder

    // Mostrar página de login
    @GetMapping("/login")
    public String mostrarLogin() {
        return "login"; // login.html en templates
    }

    // Procesar login
    @PostMapping("/login")
    public String procesarLogin(@RequestParam String correo,
                                @RequestParam String contrasena,
                                HttpSession session,
                                Model model) {
        Optional<Empleado> empleadoOpt = empleadoRepository.findByCorreoAndActivoTrue(correo);

        if (empleadoOpt.isPresent()) {
            Empleado empleado = empleadoOpt.get();
            
            if (passwordEncoder.matches(contrasena, empleado.getContrasena())) {
                session.setAttribute("empleadoLogueado", empleado);
                return "redirect:/home";
            }
        }

        model.addAttribute("error", "Credenciales inválidas o empleado inactivo");
        return "login";
    }

    // Mostrar formulario de registro
    @GetMapping("/registro")
    public String mostrarFormularioRegistro(Model model) {
        model.addAttribute("empleado", new Empleado());
        return "registro";
    }

    // Procesar registro
    @PostMapping("/registro")
    public String registrarEmpleado(@ModelAttribute Empleado empleado) {
        empleado.setActivo(true); // por defecto activo
        if (empleado.getRolId() == null) {
            empleado.setRolId(2); // por defecto empleado normal
        }
        // 👇 encriptamos la contraseña antes de guardar
        empleado.setContrasena(passwordEncoder.encode(empleado.getContrasena()));

        empleadoRepository.save(empleado);
        return "redirect:/empleado/login";
    }

    // Logout
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/empleado/login";
    }

    // Redireccionar la raíz
    @GetMapping("/")
    public String redireccionarRaiz() {
        return "redirect:/empleado/login";
    }

    // Listar empleados (solo activos)
    @GetMapping("/lista")
    public String listarEmpleados(HttpSession session, Model model) {
        Empleado empleadoLogueado = (Empleado) session.getAttribute("empleadoLogueado");
        if (empleadoLogueado == null) {
            return "redirect:/empleado/login";
        }

        List<Empleado> empleados = empleadoRepository.findByActivoTrue();
        model.addAttribute("empleados", empleados);

        Map<Integer, String> roles = new HashMap<>();
        roles.put(1, "Administrador");
        roles.put(2, "Empleado");
        model.addAttribute("roles", roles);

        model.addAttribute("empleadoLogueado", empleadoLogueado);

        return "empleados-lista";
    }

    // Desactivar empleado (solo Admin)
    @PostMapping("/despedir/{id}")
    public String despedirEmpleado(@PathVariable("id") Long id, HttpSession session) {
        Empleado empleadoLogueado = (Empleado) session.getAttribute("empleadoLogueado");

        if (empleadoLogueado == null || empleadoLogueado.getRolId() != 1) {
            return "redirect:/empleado/login";
        }

        Empleado empleado = empleadoRepository.findById(id).orElse(null);
        if (empleado != null) {
            empleado.setActivo(false);
            empleadoRepository.save(empleado);
        }

        return "redirect:/empleado/lista";
    }
}
