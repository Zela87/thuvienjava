package com.thuvien.web;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public final class Maps {
    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Maps() {
    }

    public static String str(Map<String, Object> row, String key) {
        Object v = get(row, key);
        return v == null ? "" : v.toString();
    }

    public static int asInt(Map<String, Object> row, String key) {
        Object v = get(row, key);
        if (v == null) {
            return 0;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static double asDouble(Map<String, Object> row, String key) {
        Object v = get(row, key);
        if (v == null) {
            return 0;
        }
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(v.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static Object get(Map<String, Object> row, String key) {
        if (row == null) {
            return null;
        }
        if (row.containsKey(key)) {
            return row.get(key);
        }
        for (Map.Entry<String, Object> e : row.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(key)) {
                return e.getValue();
            }
        }
        return null;
    }

    public static String formatDate(Object v) {
        LocalDate d = toLocalDate(v);
        return d == null ? "" : d.format(D);
    }

    public static String formatDateTime(Object v) {
        if (v == null) {
            return "";
        }
        if (v instanceof Timestamp ts) {
            return ts.toLocalDateTime().format(DT);
        }
        if (v instanceof Date d) {
            return d.toLocalDate().atStartOfDay().format(DT);
        }
        return v.toString();
    }

    public static LocalDate toLocalDate(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Date d) {
            return d.toLocalDate();
        }
        if (v instanceof Timestamp ts) {
            return ts.toLocalDateTime().toLocalDate();
        }
        if (v instanceof LocalDate ld) {
            return ld;
        }
        try {
            return LocalDate.parse(v.toString().substring(0, 10));
        } catch (Exception e) {
            return null;
        }
    }

    public static String money(Object v) {
        double n = 0;
        if (v instanceof Number num) {
            n = num.doubleValue();
        } else if (v instanceof BigDecimal bd) {
            n = bd.doubleValue();
        } else if (v != null) {
            try {
                n = Double.parseDouble(v.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return String.format("%,.0f", n);
    }
}
