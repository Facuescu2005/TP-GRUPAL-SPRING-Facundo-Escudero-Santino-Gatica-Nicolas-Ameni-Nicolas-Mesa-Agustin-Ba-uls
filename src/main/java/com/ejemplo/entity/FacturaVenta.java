package com.ejemplo.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Cabecera de factura. Relación bidireccional con FacturaVentaDetalle:
 * se usa @Getter/@Setter (no @Data) y se excluye "detalles" de equals/hashCode/toString
 * para evitar recursión infinita (StackOverflowError).
 */
@Entity
@Table(name = "factura_venta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true, exclude = {"detalles", "puntoVenta", "cliente", "condicionIva", "tipoMoneda"})
@ToString(callSuper = true, exclude = {"detalles", "puntoVenta", "cliente", "condicionIva", "tipoMoneda"})
public class FacturaVenta extends AuditoriaApp {

    private Long numero;

    @Column(nullable = false)
    @Temporal(TemporalType.DATE)
    private Date fechaEmision;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "punto_venta_id", nullable = false)
    private PuntoVenta puntoVenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = true)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condicion_iva_id", nullable = false)
    private CondicionIva condicionIva;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_moneda_id", nullable = false)
    private TipoMoneda tipoMoneda;

    private double importeCobrado;

    private double importeSaldo;

    @Column(nullable = false)
    private double importeTotal;

    private String cae;

    @Temporal(TemporalType.DATE)
    private Date caeFechaVencimiento;

    private String resultadoAfip;

    private String motivoRechazo;

    @Column(nullable = false)
    private String estado;

    @Temporal(TemporalType.DATE)
    private Date fechaAnulacion;

    private String observaciones;

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FacturaVentaDetalle> detalles = new ArrayList<>();

    /** Agrega un ítem manteniendo sincronizados ambos lados de la relación. */
    public void agregarDetalle(FacturaVentaDetalle detalle) {
        detalles.add(detalle);
        detalle.setFactura(this);
    }

    public void quitarDetalle(FacturaVentaDetalle detalle) {
        detalles.remove(detalle);
        detalle.setFactura(null);
    }
}
