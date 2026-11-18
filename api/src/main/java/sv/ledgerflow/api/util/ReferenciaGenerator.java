package sv.ledgerflow.api.util;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ReferenciaGenerator {
    public String nueva() {
        return "TRX-" + UUID.randomUUID().toString()
                .replace("-", "").substring(0, 20).toUpperCase();
    }
}
