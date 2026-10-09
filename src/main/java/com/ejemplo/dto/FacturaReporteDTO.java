package com.ejemplo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

/**
 * DTO de solo lectura para el reporte ejecutivo de ventas.
 * Se llena directamente desde JPQL con "SELECT new com.ejemplo.dto.FacturaReporteDTO(...)",
 * sin cargar las entidades completas en memoria.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FacturaReporteDTO {

    private Long numeroFactura;
    private Date fechaEmision;
    private String clienteDenominacion;
    private String condicionIva;
    private String puntoVentaDescripcion;
    private double importeTotal;
    private long cantidadItems;
}
