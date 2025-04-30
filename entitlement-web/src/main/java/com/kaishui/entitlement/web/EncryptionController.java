package com.kaishui.entitlement.web;


import com.kaishui.entitlement.entity.dto.CryptoRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1/api/crypto") // Choose an appropriate base path
@RequiredArgsConstructor
@Slf4j
public class EncryptionController {

    // Inject the specific StringEncryptor bean configured in JasyptConfig
    @Autowired
    private final StringEncryptor stringEncryptor;

    /**
     * Encrypts the provided plain text value.
     * <p>
     * IMPORTANT: Secure this endpoint appropriately in a real application.
     *
     * @param requestMono Mono containing the value to encrypt.
     * @return Mono containing the encrypted value.
     */
    @PostMapping("/encrypt")
    public Mono<ResponseEntity<CryptoRequest>> encrypt(@RequestBody Mono<CryptoRequest> requestMono) {
        return requestMono
                .map(request -> {
                    if (request.getValue() == null || request.getValue().isBlank()) {
                        throw new IllegalArgumentException("Value to encrypt cannot be null or blank.");
                    }
                    log.info("Encrypting value..."); // Avoid logging the actual value
                    String encryptedValue = stringEncryptor.encrypt(request.getValue());
                    // Optionally wrap with ENC() if needed for direct use in YAML,
                    // but often just the encrypted value is sufficient for API response.
                    // return new CryptoRequest("ENC(" + encryptedValue + ")");
                    return new CryptoRequest(encryptedValue);
                })
                .map(ResponseEntity::ok)
                .doOnError(IllegalArgumentException.class, e -> log.warn("Encryption failed: {}", e.getMessage()))
                .onErrorResume(IllegalArgumentException.class, e ->
                        Mono.just(ResponseEntity.badRequest().body(new CryptoRequest("Error: " + e.getMessage())))
                )
                .doOnError(e -> !(e instanceof IllegalArgumentException), e -> log.error("Encryption error: {}", e.getMessage(), e))
                .onErrorResume(e -> !(e instanceof IllegalArgumentException), e ->
                        Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CryptoRequest("Internal server error during encryption.")))
                );
    }

    /**
     * Decrypts the provided encrypted value.
     * Handles values potentially wrapped with ENC().
     * <p>
     * IMPORTANT: Secure this endpoint appropriately in a real application.
     *
     * @param requestMono Mono containing the value to decrypt.
     * @return Mono containing the decrypted value.
     */
    @PostMapping("/decrypt")
    public Mono<ResponseEntity<CryptoRequest>> decrypt(@RequestBody Mono<CryptoRequest> requestMono) {
        return requestMono
                .map(request -> {
                    if (request.getValue() == null || request.getValue().isBlank()) {
                        throw new IllegalArgumentException("Value to decrypt cannot be null or blank.");
                    }
                    String valueToDecrypt = request.getValue();
                    // Remove ENC() wrapper if present
                    if (valueToDecrypt.startsWith("ENC(") && valueToDecrypt.endsWith(")")) {
                        valueToDecrypt = valueToDecrypt.substring(4, valueToDecrypt.length() - 1);
                    }
                    log.info("Decrypting value...");
                    try {
                        String decryptedValue = stringEncryptor.decrypt(valueToDecrypt);
                        return new CryptoRequest(decryptedValue);
                    } catch (EncryptionOperationNotPossibleException e) {
                        log.warn("Decryption failed. Input might not be valid encrypted data or wrong key used.");
                        // Throw a specific exception type or handle differently
                        throw new IllegalArgumentException("Decryption failed. Invalid input or key.", e);
                    }
                })
                .map(ResponseEntity::ok)
                .doOnError(IllegalArgumentException.class, e -> log.warn("Decryption failed: {}", e.getMessage()))
                .onErrorResume(IllegalArgumentException.class, e ->
                        Mono.just(ResponseEntity.badRequest().body(new CryptoRequest("Error: " + e.getMessage())))
                )
                .doOnError(e -> !(e instanceof IllegalArgumentException), e -> log.error("Decryption error: {}", e.getMessage(), e))
                .onErrorResume(e -> !(e instanceof IllegalArgumentException), e ->
                        Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CryptoRequest("Internal server error during decryption.")))
                );
    }
}
