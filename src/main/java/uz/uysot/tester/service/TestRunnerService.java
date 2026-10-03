package uz.uysot.tester.service;

import com.intellij.openapi.diagnostic.Logger;
import uz.uysot.tester.model.ApiErrorDetails;
import uz.uysot.tester.model.TestRunResult;
import uz.uysot.tester.state.UysotSettingsState;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestRunnerService {
    private static final Logger LOG = Logger.getInstance(TestRunnerService.class);
    private static Process currentProcess = null;

    public interface TestRunCallback {
        void onOutput(String text);
        void onTestStarted(String testName);
        void onTestFinished(TestRunResult result);
        void onRunCompleted(int total, int passed, int failed, int skipped);
    }

    public static synchronized void stopCurrentRun() {
        if (currentProcess != null && currentProcess.isAlive()) {
            currentProcess.destroyForcibly();
            currentProcess = null;
        }
    }

    public static void runTests(String pytestArgs, TestRunCallback callback) {
        new Thread(() -> {
            UysotSettingsState settings = UysotSettingsState.getInstance();
            File testDir = new File(settings.clonePath);

            if (!testDir.exists() || !new File(testDir, ".git").exists()) {
                callback.onOutput("Xatolik: Test loyihasi hali yuklanmagan. Avval 'Testlarni yuklash/yangilash' tugmasini bosing.");
                callback.onRunCompleted(0, 0, 0, 0);
                return;
            }

            File venvDir = new File(testDir, ".venv");
            File pythonBin = new File(venvDir, "bin/python");
            File pytestBin = new File(venvDir, "bin/pytest");

            if (!pytestBin.exists()) {
                callback.onOutput("Python virtual muhit yaratilmoqda (.venv)...");
                try {
                    Process venvProc = new ProcessBuilder("python3", "-m", "venv", ".venv")
                            .directory(testDir)
                            .redirectErrorStream(true)
                            .start();
                    streamOutput(venvProc, callback);
                    venvProc.waitFor();

                    callback.onOutput("Kutubxonalar o'rnatilmoqda (pip install -r requirements.txt)...");
                    Process pipProc = new ProcessBuilder(pythonBin.getAbsolutePath(), "-m", "pip", "install", "-r", "requirements.txt")
                            .directory(testDir)
                            .redirectErrorStream(true)
                            .start();
                    streamOutput(pipProc, callback);
                    pipProc.waitFor();
                } catch (Exception e) {
                    LOG.error("Failed to setup virtualenv", e);
                    callback.onOutput("Virtual muhit o'rnatishda xatolik: " + e.getMessage());
                    callback.onRunCompleted(0, 0, 0, 0);
                    return;
                }
            }

            callback.onOutput("\n=======================================================");
            callback.onOutput("🚀 Testlar boshlanmoqda...");
            callback.onOutput("🌐 Base URL: " + settings.baseUrl);
            callback.onOutput("🎯 Parametrlar: " + pytestArgs);
            callback.onOutput("=======================================================\n");

            List<String> cmd = new ArrayList<>();
            cmd.add(pytestBin.getAbsolutePath());
            for (String arg : pytestArgs.split("\\s+")) {
                if (!arg.trim().isEmpty()) {
                    cmd.add(arg.trim());
                }
            }
            cmd.add("-v");

            Map<String, TestRunResult> testResults = new LinkedHashMap<>();

            try {
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.directory(testDir);
                pb.environment().put("BASE_URL", settings.baseUrl);
                if (settings.token != null && !settings.token.trim().isEmpty()) {
                    pb.environment().put("TOKEN", settings.token.trim());
                }

                pb.redirectErrorStream(true);
                currentProcess = pb.start();

                Pattern testPattern = Pattern.compile("^(pytest_uysot/[^\\s]+)\\s+(PASSED|FAILED|SKIPPED|ERROR)");

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(currentProcess.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        callback.onOutput(line);

                        Matcher matcher = testPattern.matcher(line.trim());
                        if (matcher.find()) {
                            String testName = matcher.group(1);
                            String statusStr = matcher.group(2);

                            TestRunResult.Status status = TestRunResult.Status.PASSED;
                            if ("FAILED".equals(statusStr) || "ERROR".equals(statusStr)) {
                                status = TestRunResult.Status.FAILED;
                            } else if ("SKIPPED".equals(statusStr)) {
                                status = TestRunResult.Status.SKIPPED;
                            }

                            TestRunResult result = new TestRunResult(testName, status);
                            testResults.put(testName, result);
                            callback.onTestFinished(result);
                        }
                    }
                }

                currentProcess.waitFor();
                currentProcess = null;

                // Parse logs/api_errors.log for detailed failure reasons
                File errorLog = new File(testDir, "logs/api_errors.log");
                if (errorLog.exists()) {
                    parseErrorLog(errorLog, testResults);
                }

                int passed = 0;
                int failed = 0;
                int skipped = 0;

                for (TestRunResult res : testResults.values()) {
                    if (res.getStatus() == TestRunResult.Status.PASSED) passed++;
                    else if (res.getStatus() == TestRunResult.Status.FAILED) failed++;
                    else if (res.getStatus() == TestRunResult.Status.SKIPPED) skipped++;
                }

                callback.onRunCompleted(testResults.size(), passed, failed, skipped);

            } catch (Exception e) {
                LOG.error("Test execution error", e);
                callback.onOutput("Ijro etishda xatolik: " + e.getMessage());
                callback.onRunCompleted(0, 0, 0, 0);
            }
        }).start();
    }

    private static void streamOutput(Process proc, TestRunCallback callback) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                callback.onOutput(line);
            }
        }
    }

    private static void parseErrorLog(File logFile, Map<String, TestRunResult> results) {
        try {
            List<String> lines = Files.readAllLines(logFile.toPath(), StandardCharsets.UTF_8);
            String currentTest = null;
            String request = null;
            String params = null;
            String body = null;
            int status = 0;
            StringBuilder responseBuilder = new StringBuilder();
            boolean readingResponse = false;

            for (String line : lines) {
                if (line.startsWith("Test:     ")) {
                    if (currentTest != null && results.containsKey(currentTest)) {
                        results.get(currentTest).setErrorDetails(
                                new ApiErrorDetails(currentTest, request, null, params, body, status, responseBuilder.toString().trim())
                        );
                    }
                    currentTest = line.substring("Test:     ".length()).trim();
                    request = null;
                    params = null;
                    body = null;
                    status = 0;
                    responseBuilder = new StringBuilder();
                    readingResponse = false;
                } else if (line.startsWith("Request:  ")) {
                    request = line.substring("Request:  ".length()).trim();
                } else if (line.startsWith("Params:   ")) {
                    params = line.substring("Params:   ".length()).trim();
                } else if (line.startsWith("Body:     ")) {
                    body = line.substring("Body:     ".length()).trim();
                } else if (line.startsWith("Status:   ")) {
                    try {
                        status = Integer.parseInt(line.substring("Status:   ".length()).trim());
                    } catch (NumberFormatException ignored) {}
                } else if (line.startsWith("Response: ")) {
                    readingResponse = true;
                    responseBuilder.append(line.substring("Response: ".length())).append("\n");
                } else if (readingResponse && !line.startsWith("=====")) {
                    responseBuilder.append(line).append("\n");
                }
            }

            if (currentTest != null && results.containsKey(currentTest)) {
                results.get(currentTest).setErrorDetails(
                        new ApiErrorDetails(currentTest, request, null, params, body, status, responseBuilder.toString().trim())
                );
            }

        } catch (Exception e) {
            LOG.warn("Failed to parse api_errors.log", e);
        }
    }
}
