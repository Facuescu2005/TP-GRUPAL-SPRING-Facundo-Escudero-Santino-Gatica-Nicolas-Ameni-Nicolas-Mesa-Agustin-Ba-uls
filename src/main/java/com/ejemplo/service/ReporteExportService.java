package com.ejemplo.service;

import com.ejemplo.dto.FacturaReporteDTO;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Genera el reporte de facturas en PDF (OpenPDF) y en Excel .xlsx (Apache POI).
 */
@Service
public class ReporteExportService {

    private static final String[] COLUMNAS = {
            "N° Factura", "Fecha emisión", "Cliente", "Condición IVA",
            "Punto de venta", "Importe total", "Cant. ítems"
    };

    private final SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy");
    private final NumberFormat formatoMoneda = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR"));

    // ------------------------------------------------------------------ PDF

    public byte[] generarPdf(List<FacturaReporteDTO> facturas) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 30, 30, 30, 30);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Font titulo = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font subtitulo = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
            Font encabezado = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
            Font celda = new Font(Font.HELVETICA, 9);
            Font totalFont = new Font(Font.HELVETICA, 10, Font.BOLD);

            doc.add(new Paragraph("Reporte ejecutivo de ventas", titulo));
            doc.add(new Paragraph("Generado el " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date())
                    + "  ·  " + facturas.size() + " facturas", subtitulo));
            doc.add(new Paragraph(" "));

            PdfPTable tabla = new PdfPTable(COLUMNAS.length);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[]{1.2f, 1.3f, 2.6f, 2.4f, 2f, 1.6f, 1f});
            tabla.setHeaderRows(1);

            Color azul = new Color(31, 78, 121);
            for (String col : COLUMNAS) {
                PdfPCell c = new PdfPCell(new Phrase(col, encabezado));
                c.setBackgroundColor(azul);
                c.setPadding(6);
                c.setHorizontalAlignment(Element.ALIGN_CENTER);
                tabla.addCell(c);
            }

            double total = 0;
            boolean alterna = false;
            Color gris = new Color(242, 242, 242);
            for (FacturaReporteDTO f : facturas) {
                Color fondo = alterna ? gris : Color.WHITE;
                agregarCelda(tabla, String.valueOf(f.getNumeroFactura()), celda, Element.ALIGN_CENTER, fondo);
                agregarCelda(tabla, f.getFechaEmision() != null ? formatoFecha.format(f.getFechaEmision()) : "",
                        celda, Element.ALIGN_CENTER, fondo);
                agregarCelda(tabla, f.getClienteDenominacion(), celda, Element.ALIGN_LEFT, fondo);
                agregarCelda(tabla, f.getCondicionIva(), celda, Element.ALIGN_LEFT, fondo);
                agregarCelda(tabla, f.getPuntoVentaDescripcion(), celda, Element.ALIGN_LEFT, fondo);
                agregarCelda(tabla, formatoMoneda.format(f.getImporteTotal()), celda, Element.ALIGN_RIGHT, fondo);
                agregarCelda(tabla, String.valueOf(f.getCantidadItems()), celda, Element.ALIGN_CENTER, fondo);
                total += f.getImporteTotal();
                alterna = !alterna;
            }

            PdfPCell etiquetaTotal = new PdfPCell(new Phrase("TOTAL FACTURADO", totalFont));
            etiquetaTotal.setColspan(5);
            etiquetaTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            etiquetaTotal.setPadding(6);
            tabla.addCell(etiquetaTotal);
            PdfPCell valorTotal = new PdfPCell(new Phrase(formatoMoneda.format(total), totalFont));
            valorTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            valorTotal.setPadding(6);
            tabla.addCell(valorTotal);
            tabla.addCell(new PdfPCell(new Phrase("")));

            doc.add(tabla);
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el PDF", e);
        } finally {
            doc.close();
        }
        return out.toByteArray();
    }

    private void agregarCelda(PdfPTable tabla, String texto, Font font, int alineacion, Color fondo) {
        PdfPCell c = new PdfPCell(new Phrase(texto == null ? "" : texto, font));
        c.setHorizontalAlignment(alineacion);
        c.setBackgroundColor(fondo);
        c.setPadding(5);
        tabla.addCell(c);
    }

    // ---------------------------------------------------------------- EXCEL

    public byte[] generarExcel(List<FacturaReporteDTO> facturas) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet hoja = wb.createSheet("Facturas");
            CreationHelper helper = wb.getCreationHelper();

            org.apache.poi.ss.usermodel.Font negrita = wb.createFont();
            negrita.setBold(true);
            negrita.setColor(IndexedColors.WHITE.getIndex());

            CellStyle estiloEncabezado = wb.createCellStyle();
            estiloEncabezado.setFont(negrita);
            estiloEncabezado.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            estiloEncabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            estiloEncabezado.setBorderBottom(BorderStyle.THIN);

            CellStyle estiloFecha = wb.createCellStyle();
            estiloFecha.setDataFormat(helper.createDataFormat().getFormat("dd/mm/yyyy"));

            CellStyle estiloImporte = wb.createCellStyle();
            estiloImporte.setDataFormat(helper.createDataFormat().getFormat("#,##0.00"));

            Row header = hoja.createRow(0);
            for (int i = 0; i < COLUMNAS.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(COLUMNAS[i]);
                c.setCellStyle(estiloEncabezado);
            }

            int fila = 1;
            for (FacturaReporteDTO f : facturas) {
                Row r = hoja.createRow(fila++);
                r.createCell(0).setCellValue(f.getNumeroFactura() != null ? f.getNumeroFactura() : 0);
                Cell fecha = r.createCell(1);
                if (f.getFechaEmision() != null) {
                    fecha.setCellValue(f.getFechaEmision());
                    fecha.setCellStyle(estiloFecha);
                }
                r.createCell(2).setCellValue(f.getClienteDenominacion());
                r.createCell(3).setCellValue(f.getCondicionIva());
                r.createCell(4).setCellValue(f.getPuntoVentaDescripcion());
                Cell importe = r.createCell(5);
                importe.setCellValue(f.getImporteTotal());
                importe.setCellStyle(estiloImporte);
                r.createCell(6).setCellValue(f.getCantidadItems());
            }

            // Fila de total con fórmula, así se recalcula si se edita la planilla
            if (!facturas.isEmpty()) {
                CellStyle estiloTotal = wb.createCellStyle();
                org.apache.poi.ss.usermodel.Font fTotal = wb.createFont();
                fTotal.setBold(true);
                estiloTotal.setFont(fTotal);
                estiloTotal.setDataFormat(helper.createDataFormat().getFormat("#,##0.00"));

                Row totalRow = hoja.createRow(fila);
                Cell etiqueta = totalRow.createCell(4);
                etiqueta.setCellValue("TOTAL");
                etiqueta.setCellStyle(estiloTotal);
                Cell suma = totalRow.createCell(5);
                suma.setCellFormula("SUM(F2:F" + fila + ")");
                suma.setCellStyle(estiloTotal);
            }

            for (int i = 0; i < COLUMNAS.length; i++) {
                hoja.autoSizeColumn(i);
            }
            hoja.createFreezePane(0, 1);

            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el Excel", e);
        }
    }
}
