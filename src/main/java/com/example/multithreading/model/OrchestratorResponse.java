package src.main.java.com.example.multithreading.model;

public class OrchestratorResponse {
    private String callType;               // "PARALLEL" or "SEQUENTIAL"
    private long totalTimeTakenMs;
    private List<ChildResponse> results;   // contains all 4 ChildResponse objects
    private String status;
    private Map<String, Long> individualTimings;
}