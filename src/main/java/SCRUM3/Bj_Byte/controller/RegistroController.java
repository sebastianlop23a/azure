package SCRUM3.Bj_Byte.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import SCRUM3.Bj_Byte.model.Empleado;
import SCRUM3.Bj_Byte.repository.EmpleadoRepository;

@Controller
@RequestMapping("/empleados")
public class RegistroController {

    @Autowired
    private EmpleadoRepository empleadoRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Value("${admin.secret}")
    private String claveAdminSecreta;

    @GetMapping("/registro")
    public String mostrarFormularioRegistro(
        @RequestParam(value = "cuentaCreada", required = false) String cuentaCreada,
        Model model
    ) {
        model.addAttribute("empleado", new Empleado());
        model.addAttribute("claveAdminInput", "");
        if ("true".equals(cuentaCreada)) {
            model.addAttribute("cuentaCreada", true);
        }
        return "registro";
    }

    @PostMapping("/registro")
    public String procesarRegistro(@ModelAttribute Empleado empleado,
                                   @RequestParam(name = "claveAdminInput", required = false) String claveAdminIngresada,
                                   Model model) {

        // Validación de clave si el rol es Admin
        if (empleado.getRolId() == 1) {
            if (claveAdminIngresada == null || !claveAdminIngresada.equals(this.claveAdminSecreta)) {
                model.addAttribute("error", "Clave de administrador incorrecta");
                model.addAttribute("empleado", empleado);
                model.addAttribute("claveAdminInput", claveAdminIngresada);
                return "registro";
            }
        }

        // Encriptar la contraseña antes de guardar
        String encriptada = passwordEncoder.encode(empleado.getContrasena());
        empleado.setContrasena(encriptada);

        // Guardar el empleado en la base de datos
        empleadoRepository.save(empleado);

        // Redirigir con parámetro para mostrar alerta
        return "redirect:/empleados/registro?cuentaCreada=true";
    }
}