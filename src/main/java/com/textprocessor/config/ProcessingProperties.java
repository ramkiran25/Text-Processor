package com.textprocessor.config;

import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;

/**
 * Externalised configuration for the text processing pipeline.

 *   cors:
 *     allowed-origins: http://localhost:4200
 * </pre>
 */
@Getter
@Setter
@Component
public class ProcessingProperties {

    /** When {@code true}, an artificial delay is injected between the two passes (dev only). */
    private boolean simulateDelay = false;

    /** Duration of the artificial delay in milliseconds.*/
    private long simulateDelayMs = 3000;
}