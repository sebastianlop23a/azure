package SCRUM3.Bj_Byte.repository;

import SCRUM3.Bj_Byte.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {

    // Ventas realizadas por un empleado específico
    List<Venta> findByEmpleadoId(Long empleadoId);

    // Ventas realizadas entre dos fechas
    List<Venta> findByFechaBetween(LocalDateTime inicio, LocalDateTime fin);

    // Total vendido históricamente por un empleado
    @Query("SELECT COALESCE(SUM(v.totalVenta), 0) FROM Venta v WHERE v.empleado.id = :empleadoId")
    BigDecimal obtenerTotalVendidoPorEmpleado(@Param("empleadoId") Long empleadoId);

    // Total vendido por un empleado en un rango de fechas específico (semi-abierto)
    @Query("SELECT COALESCE(SUM(v.totalVenta), 0) " +
           "FROM Venta v " +
           "WHERE v.empleado.id = :empleadoId " +
           "AND v.fecha >= :inicio AND v.fecha < :fin")
    BigDecimal obtenerTotalVendidoPorEmpleadoEntreFechas(@Param("empleadoId") Long empleadoId,
                                                         @Param("inicio") LocalDateTime inicio,
                                                         @Param("fin") LocalDateTime fin);

    // 🔥 Total ventas en un rango (semi-abierto, para home)
    @Query("SELECT COALESCE(SUM(v.totalVenta), 0) FROM Venta v " +
           "WHERE v.fecha >= :inicio AND v.fecha < :fin")
    BigDecimal totalVentasEnRango(@Param("inicio") LocalDateTime inicio,
                                  @Param("fin") LocalDateTime fin);

    // Total ventas históricas
    @Query("SELECT COALESCE(SUM(v.totalVenta), 0) FROM Venta v")
    BigDecimal totalVentasHistoricas();

    // Total vendido por cada empleado (nombre, total)
    @Query("SELECT v.empleado.nombre, COALESCE(SUM(v.totalVenta), 0) " +
           "FROM Venta v GROUP BY v.empleado.nombre")
    List<Object[]> obtenerTotalVendidoPorTodosLosEmpleados();
}
