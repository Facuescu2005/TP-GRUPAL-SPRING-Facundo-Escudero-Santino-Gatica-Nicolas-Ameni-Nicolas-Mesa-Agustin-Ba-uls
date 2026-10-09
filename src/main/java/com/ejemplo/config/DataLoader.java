package com.ejemplo.config;

import com.ejemplo.dto.FacturaReporteDTO;
import com.ejemplo.entity.Articulo;
import com.ejemplo.entity.Cliente;
import com.ejemplo.entity.CondicionIva;
import com.ejemplo.entity.Contacto;
import com.ejemplo.entity.Domicilio;
import com.ejemplo.entity.FacturaVenta;
import com.ejemplo.entity.FacturaVentaDetalle;
import com.ejemplo.entity.ListaPrecio;
import com.ejemplo.entity.ListaPrecioArticulo;
import com.ejemplo.entity.Marca;
import com.ejemplo.entity.PuntoVenta;
import com.ejemplo.entity.Rubro;
import com.ejemplo.entity.TipoMoneda;
import com.ejemplo.entity.Usuario;
import com.ejemplo.service.FacturaService;
import com.ejemplo.service.ReporteExportService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * Al iniciar:
 *  1) Si la base está vacía, carga datos de ejemplo (cada factura se guarda con UN solo em.persist,
 *     y la cascada inserta sus detalles).
 *  2) Ejecuta la consulta JPQL del reporte y genera los archivos PDF y Excel (Parte 2 del TP).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    @PersistenceContext
    private EntityManager em;

    private final TransactionTemplate tx;
    private final FacturaService facturaService;
    private final ReporteExportService reporteExportService;

    @Value("${app.reportes.carpeta:reportes}")
    private String carpetaReportes;

    @Override
    public void run(String... args) throws IOException {
        tx.executeWithoutResult(status -> {
            Long cantidad = em.createQuery("SELECT COUNT(f) FROM FacturaVenta f", Long.class).getSingleResult();
            if (cantidad == 0) {
                cargarDatosDeEjemplo();
                log.info("Datos de ejemplo cargados.");
            }
        });
        generarReportes();
    }

    // --------------------------------------------------------------------------------------------

    private void generarReportes() throws IOException {
        List<FacturaReporteDTO> reporte = facturaService.obtenerReporte();
        Path carpeta = Paths.get(carpetaReportes);
        Files.createDirectories(carpeta);
        Path pdf = carpeta.resolve("reporte_facturas.pdf");
        Path xlsx = carpeta.resolve("reporte_facturas.xlsx");
        Files.write(pdf, reporteExportService.generarPdf(reporte));
        Files.write(xlsx, reporteExportService.generarExcel(reporte));
        log.info("Reportes generados: {} y {} ({} facturas)",
                pdf.toAbsolutePath(), xlsx.toAbsolutePath(), reporte.size());
    }

    private void cargarDatosDeEjemplo() {
        Date hoy = new Date();

        Usuario admin = Usuario.builder()
                .usuario("admin").clave("admin123").nombre("Ana").apellido("Gómez").build();
        em.persist(admin);

        CondicionIva respInscripto = CondicionIva.builder().codigoAfip(1).denominacion("IVA Responsable Inscripto")
                .fechaAlta(hoy).fechaModificacion(hoy).usuarioCarga(admin).usuarioModificacion(admin).build();
        CondicionIva consFinal = CondicionIva.builder().codigoAfip(5).denominacion("Consumidor Final")
                .fechaAlta(hoy).fechaModificacion(hoy).usuarioCarga(admin).usuarioModificacion(admin).build();
        CondicionIva monotributo = CondicionIva.builder().codigoAfip(6).denominacion("Responsable Monotributo")
                .fechaAlta(hoy).fechaModificacion(hoy).usuarioCarga(admin).usuarioModificacion(admin).build();
        em.persist(respInscripto);
        em.persist(consFinal);
        em.persist(monotributo);

        TipoMoneda pesos = TipoMoneda.builder().codigoAfip("PES").denominacion("Peso argentino").simbolo("$")
                .fechaAlta(hoy).fechaModificacion(hoy).usuarioCarga(admin).usuarioModificacion(admin).build();
        em.persist(pesos);

        PuntoVenta central = PuntoVenta.builder().numero(1).descripcion("Sucursal Central")
                .tipoEmision("Electrónica").domicilioComercial("San Martín 1200, Mendoza")
                .fechaAlta(hoy).fechaModificacion(hoy).usuarioCarga(admin).usuarioModificacion(admin).build();
        PuntoVenta godoyCruz = PuntoVenta.builder().numero(2).descripcion("Sucursal Godoy Cruz")
                .tipoEmision("Electrónica").domicilioComercial("Perito Moreno 450, Godoy Cruz")
                .fechaAlta(hoy).fechaModificacion(hoy).usuarioCarga(admin).usuarioModificacion(admin).build();
        em.persist(central);
        em.persist(godoyCruz);

        Cliente empresaTech = Cliente.builder().cuitCuil("30-71234567-8").denominacion("Empresa Tech S.A.")
                .contacto(Contacto.builder().email("compras@empresatech.com").telefono("261-4200000").build())
                .domicilio(Domicilio.builder().nombreCalle("Av. Colón").numeroCalle("850").build())
                .fechaAlta(hoy).fechaModificacion(hoy).usuarioCarga(admin).usuarioModificacion(admin).build();
        Cliente juanPerez = Cliente.builder().cuitCuil("20-30111222-3").denominacion("Juan Pérez")
                .contacto(Contacto.builder().email("juanperez@mail.com").celular("261-5550001").build())
                .domicilio(Domicilio.builder().nombreCalle("Belgrano").numeroCalle("321").build())
                .fechaAlta(hoy).fechaModificacion(hoy).usuarioCarga(admin).usuarioModificacion(admin).build();
        em.persist(empresaTech); // contacto y domicilio se insertan por cascada
        em.persist(juanPerez);

        Rubro informatica = Rubro.builder().codigo(10).denominacion("Informática")
                .fechaAlta(hoy).fechaModificacion(hoy).usuarioCarga(admin).usuarioModificacion(admin).build();
        Marca generica = Marca.builder().codigo(100).denominacion("Genérica")
                .fechaAlta(hoy).fechaModificacion(hoy).usuarioCarga(admin).usuarioModificacion(admin).build();
        em.persist(informatica);
        em.persist(generica);

        ListaPrecio minorista = ListaPrecio.builder().codigo("MIN").denominacion("Lista minorista")
                .fechaAlta(hoy).fechaModificacion(hoy).usuarioCarga(admin).usuarioModificacion(admin).build();
        em.persist(minorista);

        ListaPrecioArticulo mouse = nuevoArticulo("A001", "Mouse inalámbrico", 500, informatica, generica, minorista, admin, hoy);
        ListaPrecioArticulo teclado = nuevoArticulo("A002", "Teclado mecánico", 1000, informatica, generica, minorista, admin, hoy);
        ListaPrecioArticulo monitor = nuevoArticulo("A003", "Monitor 24\"", 25000, informatica, generica, minorista, admin, hoy);
        ListaPrecioArticulo notebook = nuevoArticulo("A004", "Notebook 15\"", 30000.5, informatica, generica, minorista, admin, hoy);

        // --- Facturas: un único em.persist por factura; los detalles entran por cascada ---
        persistirFactura(1001L, "2026-03-15", null, consFinal, central, pesos, "EMITIDA", admin, hoy,
                new Object[][]{{mouse, 1.0}, {teclado, 1.0}});

        persistirFactura(1002L, "2026-03-20", empresaTech, respInscripto, central, pesos, "EMITIDA", admin, hoy,
                new Object[][]{{monitor, 1.0}, {notebook, 1.0}, {mouse, 20.0}, {teclado, 19.0}, {mouse, 2.0}});

        persistirFactura(1003L, "2026-04-02", juanPerez, monotributo, godoyCruz, pesos, "EMITIDA", admin, hoy,
                new Object[][]{{teclado, 2.0}, {mouse, 2.0}, {monitor, 1.0}});

        persistirFactura(1004L, "2026-04-10", null, consFinal, godoyCruz, pesos, "ANULADA", admin, hoy,
                new Object[][]{{mouse, 3.0}});

        persistirFactura(1005L, "2026-05-05", empresaTech, respInscripto, central, pesos, "EMITIDA", admin, hoy,
                new Object[][]{{notebook, 4.0}, {monitor, 4.0}, {teclado, 4.0}, {mouse, 4.0}});
    }

    private ListaPrecioArticulo nuevoArticulo(String codigo, String denominacion, double precio, Rubro rubro,
                                              Marca marca, ListaPrecio lista, Usuario u, Date hoy) {
        Articulo art = Articulo.builder().codigo(codigo).denominacion(denominacion).rubro(rubro).marca(marca)
                .fechaAlta(hoy).fechaModificacion(hoy).usuarioCarga(u).usuarioModificacion(u).build();
        em.persist(art);
        ListaPrecioArticulo lpa = ListaPrecioArticulo.builder().listaPrecio(lista).articulo(art).precioVenta(precio)
                .fechaAlta(hoy).fechaModificacion(hoy).usuarioCarga(u).usuarioModificacion(u).build();
        em.persist(lpa);
        return lpa;
    }

    private void persistirFactura(Long numero, String fecha, Cliente cliente, CondicionIva condicion,
                                  PuntoVenta pv, TipoMoneda moneda, String estado, Usuario u, Date hoy,
                                  Object[][] items) {
        FacturaVenta factura = FacturaVenta.builder()
                .numero(numero)
                .fechaEmision(fecha(fecha))
                .cliente(cliente)
                .condicionIva(condicion)
                .puntoVenta(pv)
                .tipoMoneda(moneda)
                .estado(estado)
                .fechaAnulacion("ANULADA".equals(estado) ? fecha(fecha) : null)
                .fechaAlta(hoy).fechaModificacion(hoy).usuarioCarga(u).usuarioModificacion(u)
                .build();

        double total = 0;
        for (Object[] item : items) {
            ListaPrecioArticulo lpa = (ListaPrecioArticulo) item[0];
            double cantidad = (Double) item[1];
            double subtotal = lpa.getPrecioVenta() * cantidad;
            double neto = Math.round(subtotal / 1.21 * 100) / 100.0;
            FacturaVentaDetalle detalle = FacturaVentaDetalle.builder()
                    .listaPrecioArticulo(lpa)
                    .descripcion(lpa.getArticulo().getDenominacion())
                    .cantidad(cantidad)
                    .precioUnitario(lpa.getPrecioVenta())
                    .importeNeto(neto)
                    .importeIva(Math.round((subtotal - neto) * 100) / 100.0)
                    .importeSubtotal(subtotal)
                    .build();
            factura.agregarDetalle(detalle); // vincula ambos lados de la relación
            total += subtotal;
        }
        factura.setImporteTotal(total);
        factura.setImporteCobrado("ANULADA".equals(estado) ? 0 : total);

        em.persist(factura); // CascadeType.ALL inserta también todos los detalles
    }

    private static Date fecha(String iso) {
        return Date.from(LocalDate.parse(iso).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
