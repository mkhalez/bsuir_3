package com.shoplab.auth.web;

import com.shoplab.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalController {

    private final AuthService auth;

    @PatchMapping("/{id}/disable")
    public ResponseEntity<Void> disable(@PathVariable long id) {
        auth.setActive(id, false);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/enable")
    public ResponseEntity<Void> enable(@PathVariable long id) {
        auth.setActive(id, true);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        auth.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
