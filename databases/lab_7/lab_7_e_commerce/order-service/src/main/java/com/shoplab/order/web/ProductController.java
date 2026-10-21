package com.shoplab.order.web;

import com.shoplab.order.domain.Product;
import com.shoplab.order.dto.ProductRequest;
import com.shoplab.order.repo.ProductJdbcRepository;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class ProductController {

    private final ProductJdbcRepository products;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<List<Product>> catalog(@RequestParam(required = false) Long categoryId,
                                                 @RequestParam(required = false) BigDecimal maxPrice,
                                                 @RequestParam(required = false) String name,
                                                 @RequestParam(defaultValue = "20") int limit,
                                                 @RequestParam(required = false) BigDecimal cursorPrice,
                                                 @RequestParam(required = false) Long cursorId) {
        return ResponseEntity.ok(products.catalog(categoryId, maxPrice, name, Math.min(limit, 100), cursorPrice, cursorId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Product> byId(@PathVariable long id) {
        return ResponseEntity.ok(products.findById(id).orElseThrow(() -> new IllegalArgumentException("Product not found")));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Product> create(@Valid @RequestBody ProductRequest req) {
        long id = products.insert(req.categoryId(), req.sku(), req.name(), req.price(), req.stock());
        return ResponseEntity.ok(products.findById(id).orElseThrow());
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Product> update(@PathVariable long id, @RequestBody ProductRequest req) {
        products.update(id, req.name(), req.price(), req.stock(), null);
        return ResponseEntity.ok(products.findById(id).orElseThrow());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        products.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats/top")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> top(@RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(products.topSelling(limit));
    }

    @GetMapping("/categories")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<List<Map<String, Object>>> categories() {
        return ResponseEntity.ok(products.categoriesWithCounts());
    }
}
