package com.ejemplo.controller;

import com.ejemplo.dto.FacturaReporteDTO;
import com.ejemplo.service.FacturaService;
import com.ejemplo.service.ReporteExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/facturas")
@RequiredArgsConstructor
public class FacturaRestController {

    private final FacturaService facturaService;
    private final ReporteExportService reporteExportService;

    /**
     * GET /api/facturas?fechaDesde=2026-03-01&fechaHasta=2026-03-31&estado=EMITIDA&montoMinimo=1000
     * Todos los parámetros son opcionales.
     */
    @GetMapping
    public List<FacturaReporteDTO> listar(
            @RequestParam(name = "fechaDesde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fechaDesde,
            @RequestParam(name = "fechaHasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fechaHasta,
            @RequestParam(name = "estado", required = false) String estado,
            @RequestParam(name = "montoMinimo", required = false) Double montoMinimo) {
        return facturaService.buscarFacturasFiltradas(fechaDesde, fechaHasta, estado, montoMinimo);
    }

    /** Descarga el reporte en PDF (acepta los mismos filtros opcionales). */
    @GetMapping("/reporte/pdf")
    public ResponseEntity<byte[]> reportePdf(
            @RequestParam(name = "fechaDesde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fechaDesde,
            @RequestParam(name = "fechaHasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fechaHasta,
            @RequestParam(name = "estado", required = false) String estado,
            @RequestParam(name = "montoMinimo", required = false) Double montoMinimo) {
        byte[] pdf = reporteExportService.generarPdf(
                facturaService.buscarFacturasFiltradas(fechaDesde, fechaHasta, estado, montoMinimo));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_facturas.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    /** Descarga el reporte en Excel .xlsx (acepta los mismos filtros opcionales). */
    @GetMapping("/reporte/excel")
    public ResponseEntity<byte[]> reporteExcel(
            @RequestParam(name = "fechaDesde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fechaDesde,
            @RequestParam(name = "fechaHasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date fechaHasta,
            @RequestParam(name = "estado", required = false) String estado,
            @RequestParam(name = "montoMinimo", required = false) Double montoMinimo) {
        byte[] xlsx = reporteExportService.generarExcel(
                facturaService.buscarFacturasFiltradas(fechaDesde, fechaHasta, estado, montoMinimo));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_facturas.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(xlsx);
    }
}
