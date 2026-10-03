package uz.uysot.tester.model;

public class TestRunResult {
    public enum Status {
        PASSED,
        FAILED,
        SKIPPED,
        RUNNING
    }

    private String name;
    private Status status;
    private String duration;
    private ApiErrorDetails errorDetails;
    private String rawOutput;

    public TestRunResult(String name, Status status) {
        this.name = name;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public ApiErrorDetails getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(ApiErrorDetails errorDetails) {
        this.errorDetails = errorDetails;
    }

    public String getRawOutput() {
        return rawOutput;
    }

    public void setRawOutput(String rawOutput) {
        this.rawOutput = rawOutput;
    }

    @Override
    public String toString() {
        String icon = status == Status.PASSED ? "✅" : (status == Status.FAILED ? "❌" : "⚠️");
        return icon + " " + name;
    }
}
