package com.example.springbootnyano.util;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CatalogFormats {
    private CatalogFormats() {}

    private static final Pattern TENSION_RE = Pattern.compile("^(\\d{1,2}(?:\\.\\d{1,2})?)\\s*kg$", Pattern.CASE_INSENSITIVE);
    private static final Pattern GRIP_RE = Pattern.compile("^([2-5])u/g([3-6])$", Pattern.CASE_INSENSITIVE);

    public static String clean(String raw) {
        String v = raw == null ? "" : Normalizer.normalize(raw, Normalizer.Form.NFC);
        return v.trim().replaceAll("\\s+", " ");
    }

    public static String canonical(String category, String raw) {
        String v = clean(raw);
        if (v.length() < 2) throw bad("Giá trị quá ngắn (tối thiểu 2 ký tự)");
        if (v.length() > 100) throw bad("Giá trị quá dài (tối đa 100 ký tự)");
        if (category.equals("TENSION")) {
            Matcher m = TENSION_RE.matcher(v);
            if (!m.matches()) throw bad("Độ căng phải kèm đơn vị kg, vd: 10 kg hoặc 10.5kg");
            double n = Double.parseDouble(m.group(1));
            if (n < 8 || n > 16) throw bad("Độ căng hợp lệ của vợt cầu lông: 8–16 kg");
            return m.group(1) + " kg";
        }
        if (category.equals("GRIP")) {
            Matcher m = GRIP_RE.matcher(v);
            if (!m.matches()) throw bad("Trọng lượng/cỡ cán phải dạng sốU/Gsố, vd: 4U/G5");
            return m.group(1) + "U/G" + m.group(2);
        }
        return v;
    }

    public static double tensionKey(String v) {
        try { return Double.parseDouble(v.replace(" kg", "").trim()); } catch (Exception e) { return Double.MAX_VALUE; }
    }
    public static int gripKey(String v) {
        try { return Integer.parseInt(v.substring(0, 1)) * 10 + Integer.parseInt(v.substring(4, 5)); }
        catch (Exception e) { return Integer.MAX_VALUE; }
    }

    private static ResponseStatusException bad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    public static String stripAccents(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return n.replace('đ', 'd').replace('Đ', 'D');
    }
}