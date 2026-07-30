package com.obratrack.service;

import com.obratrack.model.Partida;
import org.apache.poi.ss.usermodel.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.util.*;

/**
 * Importa partidas desde un Excel de licitacion/presupuesto.
 * Los Excel de obras en Peru NO tienen formato estandar (cada entidad usa el suyo),
 * por eso esta clase detecta automaticamente:
 *   1) la fila de encabezados (buscando palabras clave, no posicion fija)
 *   2) que columna corresponde a cada campo (codigo, descripcion, unidad, cantidad, precio, total)
 *   3) filas de subtotal/total que deben excluirse
 *   4) partidas "padre" (agrupadoras, sin metrado) vs partidas "hoja" (ejecutables)
 *
 * Soporta .xlsx y .xls (a traves de Apache POI, que detecta el formato por el contenido).
 *
 * Un mismo libro suele traer varias hojas de presupuesto (p.ej. "PPTO", "PPTO 95%",
 * un consolidado), cada una con un total distinto. Por eso se expone
 * {@link #listarHojasImportables(String)} para que la UI deje elegir cual importar,
 * e {@link #importar(String, String)} para importar una hoja concreta.
 */
public class ExcelImporter {

    private static final String[] KEYWORDS_ITEM = {"item", "n°", "nro", "codigo", "código"};
    private static final String[] KEYWORDS_DESCRIPCION = {"partida", "descripcion", "descripción", "obra", "detalle"};
    private static final String[] KEYWORDS_UNIDAD = {"unidad", "und", "u/m", "um", "medida", "und."};
    private static final String[] KEYWORDS_CANTIDAD = {"cantidad", "cant", "metrado"};
    private static final String[] KEYWORDS_PRECIO = {"precio", "costo", "p.u.", "pu", "unitario", "unit", "precio s/"};
    private static final String[] KEYWORDS_TOTAL = {"total", "parcial", "subtotal", "importe", "parcial s/"};

    private static final int MAX_FILAS_BUSCAR_HEADER = 20;

    /** Mapa de equivalencias de unidades para normalizar (m2/M2/mt2 -> m2, etc.) */
    private static final Map<String, String> UNIDADES_EQUIVALENTES = Map.ofEntries(
            Map.entry("bls", "bolsa"), Map.entry("bls.", "bolsa"), Map.entry("bol", "bolsa"),
            Map.entry("m2", "m2"), Map.entry("mt2", "m2"),
            Map.entry("m3", "m3"), Map.entry("mt3", "m3"),
            Map.entry("kg", "kg"), Map.entry("kgf", "kg"),
            Map.entry("gln", "galon"), Map.entry("gal", "galon"),
            Map.entry("glb", "global"), Map.entry("global", "global"),
            Map.entry("jgo", "juego"), Map.entry("jg", "juego"),
            Map.entry("pto", "punto"), Map.entry("pza", "pieza"), Map.entry("pzas", "pieza"),
            Map.entry("vje", "viaje"), Map.entry("mes", "mes"),
            Map.entry("ml", "ml"), Map.entry("m.l.", "ml"),
            Map.entry("und", "und"), Map.entry("und.", "und"), Map.entry("u", "und")
    );

    /**
     * Estructura de columnas detectada en una hoja concreta.
     * Se expone por separado para poder mostrar un preview al usuario antes de confirmar.
     */
    public static class EstructuraDetectada {
        public int filaHeader = -1;
        public int colItem = -1;
        public int colDescripcion = -1;
        public int colUnidad = -1;
        public int colCantidad = -1;
        public int colPrecio = -1;
        public int colTotal = -1;
        public String nombreHoja;
        public int totalHojasEnLibro = 1;

        public boolean esValida() {
            return filaHeader >= 0 && colDescripcion >= 0;
        }
    }

    /**
     * Resumen ligero de una hoja candidata a importar: nombre, cuantas partidas
     * detecta y el presupuesto (suma de partidas ejecutables). Permite que la UI
     * muestre "PPTO — 688 partidas — S/. 3,639,932.03" y el usuario elija.
     */
    public static final class HojaImportable {
        public final String nombre;
        public final int totalPartidas;
        public final int partidasEjecutables;
        public final double presupuesto;

        public HojaImportable(String nombre, int totalPartidas, int partidasEjecutables, double presupuesto) {
            this.nombre = nombre;
            this.totalPartidas = totalPartidas;
            this.partidasEjecutables = partidasEjecutables;
            this.presupuesto = presupuesto;
        }

