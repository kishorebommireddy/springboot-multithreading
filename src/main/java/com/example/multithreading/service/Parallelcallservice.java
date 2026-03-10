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
import java.util.concurrent.*;

/**
 * Demonstrates PARALLEL calls to all 4 child services simultaneously.
 *
 * How it works:
 *  - Each child service call is submitted as a CompletableFuture task
 *    on a shared ExecutorService (fixed thread pool of 4 threads).
 *  - All 4 futures are launched at roughly the same time.
 *  - CompletableFuture.allOf(...).join() blocks until ALL of them finish.
 *  - Total time ≈ max(individual response times), NOT the sum.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Parallelcallservice {

    private final RestTemplate restTemplate;
    private final ExecutorService parallelExecutor;

    // Child service base URLs  (ports 8081-8084)
    private static final List<String> CHILD_URLS = List.of(
            "http://localhost:8081/child/process",
            "http://localhost:8082/child/process",
            "http://localhost:8083/child/process",
            "http://localhost:8084/child/process"
    );

    public OrchestratorResponse callAllChildServicesInParallel() {
        log.info("=== Starting PARALLEL calls to {} child services ===", CHILD_URLS.size());
        long overallStart = System.currentTimeMillis();

        // 1. Create a CompletableFuture for each child service
        List<CompletableFuture<ChildResponse>> futures = new ArrayList<>();
        Map<String, Long> individualTimings = new ConcurrentHashMap<>();

        for (int i = 0; i < CHILD_URLS.size(); i++) {
            final String url = CHILD_URLS.get(i);
            final String serviceName = "child-service-" + (i + 1);

            CompletableFuture<ChildResponse> future = CompletableFuture.supplyAsync(() -> {
                log.info("[{}] Sending request on thread: {}", serviceName, Thread.currentThread().getName());
                long start = System.currentTimeMillis();

                try {
                    ChildResponse response = restTemplate.getForObject(url, ChildResponse.class);
                    long elapsed = System.currentTimeMillis() - start;
                    individualTimings.put(serviceName, elapsed);
                    log.info("[{}] Completed in {}ms", serviceName, elapsed);
                    return response;
                } catch (Exception e) {
                    long elapsed = System.currentTimeMillis() - start;
                    individualTimings.put(serviceName, elapsed);
                    log.error("[{}] Failed after {}ms: {}", serviceName, elapsed, e.getMessage());
                    return ChildResponse.builder()
                            .serviceName(serviceName)
                            .message("ERROR: " + e.getMessage())
                            .status("FAILED")
                            .processingTimeMs(elapsed)
                            .build();
                }
            }, parallelExecutor);

            futures.add(future);
        }

        // 2. Wait for ALL futures to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );
        allFutures.join(); // blocks here until every future is done

        // 3. Collect results
        List<ChildResponse> results = new ArrayList<>();
        for (CompletableFuture<ChildResponse> future : futures) {
            try {
                results.add(future.get());
            } catch (InterruptedException | ExecutionException e) {
                log.error("Error retrieving future result: {}", e.getMessage());
                Thread.currentThread().interrupt();
            }
        }

        long totalTime = System.currentTimeMillis() - overallStart;
        log.info("=== PARALLEL calls completed. Total time: {}ms ===", totalTime);

        return OrchestratorResponse.builder()
                .callType("PARALLEL")
                .totalTimeTakenMs(totalTime)
                .results(results)
                .status("SUCCESS")
                .individualTimings(individualTimings)
                .build();
    }
}