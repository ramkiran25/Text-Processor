package com.textprocessor.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalised configuration for the text processing pipeline.
 *
 * <p>Bound from {@code application.yml} under the {@code app.processing} prefix:
 *
 * <pre>
 * app:
 *   processing:
 *     simulate-delay: false      # set true in dev to test frontend spinner
 *     simulate-delay-ms: 3000
 *   cors:
 *     allowed-origins: http://localhost:4200
 * </pre>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.processing")
public class ProcessingProperties {

    /** When {@code true}, an artificial delay is injected between the two passes (dev only). */
    private boolean simulateDelay = false;

    /** Duration of the artificial delay in milliseconds. Ignored when {@code simulateDelay} is false. */
    private long simulateDelayMs = 3000;
}