        @Override
        public String toString() {
            return String.format("%s  —  %d partidas (%d ejecutables)  —  S/. %,.2f",
                    nombre, totalPartidas, partidasEjecutables, presupuesto);
        }
    }

    // ============================================================
    //  Deteccion de estructura
    // ============================================================

    /** Detecta la estructura de la hoja de presupuesto por defecto del libro (para preview). */
    public EstructuraDetectada detectarEstructura(String rutaArchivo) throws IOException {
        try (FileInputStream fis = new FileInputStream(rutaArchivo);
             Workbook workbook = WorkbookFactory.create(fis)) {
            Sheet hoja = seleccionarHojaPresupuesto(workbook);
            return detectarEnHoja(hoja, workbook.getNumberOfSheets());
        }
    }

    /** Escanea las primeras filas de una hoja y devuelve la estructura de columnas detectada. */
    private EstructuraDetectada detectarEnHoja(Sheet hoja, int totalHojas) {
        EstructuraDetectada estructura = new EstructuraDetectada();
        estructura.nombreHoja = hoja.getSheetName();
        estructura.totalHojasEnLibro = totalHojas;

        int mejorFila = -1;
        int mejorPuntaje = 0;
        Map<String, Integer> mejoresColumnas = null;

        int limite = Math.min(MAX_FILAS_BUSCAR_HEADER, hoja.getLastRowNum() + 1);
        for (int i = 0; i <= limite; i++) {
            Row fila = hoja.getRow(i);
            if (fila == null) continue;

            Map<String, Integer> columnas = mapearColumnas(fila);
            int puntaje = columnas.size();
            if (puntaje > mejorPuntaje && columnas.containsKey("descripcion")) {
                mejorPuntaje = puntaje;
                mejorFila = i;
                mejoresColumnas = columnas;
            }
        }

        if (mejorFila >= 0 && mejoresColumnas != null) {
            estructura.filaHeader = mejorFila;
            estructura.colItem = mejoresColumnas.getOrDefault("item", -1);
            estructura.colDescripcion = mejoresColumnas.getOrDefault("descripcion", -1);
            estructura.colUnidad = mejoresColumnas.getOrDefault("unidad", -1);
            estructura.colCantidad = mejoresColumnas.getOrDefault("cantidad", -1);
            estructura.colPrecio = mejoresColumnas.getOrDefault("precio", -1);
            estructura.colTotal = mejoresColumnas.getOrDefault("total", -1);
        }
        return estructura;
    }

