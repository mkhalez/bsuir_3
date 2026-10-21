package com.shoplab.auth.config;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.keys")
public record RsaKeyProperties(RSAPublicKey publicKey, RSAPrivateKey privateKey, String kid) {
}
