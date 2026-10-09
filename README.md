# TP – Lombok, DTOs y Servicios Web REST con Spring Boot

Proyecto Spring Boot 3.5 (Java 17+) sobre el modelo de facturación del TP de ORM/JPA.

## Estructura

```
src/main/java/com/ejemplo
├── FacturacionApplication.java      → clase main de Spring Boot
├── entity/                          → Parte 1: entidades JPA con Lombok
│   ├── EntityId, AuditoriaApp       (superclases @MappedSuperclass)
│   └── FacturaVenta, FacturaVentaDetalle, Cliente, Articulo, ... (14 entidades)
├── dto/FacturaReporteDTO.java       → Parte 2: DTO del reporte
├── service/
│   ├── FacturaService.java          → Parte 2 y 3: JPQL "SELECT new ..." + búsqueda con filtros
│   └── ReporteExportService.java    → Parte 2: genera PDF (OpenPDF) y Excel .xlsx (Apache POI)
├── controller/FacturaRestController → Parte 3: API REST en /api/facturas
└── config/DataLoader.java           → carga datos de ejemplo y genera los reportes al iniciar
```

## Cómo ejecutarlo

**Opción A – sin instalar nada (base H2 en memoria):**

```
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

En IntelliJ: Run configuration → *Active profiles* = `h2`.

**Opción B – con MySQL (configuración por defecto):**

1. Revisar usuario y clave en `src/main/resources/application.properties`.
2. `mvn spring-boot:run` (la base `facturacion` se crea sola).

Al arrancar se cargan 5 facturas de ejemplo (solo si la base está vacía) y se generan
`reportes/reporte_facturas.pdf` y `reportes/reporte_facturas.xlsx` en la carpeta del proyecto.

## Endpoints

| Método | URL | Descripción |
|---|---|---|
| GET | `/api/facturas` | Lista de `FacturaReporteDTO` en JSON |
| GET | `/api/facturas/reporte/pdf` | Descarga el reporte en PDF |
| GET | `/api/facturas/reporte/excel` | Descarga el reporte en Excel |

Los tres aceptan los filtros opcionales `fechaDesde`, `fechaHasta` (formato `yyyy-MM-dd`),
`estado` y `montoMinimo`.

Ejemplos para Postman o el navegador:

```
http://localhost:8080/api/facturas
http://localhost:8080/api/facturas?estado=EMITIDA
http://localhost:8080/api/facturas?fechaDesde=2026-03-01&fechaHasta=2026-03-31
http://localhost:8080/api/facturas?montoMinimo=50000
http://localhost:8080/api/facturas?estado=ANULADA
```

## Decisiones tomadas

- **`jakarta.persistence` en vez de `javax.persistence`:** Spring Boot 3 usa Jakarta EE,
  así que los imports del enunciado (`javax.persistence.*`) cambian a `jakarta.persistence.*`.
- **`@SuperBuilder` en vez de `@Builder`:** `@Builder` no incluye los campos heredados
  (`id`, `fechaAlta`, `usuarioCarga`, ...). `@SuperBuilder` en toda la jerarquía permite
  construir una `FacturaVenta` completa, auditoría incluida, con un solo builder.
- **Recursión infinita:** se usa `@Getter`/`@Setter` (nunca `@Data`) y se excluyen las
  relaciones de `@ToString` y `@EqualsAndHashCode` (`detalles` en `FacturaVenta`, `factura`
  en `FacturaVentaDetalle`, etc.). Todas las subclases usan `callSuper = true`.
- **Relación bidireccional:** `FacturaVenta.agregarDetalle(...)` vincula ambos lados, y cada
  factura se guarda con un único `em.persist(factura)`; la cascada inserta los detalles.
- **Excel:** se generó un `.xlsx` real con Apache POI (con formato de fecha, importes y una
  fila de total con fórmula) en lugar del `.txt` con tabulaciones.
