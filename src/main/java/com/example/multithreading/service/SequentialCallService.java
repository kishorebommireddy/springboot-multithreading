package src.main.java.com.example.multithreading.service;

import com.example.main.model.ChildResponse;
import com.example.main.model.OrchestratorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Demonstrates SEQUENTIAL calls to all 4 child services one by one.
 *
 * How it works:
 *  - Each child service is called in a simple for-loop on the main thread.
 *  - The next call starts only after the previous one completes.
 *  - Total time ≈ sum of all individual response times.
 *  - Compare this with the parallel version to see the difference!
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SequentialCallService {

    private final RestTemplate restTemplate;

    // Child service base URLs (ports 8081-8084)
    private static final List<String> CHILD_URLS = List.of(
            "http://localhost:8081/child/process",
            "http://localhost:8082/child/process",
            "http://localhost:8083/child/process",
            "http://localhost:8084/child/process"
    );

    public OrchestratorResponse callAllChildServicesSequentially() {
        log.info("=== Starting SEQUENTIAL calls to {} child services ===", CHILD_URLS.size());
        long overallStart = System.currentTimeMillis();

        List<ChildResponse> results = new ArrayList<>();
        Map<String, Long> individualTimings = new HashMap<>();

        // Call each child service one at a time, in order
        for (int i = 0; i < CHILD_URLS.size(); i++) {
            String url = CHILD_URLS.get(i);
            String serviceName = "child-service-" + (i + 1);

            log.info("[{}] Sending request on thread: {}", serviceName, Thread.currentThread().getName());
            long start = System.currentTimeMillis();

            try {
                ChildResponse response = restTemplate.getForObject(url, ChildResponse.class);
                long elapsed = System.currentTimeMillis() - start;
                individualTimings.put(serviceName, elapsed);
                log.info("[{}] Completed in {}ms", serviceName, elapsed);
                results.add(response);
            } catch (Exception e) {
                long elapsed = System.currentTimeMillis() - start;
                individualTimings.put(serviceName, elapsed);
                log.error("[{}] Failed after {}ms: {}", serviceName, elapsed, e.getMessage());
                results.add(ChildResponse.builder()
                        .serviceName(serviceName)
                        .message("ERROR: " + e.getMessage())
                        .status("FAILED")
                        .processingTimeMs(elapsed)
                        .build());
            }
        }

        long totalTime = System.currentTimeMillis() - overallStart;
        log.info("=== SEQUENTIAL calls completed. Total time: {}ms (sum of individual timings) ===", totalTime);

        return OrchestratorResponse.builder()
                .callType("SEQUENTIAL")
                .totalTimeTakenMs(totalTime)
                .results(results)
                .status("SUCCESS")
                .individualTimings(individualTimings)
                .build();
    }
}