    /**
     * Elige la hoja de presupuesto por defecto: prioriza una llamada PRESUPUESTO/RESUMEN;
     * si ninguna coincide, la primera hoja con una estructura reconocible; en ultimo
     * caso, la primera hoja del libro.
     */
    private Sheet seleccionarHojaPresupuesto(Workbook workbook) {
        Sheet preferidaPorNombre = null;
        Sheet primeraValida = null;
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet hoja = workbook.getSheetAt(i);
            String nombre = normalizar(workbook.getSheetName(i));
            boolean valida = detectarEnHoja(hoja, workbook.getNumberOfSheets()).esValida();
            if (valida && primeraValida == null) {
                primeraValida = hoja;
            }
            if (valida && preferidaPorNombre == null
                    && (nombre.contains("presupuesto") || nombre.contains("resumen"))) {
                preferidaPorNombre = hoja;
            }
        }
        if (preferidaPorNombre != null) return preferidaPorNombre;
        if (primeraValida != null) return primeraValida;
        return workbook.getSheetAt(0);
    }

    private Map<String, Integer> mapearColumnas(Row fila) {
        Map<String, Integer> columnas = new HashMap<>();
        for (Cell celda : fila) {
            String texto = normalizar(obtenerTextoCelda(celda));
            if (texto.isBlank()) continue;

            if (!columnas.containsKey("item") && contieneAlguna(texto, KEYWORDS_ITEM)) {
                columnas.put("item", celda.getColumnIndex());
            } else if (!columnas.containsKey("descripcion") && contieneAlguna(texto, KEYWORDS_DESCRIPCION)) {
                columnas.put("descripcion", celda.getColumnIndex());
            } else if (!columnas.containsKey("unidad") && contieneAlguna(texto, KEYWORDS_UNIDAD)) {
                columnas.put("unidad", celda.getColumnIndex());
            } else if (!columnas.containsKey("cantidad") && contieneAlguna(texto, KEYWORDS_CANTIDAD)) {
                columnas.put("cantidad", celda.getColumnIndex());
            } else if (!columnas.containsKey("precio") && contieneAlguna(texto, KEYWORDS_PRECIO)) {
                columnas.put("precio", celda.getColumnIndex());
            } else if (!columnas.containsKey("total") && contieneAlguna(texto, KEYWORDS_TOTAL)) {
                columnas.put("total", celda.getColumnIndex());
            }
        }
        return columnas;
    }

    // ============================================================
    //  Listado de hojas importables
    // ============================================================

    /**
     * Devuelve, para cada hoja del libro con estructura reconocible, un resumen con
     * el numero de partidas y el presupuesto detectado. La UI lo usa para dejar
     * elegir que hoja importar cuando el libro trae varias.
     */
    public List<HojaImportable> listarHojasImportables(String rutaArchivo) throws IOException {
        List<HojaImportable> lista = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(rutaArchivo);
             Workbook workbook = WorkbookFactory.create(fis)) {
            int totalHojas = workbook.getNumberOfSheets();
            for (int i = 0; i < totalHojas; i++) {
                Sheet hoja = workbook.getSheetAt(i);
                EstructuraDetectada estructura = detectarEnHoja(hoja, totalHojas);
                if (!estructura.esValida()) continue;

                ImportResult temp = new ImportResult();
                procesarFilas(hoja, estructura, temp);
                int ejecutables = (int) temp.getPartidasImportadas().stream().filter(p -> !p.isEsPadre()).count();
                lista.add(new HojaImportable(hoja.getSheetName(),
                        temp.getPartidasImportadas().size(), ejecutables, temp.getPresupuestoTotal()));
            }
        }
        return lista;
    }

    // ============================================================
    //  Importacion
    // ============================================================

    /** Importa la hoja de presupuesto por defecto del libro. */
    public ImportResult importar(String rutaArchivo) {
        return importar(rutaArchivo, null);
    }

    /**
     * Importa una hoja concreta (por nombre). Si {@code nombreHoja} es null, usa la
     * hoja de presupuesto por defecto. Todo el trabajo se hace con el libro abierto,
     * evitando acceder a celdas de un Workbook ya cerrado.
     *
     * Reglas de clasificacion de filas:
     *  - fila sin descripcion -> se omite (probable fila vacia separadora)
     *  - fila cuyo texto empieza por TOTAL/SUBTOTAL y sin unidad -> se omite (subtotal)
     *  - fila con descripcion y unidad -> partida "hoja" (ejecutable, suma al presupuesto)
     *  - fila con descripcion pero SIN unidad -> partida "padre" (agrupadora)
     */
    public ImportResult importar(String rutaArchivo, String nombreHoja) {
        ImportResult resultado = new ImportResult();
        try (FileInputStream fis = new FileInputStream(rutaArchivo);
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet hoja = (nombreHoja != null && !nombreHoja.isBlank())
                    ? workbook.getSheet(nombreHoja)
                    : seleccionarHojaPresupuesto(workbook);

            if (hoja == null) {
                resultado.agregarError("No se encontro la hoja '" + nombreHoja + "' en el archivo.");
                resultado.setExitoso(false);
                return resultado;
            }

            EstructuraDetectada estructura = detectarEnHoja(hoja, workbook.getNumberOfSheets());
            if (!estructura.esValida()) {
                resultado.agregarError("No se pudo detectar una columna de descripcion/partida en las primeras "
                        + MAX_FILAS_BUSCAR_HEADER + " filas de la hoja '" + hoja.getSheetName() + "'. Verifica que el "
                        + "Excel tenga un encabezado reconocible (ej: Item, Descripcion, Unidad, Metrado, Precio, Total).");
                resultado.setExitoso(false);
                return resultado;
            }

            if (estructura.colUnidad < 0) {
                resultado.agregarAdvertencia("No se detecto columna de Unidad. Las partidas se importaran sin unidad.");
            }
            if (estructura.colPrecio < 0) {
                resultado.agregarAdvertencia("No se detecto columna de Precio Unitario. Los costos quedaran en 0 y deberas completarlos manualmente.");
            }
            if (estructura.totalHojasEnLibro > 1) {
                resultado.agregarAdvertencia("El archivo tiene " + estructura.totalHojasEnLibro
                        + " hojas. Se importo la hoja '" + estructura.nombreHoja + "'.");
            }

            procesarFilas(hoja, estructura, resultado);

            resultado.setExitoso(!resultado.getPartidasImportadas().isEmpty());
            if (resultado.getPartidasImportadas().isEmpty()) {
                resultado.agregarError("No se encontraron partidas validas despues de la fila de encabezado "
                        + "en la hoja '" + hoja.getSheetName() + "'.");
            }

            detectarCodigosDuplicados(resultado);
            calcularSubtotalesYValidar(resultado);

        } catch (java.io.FileNotFoundException e) {
            resultado.agregarError("No se encontro el archivo. Verifica la ruta: " + rutaArchivo);
            resultado.setExitoso(false);
        } catch (IOException e) {
            String detalle = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (detalle.contains("being used") || detalle.contains("locked") || detalle.contains("permission")) {
                resultado.agregarError("El archivo parece estar abierto en Excel u otro programa. Cierralo e intenta de nuevo.");
            } else {
                resultado.agregarError("No se pudo leer el archivo: " + e.getMessage());
            }
            resultado.setExitoso(false);
        } catch (Exception e) {
            resultado.agregarError("Error inesperado al importar: " + e.getMessage());
            resultado.setExitoso(false);
        }
        return resultado;
    }

    /**
     * Recorre las filas de datos de la hoja y llena el ImportResult con las partidas,
     * el presupuesto (suma de ejecutables), filas omitidas y advertencias.
     * Extraido para reutilizarse en importar() y en listarHojasImportables().
     */
    private void procesarFilas(Sheet hoja, EstructuraDetectada estructura, ImportResult resultado) {
        double sumaEjecutables = 0;

        for (int i = estructura.filaHeader + 1; i <= hoja.getLastRowNum(); i++) {
            Row fila = hoja.getRow(i);
            if (fila == null) {
                continue; // fila completamente vacia, se omite silenciosamente
            }

            String descripcion = obtenerTextoCelda(celdaDe(fila, estructura.colDescripcion));
            if (descripcion == null || descripcion.isBlank()) {
                resultado.incrementarFilasOmitidas();
                continue;
            }

            String descripcionNorm = normalizar(descripcion);
            boolean pareceTotalGeneral = descripcionNorm.startsWith("total") || descripcionNorm.startsWith("subtotal")
                    || descripcionNorm.equals("son") || descripcionNorm.startsWith("presupuesto total")
                    || descripcionNorm.startsWith("costo directo") || descripcionNorm.startsWith("costo total");

            String codigo = estructura.colItem >= 0 ? obtenerTextoCelda(celdaDe(fila, estructura.colItem)) : null;
            String unidadRaw = estructura.colUnidad >= 0 ? obtenerTextoCelda(celdaDe(fila, estructura.colUnidad)) : null;
            Double cantidad = estructura.colCantidad >= 0 ? obtenerNumeroCelda(celdaDe(fila, estructura.colCantidad)) : null;
            Double precio = estructura.colPrecio >= 0 ? obtenerNumeroCelda(celdaDe(fila, estructura.colPrecio)) : null;
            Double total = estructura.colTotal >= 0 ? obtenerNumeroCelda(celdaDe(fila, estructura.colTotal)) : null;

            boolean tieneUnidad = unidadRaw != null && !unidadRaw.isBlank();

            if (pareceTotalGeneral && !tieneUnidad) {
                resultado.incrementarFilasOmitidas();
                continue;
            }

            Partida p = new Partida();
            p.setCodigo(codigo != null ? codigo.trim() : null);
            p.setDescripcion(descripcion.trim());
            p.setUnidad(normalizarUnidad(unidadRaw));
            p.setCantidadPresupuestada(cantidad != null ? cantidad : 0);
            p.setCostoUnitario(precio != null ? precio : 0);

            double totalCalculado;
            if (total != null && total > 0) {
                totalCalculado = total;
            } else {
                totalCalculado = p.getCantidadPresupuestada() * p.getCostoUnitario();
            }
            p.setCostoTotalPresupuestado(totalCalculado);

            p.setEsPadre(!tieneUnidad);
            p.setNivel(codigo != null && !codigo.isBlank() ? codigo.trim().split("\\.").length : 1);

            if (!tieneUnidad) {
                resultado.agregarAdvertencia("Fila " + (i + 1) + ": '" + truncar(descripcion, 50) + "' sin unidad -> se importo como partida agrupadora (padre).");
            } else if (cantidad == null || cantidad == 0) {
                resultado.agregarAdvertencia("Fila " + (i + 1) + ": '" + truncar(descripcion, 50) + "' tiene unidad pero cantidad 0 o vacia.");
            }

            // Verificacion de coherencia por partida ejecutable: total del Excel vs cantidad x precio.
            // Detecta columnas mal mapeadas o numeros mal leidos.
            if (tieneUnidad && total != null && total > 0 && cantidad != null && cantidad > 0
                    && precio != null && precio > 0) {
                double calc = cantidad * precio;
                double tol = Math.max(1.0, total * 0.01); // 1% o 1 sol
                if (Math.abs(calc - total) > tol) {
                    resultado.agregarAdvertencia(String.format(
                            "Fila %d: '%s' -> el total del Excel (S/. %,.2f) no coincide con cantidad x precio (%,.2f x %,.2f = S/. %,.2f). Se respeto el total del Excel.",
                            i + 1, truncar(descripcion, 40), total, cantidad, precio, calc));
                }
            }
            if (tieneUnidad && (codigo == null || codigo.isBlank())) {
                resultado.agregarAdvertencia("Fila " + (i + 1) + ": '" + truncar(descripcion, 40)
                        + "' es una partida ejecutable SIN codigo. No se podra agrupar en ningun subtotal de seccion.");
            }

            resultado.agregarPartida(p);
            if (tieneUnidad) {
                sumaEjecutables += totalCalculado;
            }
        }

        resultado.setPresupuestoTotal(sumaEjecutables);
    }

    /**
     * Calcula el subtotal de cada partida agrupadora (padre) como la SUMA de las partidas
     * ejecutables que cuelgan de ella (segun el codigo jerarquico: una hoja "01.02.03.01"
     * pertenece a los padres "01", "01.02" y "01.02.03"). Ademas RECONCILIA ese subtotal
     * calculado contra el subtotal que traia el propio Excel en la columna de total:
     * si no cuadran (mas alla de una tolerancia), lo reporta como advertencia y prevalece
     * la suma real de las partidas. Es la parte mas minuciosa: garantiza que lo importado
     * sea internamente consistente.
     */
    private void calcularSubtotalesYValidar(ImportResult resultado) {
        List<Partida> partidas = resultado.getPartidasImportadas();
        int padres = 0;
        int descuadres = 0;

        for (Partida padre : partidas) {
            if (!padre.isEsPadre()) continue;
            padres++;

            String codigoPadre = padre.getCodigo();
            double subtotalExcel = padre.getCostoTotalPresupuestado(); // lo que traia el Excel (o 0)

            if (codigoPadre == null || codigoPadre.isBlank()) {
                // Sin codigo no podemos hacer roll-up: conservamos lo que trajo el Excel.
                continue;
            }

            String prefijo = codigoPadre.trim() + ".";
            double sumaHijas = 0;
            int nHijas = 0;
            for (Partida hija : partidas) {
                if (hija.isEsPadre()) continue;
                String codHija = hija.getCodigo();
                if (codHija != null && codHija.trim().startsWith(prefijo)) {
                    sumaHijas += hija.getCostoTotalPresupuestado();
                    nHijas++;
                }
            }

            // El subtotal mostrado sera la suma real de las partidas (si tiene hijas);
            // si el padre no tiene hijas ejecutables, se respeta el valor del Excel.
            double subtotalFinal = nHijas > 0 ? sumaHijas : subtotalExcel;
            padre.setCostoTotalPresupuestado(subtotalFinal);

            // Reconciliacion: si el Excel declaraba un subtotal y no cuadra con la suma real.
            if (subtotalExcel > 0 && nHijas > 0) {
                double dif = Math.abs(subtotalExcel - sumaHijas);
                double tol = Math.max(1.0, subtotalExcel * 0.005); // 0.5% o 1 sol
                if (dif > tol) {
                    descuadres++;
                    resultado.agregarAdvertencia(String.format(
                            "Subtotal de la seccion '%s %s': el Excel indica S/. %,.2f pero la suma de sus %d partidas es S/. %,.2f (diferencia S/. %,.2f). Se uso la suma real de las partidas.",
                            codigoPadre.trim(), truncar(padre.getDescripcion(), 40),
                            subtotalExcel, nHijas, sumaHijas, dif));
                }
            }
        }

        resultado.setPartidasPadre(padres);
        resultado.setSubtotalesCuadran(descuadres == 0);

        // Mensaje de verificacion final (positivo) para dar confianza de que se importo bien.
        int ejecutables = partidas.size() - padres;
        if (descuadres == 0) {
            resultado.agregarInforme(String.format(
                    "Verificacion OK: %d partidas (%d ejecutables, %d agrupadoras). "
                    + "Presupuesto total: S/. %,.2f. Todos los subtotales de seccion cuadran con la suma de sus partidas.",
                    partidas.size(), ejecutables, padres, resultado.getPresupuestoTotal()));
        } else {
            resultado.agregarInforme(String.format(
                    "Importacion revisada: %d partidas (%d ejecutables, %d agrupadoras). "
                    + "Presupuesto total (suma de ejecutables): S/. %,.2f. "
                    + "%d subtotal(es) de seccion NO cuadraban con el Excel; se corrigieron con la suma real (ver advertencias).",
                    partidas.size(), ejecutables, padres, resultado.getPresupuestoTotal(), descuadres));
        }
    }

    // ---------- utilidades ----------

    private void detectarCodigosDuplicados(ImportResult resultado) {
        Map<String, Integer> conteo = new HashMap<>();
        for (Partida p : resultado.getPartidasImportadas()) {
            if (p.getCodigo() == null || p.getCodigo().isBlank()) continue;
            conteo.merge(p.getCodigo().trim(), 1, Integer::sum);
        }
        conteo.forEach((codigo, veces) -> {
            if (veces > 1) {
                resultado.agregarAdvertencia("El codigo '" + codigo + "' aparece " + veces
                        + " veces en el Excel. Revisa que no haya partidas duplicadas.");
            }
        });
    }

    private Cell celdaDe(Row fila, int col) {
        return col >= 0 ? fila.getCell(col) : null;
    }

    private String obtenerTextoCelda(Cell celda) {
        if (celda == null) return null;
        switch (celda.getCellType()) {
            case STRING:
                return celda.getStringCellValue();
            case NUMERIC:
                double v = celda.getNumericCellValue();
                if (v == Math.floor(v)) return String.valueOf((long) v);
                return String.valueOf(v);
            case FORMULA:
                try {
                    return celda.getStringCellValue();
                } catch (Exception e) {
                    try {
                        return String.valueOf(celda.getNumericCellValue());
                    } catch (Exception e2) {
                        return null;
                    }
                }
            case BOOLEAN:
                return String.valueOf(celda.getBooleanCellValue());
            default:
                return null;
        }
    }

    private Double obtenerNumeroCelda(Cell celda) {
        if (celda == null) return null;
        try {
            if (celda.getCellType() == CellType.NUMERIC) {
                return celda.getNumericCellValue();
            }
            if (celda.getCellType() == CellType.FORMULA) {
                try {
                    return celda.getNumericCellValue();
                } catch (Exception ignored) {
                    // la formula devuelve texto: cae al parseo de texto de abajo
                }
            }
            String texto = obtenerTextoCelda(celda);
            if (texto == null || texto.isBlank()) return null;
            String limpio = texto.replace("S/.", "").replace("S/", "").trim();
            if (limpio.isBlank()) return null;
            return parsearNumeroFlexible(limpio);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parsea numeros que pueden venir en formato "1,750.50" (punto decimal, coma de miles)
     * o "1.750,50" (coma decimal, punto de miles), segun cual sea el ultimo separador.
     */
    private Double parsearNumeroFlexible(String texto) {
        boolean tieneComa = texto.contains(",");
        boolean tienePunto = texto.contains(".");

        String limpio;
        if (tieneComa && tienePunto) {
            // El separador que aparece mas a la derecha es el decimal
            if (texto.lastIndexOf(',') > texto.lastIndexOf('.')) {
                limpio = texto.replace(".", "").replace(",", ".");
            } else {
                limpio = texto.replace(",", "");
            }
        } else if (tieneComa) {
            // Solo coma: asumimos que es decimal si tiene 1-2 digitos despues, sino es separador de miles
            int pos = texto.lastIndexOf(',');
            String despues = texto.substring(pos + 1);
            limpio = (despues.length() <= 2) ? texto.replace(",", ".") : texto.replace(",", "");
        } else {
            limpio = texto;
        }
        return Double.parseDouble(limpio);
    }

    private boolean contieneAlguna(String texto, String[] keywords) {
        for (String k : keywords) {
            if (texto.equals(k) || texto.contains(k)) return true;
        }
        return false;
    }

    private String normalizar(String texto) {
        if (texto == null) return "";
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return sinTildes.toLowerCase().trim();
    }

    private String normalizarUnidad(String unidadRaw) {
        if (unidadRaw == null || unidadRaw.isBlank()) return null;
        String clave = normalizar(unidadRaw).replace(".", "");
        return UNIDADES_EQUIVALENTES.getOrDefault(clave, unidadRaw.trim());
    }

    private String truncar(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
