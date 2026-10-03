package uz.uysot.tester.model;

public class ApiErrorDetails {
    private String testNodeId;
    private String requestMethod;
    private String requestUrl;
    private String params;
    private String requestBody;
    private int statusCode;
    private String responseBody;

    public ApiErrorDetails() {
    }

    public ApiErrorDetails(String testNodeId, String requestMethod, String requestUrl, String params, String requestBody, int statusCode, String responseBody) {
        this.testNodeId = testNodeId;
        this.requestMethod = requestMethod;
        this.requestUrl = requestUrl;
        this.params = params;
        this.requestBody = requestBody;
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public String getTestNodeId() {
        return testNodeId;
    }

    public void setTestNodeId(String testNodeId) {
        this.testNodeId = testNodeId;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }
}
