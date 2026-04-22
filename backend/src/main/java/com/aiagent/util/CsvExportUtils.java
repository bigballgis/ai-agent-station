package com.aiagent.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Utility class for CSV export operations.
 * Provides streaming CSV writing to avoid loading entire datasets into memory.
 */
public final class CsvExportUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String CSV_DELIMITER = ",";
    private static final String LINE_SEPARATOR = "\n";

    private CsvExportUtils() {
    }

    /**
     * Write CSV data to an output stream using streaming.
     *
     * @param outputStream target output stream
     * @param headers      list of header column names
     * @param rows         list of row data arrays (each Object[] represents one row)
     * @throws IOException if writing fails
     */
    public static void writeCsv(OutputStream outputStream, List<String> headers, List<Object[]> rows)
            throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            // Write BOM for Excel compatibility
            writer.write('\uFEFF');

            // Write header row
            if (headers != null && !headers.isEmpty()) {
                StringBuilder headerLine = new StringBuilder();
                for (int i = 0; i < headers.size(); i++) {
                    if (i > 0) {
                        headerLine.append(CSV_DELIMITER);
                    }
                    headerLine.append(escapeCsv(headers.get(i)));
                }
                headerLine.append(LINE_SEPARATOR);
                writer.write(headerLine.toString());
            }

            // Write data rows
            if (rows != null) {
                for (Object[] row : rows) {
                    StringBuilder rowLine = new StringBuilder();
                    for (int i = 0; i < row.length; i++) {
                        if (i > 0) {
                            rowLine.append(CSV_DELIMITER);
                        }
                        rowLine.append(escapeCsv(formatValue(row[i])));
                    }
                    rowLine.append(LINE_SEPARATOR);
                    writer.write(rowLine.toString());
                }
            }

            writer.flush();
        }
    }

    /**
     * Escape a value for safe CSV output.
     * Wraps in double quotes if the value contains commas, quotes, or newlines.
     *
     * @param value the value to escape
     * @return escaped CSV-safe string
     */
    public static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }

        // If the value contains comma, double quote, or newline, wrap in quotes
        if (value.contains(CSV_DELIMITER) || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            // Escape double quotes by doubling them
            String escaped = value.replace("\"", "\"\"");
            return "\"" + escaped + "\"";
        }

        return value;
    }

    /**
     * Format a LocalDateTime to a standard date-time string.
     *
     * @param dateTime the date-time to format
     * @return formatted string, or empty string if null
     */
    public static String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DATE_FORMATTER);
    }

    /**
     * Format a file size in bytes to a human-readable string.
     *
     * @param bytes file size in bytes
     * @return human-readable size string (e.g., "1.5 MB")
     */
    public static String formatFileSize(Long bytes) {
        if (bytes == null || bytes < 0) {
            return "0 B";
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char unit = "KMGTPE".charAt(exp - 1);
        double value = bytes / Math.pow(1024, exp);
        return String.format("%.1f %sB", value, unit);
    }

    /**
     * Format an arbitrary value to a string suitable for CSV output.
     */
    private static String formatValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof LocalDateTime dateTime) {
            return formatDate(dateTime);
        }
        if (value instanceof Boolean bool) {
            return bool ? "Yes" : "No";
        }
        return value.toString();
    }
}
