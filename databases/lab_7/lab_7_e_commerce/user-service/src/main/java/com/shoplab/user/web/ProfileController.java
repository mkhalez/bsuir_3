package com.shoplab.user.web;

import com.shoplab.user.domain.Address;
import com.shoplab.user.domain.Profile;
import com.shoplab.user.dto.AddressRequest;
import com.shoplab.user.dto.UpdateProfileRequest;
import com.shoplab.user.repo.AddressJdbcRepository;
import com.shoplab.user.service.ProfileService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService service;
    private final AddressJdbcRepository addresses;

    private static long userId(Jwt jwt) {
        Object v = jwt.getClaim("userId");
        return v == null ? -1 : Long.parseLong(v.toString());
    }

    @GetMapping("/by-email")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Profile> byEmail(@RequestParam String email) {
        return ResponseEntity.ok(service.getByEmail(email));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @profileAuthChecker.isOwner(#id, authentication)")
    public ResponseEntity<Profile> byId(@PathVariable long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Profile>> search(@RequestParam(required = false) String firstName,
                                                @RequestParam(required = false) String lastName,
                                                @RequestParam(defaultValue = "20") int limit,
                                                @RequestParam(required = false) Long cursor) {
        return ResponseEntity.ok(service.search(firstName, lastName, Math.min(limit, 100), cursor));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @profileAuthChecker.isOwner(#id, authentication)")
    public ResponseEntity<Profile> update(@PathVariable long id, @RequestBody UpdateProfileRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable long id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> activate(@PathVariable long id) {
        service.activate(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/addresses")
    @PreAuthorize("hasRole('ADMIN') or @profileAuthChecker.isOwner(#id, authentication)")
    public ResponseEntity<List<Address>> addressesOf(@PathVariable long id) {
        return ResponseEntity.ok(addresses.findByProfileId(id));
    }

    @PostMapping("/{id}/addresses")
    @PreAuthorize("hasRole('ADMIN') or @profileAuthChecker.isOwner(#id, authentication)")
    public ResponseEntity<Address> addAddress(@PathVariable long id, @Valid @RequestBody AddressRequest req) {
        long aid = addresses.insert(id, req.city(), req.street(), req.zip(), req.isDefault());
        return ResponseEntity.ok(addresses.findById(aid).orElseThrow());
    }
}
