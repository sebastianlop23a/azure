package SCRUM3.Bj_Byte.repository;

import SCRUM3.Bj_Byte.model.Inventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;


@Repository
public interface InventarioRepository extends JpaRepository<Inventario, Long> {
    Optional<Inventario> findByProductoIdAndUbicacionAndEstado(Long productoId, String ubicacion, String estado);

    // Productos con bajo stock (menos de 5 unidades)
    List<Inventario> findByCantidadLessThan(int cantidad);
}
