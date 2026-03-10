
package src.main.java.com.example.multithreading.controller;

import com.example.main.model.OrchestratorResponse;
import src.main.java.com.example.multithreading.service.Parallelcallservice;
import src.main.java.com.example.multithreading.service.SequentialCallService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OrchestratorController exposes two endpoints:
 *
 *  GET /orchestrator/parallel   → calls all 4 child services IN PARALLEL
 *  GET /orchestrator/sequential → calls all 4 child services ONE BY ONE
 *
 * Compare the totalTimeTakenMs in both responses to see the speedup!
 */
@Slf4j
@RestController
@RequestMapping("/orchestrator")
@RequiredArgsConstructor
public class MultithreadingController {

    private final ParallelCallService parallelCallService;
    private final SequentialCallService sequentialCallService;

    /**
     * PARALLEL call endpoint.
     * All 4 child services are invoked simultaneously on separate threads.
     * Expected total time ≈ max(child response times).
     */
    @GetMapping("/parallel")
    public ResponseEntity<OrchestratorResponse> parallelCall() {
        log.info("Received request for PARALLEL processing");
        OrchestratorResponse response = parallelCallService.callAllChildServicesInParallel();
        return ResponseEntity.ok(response);
    }

    /**
     * SEQUENTIAL call endpoint.
     * Each child service is called one after another on the same thread.
     * Expected total time ≈ sum(child response times).
     */
    @GetMapping("/sequential")
    public ResponseEntity<OrchestratorResponse> sequentialCall() {
        log.info("Received request for SEQUENTIAL processing");
        OrchestratorResponse response = sequentialCallService.callAllChildServicesSequentially();
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Main Orchestrator Service is UP on port 8080");
    }
}
