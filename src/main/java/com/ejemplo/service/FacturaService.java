package com.ejemplo.service;

import com.ejemplo.dto.FacturaReporteDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Consultas del reporte de facturas usando proyección a DTO con JPQL.
 */
@Service
@Transactional(readOnly = true)
public class FacturaService {

    @PersistenceContext
    private EntityManager em;

    private static final String SELECT_DTO =
            "SELECT new com.ejemplo.dto.FacturaReporteDTO(" +
            "  f.numero, " +
            "  f.fechaEmision, " +
            "  COALESCE(c.denominacion, 'Consumidor Final'), " +
            "  ci.denominacion, " +
            "  pv.descripcion, " +
            "  f.importeTotal, " +
            "  COUNT(d) " +
            ") " +
            "FROM FacturaVenta f " +
            "LEFT JOIN f.cliente c " +
            "JOIN f.condicionIva ci " +
            "JOIN f.puntoVenta pv " +
            "JOIN f.detalles d ";

    private static final String GROUP_BY =
            "GROUP BY f.id, f.numero, f.fechaEmision, c.denominacion, " +
            "ci.denominacion, pv.descripcion, f.importeTotal " +
            "ORDER BY f.numero";

    /** Parte 2: reporte completo (sin filtros), usado para generar el PDF y el Excel. */
    public List<FacturaReporteDTO> obtenerReporte() {
        String jpql = SELECT_DTO + GROUP_BY;
        return em.createQuery(jpql, FacturaReporteDTO.class).getResultList();
    }

    /** Parte 3: búsqueda dinámica; cada filtro se agrega solo si viene informado. */
    public List<FacturaReporteDTO> buscarFacturasFiltradas(Date fechaDesde, Date fechaHasta,
                                                           String estado, Double montoMinimo) {
        StringBuilder jpql = new StringBuilder(SELECT_DTO).append("WHERE 1=1 ");
        Map<String, Object> params = new HashMap<>();

        if (fechaDesde != null) {
            jpql.append("AND f.fechaEmision >= :fechaDesde ");
            params.put("fechaDesde", fechaDesde);
        }
        if (fechaHasta != null) {
            jpql.append("AND f.fechaEmision <= :fechaHasta ");
            params.put("fechaHasta", fechaHasta);
        }
        if (estado != null && !estado.trim().isEmpty()) {
            jpql.append("AND f.estado = :estado ");
            params.put("estado", estado.trim().toUpperCase());
        }
        if (montoMinimo != null) {
            jpql.append("AND f.importeTotal >= :montoMinimo ");
            params.put("montoMinimo", montoMinimo);
        }

        jpql.append(GROUP_BY);

        TypedQuery<FacturaReporteDTO> query = em.createQuery(jpql.toString(), FacturaReporteDTO.class);
        params.forEach(query::setParameter);
        return query.getResultList();
    }
}
