package SCRUM3.Bj_Byte.service;

import SCRUM3.Bj_Byte.model.Inventario;
import SCRUM3.Bj_Byte.repository.InventarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class InventarioService {

    @Autowired
    private InventarioRepository inventarioRepository;

    // Listar todos los inventarios
    public List<Inventario> listar() {
        return inventarioRepository.findAll();
    }

    // Guardar o actualizar inventario
    public void guardar(Inventario inventario) {
        inventarioRepository.save(inventario);
    }

    // Buscar inventario específico por producto, ubicación y estado
    public Optional<Inventario> buscarPorProductoUbicacionEstado(Long productoId, String ubicacion, String estado) {
        return inventarioRepository.findByProductoIdAndUbicacionAndEstado(productoId, ubicacion, estado);
    }
}
