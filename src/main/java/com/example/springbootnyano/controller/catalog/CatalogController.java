package com.example.springbootnyano.controller.catalog;

import com.example.springbootnyano.entity.catalog.OptionCatalogEntity;
import com.example.springbootnyano.repository.OptionCatalogRepository;
import com.example.springbootnyano.util.AuthGuard;
import com.example.springbootnyano.util.CatalogFormats;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
public class CatalogController {

    @Autowired private OptionCatalogRepository repo;
    @Autowired private AuthGuard guard;

    private static final Map<String, List<String>> DEFAULTS = Map.of(
            "BRAND",   List.of("Yonex", "Li-Ning", "Victor", "Mizuno", "Kumpoo", "Apacs"),
            "COLOR",   List.of("Đỏ đô", "Đen nhám", "Xanh đen", "Trắng bạc", "Vàng đồng", "Hồng", "Xanh ngọc"),
            "STRING",  List.of("Yonex BG65", "Yonex BG80", "Yonex BG66 Ultimax", "Li-Ning No.1", "Victor VS-850", "Yonex Aerobite"),
            "TENSION", List.of("9 kg", "9.5 kg", "10 kg", "10.5 kg", "11 kg", "11.5 kg", "12 kg"),
            "GRIP",    List.of("3U/G5", "4U/G5", "4U/G6", "5U/G5", "5U/G6")
    );

    @GetMapping("/catalog/{category}")
    public List<OptionCatalogEntity> list(@PathVariable String category) {
        String cat = category.toUpperCase();
        List<OptionCatalogEntity> list = repo.findByCategoryOrderByIdAsc(cat);
        if (list.isEmpty() && DEFAULTS.containsKey(cat)) {
            for (String v : DEFAULTS.get(cat)) {
                OptionCatalogEntity e = new OptionCatalogEntity();
                e.setCategory(cat);
                e.setOptionValue(v);
                list.add(repo.save(e));
            }
        }
        Collator vi = Collator.getInstance(new Locale("vi", "VN"));
        Comparator<OptionCatalogEntity> cmp;
        if (cat.equals("TENSION"))      cmp = Comparator.comparingDouble(e -> CatalogFormats.tensionKey(e.getOptionValue()));
        else if (cat.equals("GRIP"))    cmp = Comparator.comparingInt(e -> CatalogFormats.gripKey(e.getOptionValue()));
        else                            cmp = (a, b) -> vi.compare(a.getOptionValue(), b.getOptionValue());
        list.sort(cmp);
        return list;
    }

    @PostMapping("/admin/catalog/{category}")
    public ResponseEntity<?> add(HttpServletRequest request, @PathVariable String category,
                                 @RequestBody Map<String, String> body) {
        guard.requireAdmin(request);
        String cat = category.toUpperCase();
        if (!DEFAULTS.containsKey(cat)) return ResponseEntity.badRequest().body(Map.of("message", "Danh mục không hợp lệ"));
        String value = CatalogFormats.canonical(cat, body.get("value"));
        if (repo.existsByCategoryAndOptionValueIgnoreCase(cat, value))
            return ResponseEntity.status(409).body(Map.of("message", "Giá trị \"" + value + "\" đã tồn tại trong danh mục"));
        String folded = CatalogFormats.stripAccents(value);
        for (OptionCatalogEntity ex : repo.findByCategoryOrderByIdAsc(cat)) {
            if (CatalogFormats.stripAccents(ex.getOptionValue()).equalsIgnoreCase(folded))
                return ResponseEntity.status(409).body(Map.of("message",
                        "Giá trị này trùng với \"" + ex.getOptionValue() + "\" (bỏ dấu thì giống nhau). Hãy dùng giá trị có dấu."));
        }
        List<OptionCatalogEntity> existing = repo.findByCategoryOrderByIdAsc(cat);
        for (OptionCatalogEntity e : existing) {
            if (CatalogFormats.stripAccents(e.getOptionValue()).equalsIgnoreCase(CatalogFormats.stripAccents(value))) {
                return ResponseEntity.status(409).body(Map.of("message",
                        "Giá trị này gần giống \"" + e.getOptionValue() + "\" đã tồn tại (khác dấu). Dùng giá trị có dấu."));
            }
        }

        OptionCatalogEntity e = new OptionCatalogEntity();
        e.setCategory(cat);
        e.setOptionValue(value);
        return ResponseEntity.ok(repo.save(e));
    }

    @DeleteMapping("/admin/catalog/{id}")
    public ResponseEntity<?> delete(HttpServletRequest request, @PathVariable Long id) {
        guard.requireAdmin(request);
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.ok().build();
    }
}