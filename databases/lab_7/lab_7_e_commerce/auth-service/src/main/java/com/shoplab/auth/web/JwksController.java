package com.shoplab.auth.web;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.shoplab.auth.config.RsaKeyProperties;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class JwksController {

    private final RsaKeyProperties keys;

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() throws Exception {
        RSAKey key = new RSAKey.Builder(keys.publicKey()).keyID(keys.kid()).build();
        return new JWKSet(key).toJSONObject();
    }
}
