package uz.uysot.tester.service;

import com.intellij.openapi.diagnostic.Logger;
import uz.uysot.tester.model.ApiErrorDetails;
import uz.uysot.tester.model.TestRunResult;

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

    public static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    public static synchronized void stopCurrentRun() {
        if (currentProcess != null && currentProcess.isAlive()) {
            currentProcess.destroyForcibly();
            currentProcess = null;
        }
    }

    public static File getVenvPython(File venvDir) {
        if (venvDir == null) return null;
        boolean win = isWindows();

        if (win) {
            File winExe = new File(venvDir, "Scripts/python.exe");
            if (winExe.exists()) return winExe;
            File winPy = new File(venvDir, "Scripts/python");
            if (winPy.exists()) return winPy;
        }

        File unixPy = new File(venvDir, "bin/python");
        if (unixPy.exists()) return unixPy;
        File unixPy3 = new File(venvDir, "bin/python3");
        if (unixPy3.exists()) return unixPy3;
        File unixPyExe = new File(venvDir, "bin/python.exe");
        if (unixPyExe.exists()) return unixPyExe;

        // Fallback default according to OS
        return win ? new File(venvDir, "Scripts/python.exe") : new File(venvDir, "bin/python");
    }

    public static File getVenvPytest(File venvDir) {
        if (venvDir == null) return null;
        boolean win = isWindows();

        if (win) {
            File winExe = new File(venvDir, "Scripts/pytest.exe");
            if (winExe.exists()) return winExe;
            File winPytest = new File(venvDir, "Scripts/pytest");
            if (winPytest.exists()) return winPytest;
        }

        File unixPytest = new File(venvDir, "bin/pytest");
        if (unixPytest.exists()) return unixPytest;
        File unixPytestExe = new File(venvDir, "bin/pytest.exe");
        if (unixPytestExe.exists()) return unixPytestExe;

        return win ? new File(venvDir, "Scripts/pytest.exe") : new File(venvDir, "bin/pytest");
    }

    public static List<String> findSystemPython() {
        boolean win = isWindows();
        List<List<String>> candidates = new ArrayList<>();

        if (win) {
            candidates.add(Collections.singletonList("python"));
            candidates.add(Arrays.asList("py", "-3"));
            candidates.add(Collections.singletonList("py"));
            candidates.add(Collections.singletonList("python3"));

            // Check py.exe in standard Windows locations
            File py1 = new File("C:/Windows/py.exe");
            if (py1.exists()) {
                candidates.add(Arrays.asList(py1.getAbsolutePath(), "-3"));
                candidates.add(Collections.singletonList(py1.getAbsolutePath()));
            }
            File py2 = new File("C:/Windows/System32/py.exe");
            if (py2.exists()) {
                candidates.add(Arrays.asList(py2.getAbsolutePath(), "-3"));
                candidates.add(Collections.singletonList(py2.getAbsolutePath()));
            }

            // Scan standard Windows installation paths if PATH is not configured
            List<File> searchDirs = new ArrayList<>();
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null) {
                searchDirs.add(new File(localAppData, "Programs/Python"));
            }
            String appData = System.getenv("APPDATA");
            if (appData != null) {
                searchDirs.add(new File(appData, "Local/Programs/Python"));
            }
            String userHome = System.getProperty("user.home");
            if (userHome != null) {
                searchDirs.add(new File(userHome, "AppData/Local/Programs/Python"));
            }
            searchDirs.add(new File("C:/Program Files"));
            searchDirs.add(new File("C:/Program Files (x86)"));
            searchDirs.add(new File("C:/Program Files/Python"));
            searchDirs.add(new File("C:/ProgramData/chocolatey/bin"));
            searchDirs.add(new File("C:/"));

            for (File baseDir : searchDirs) {
                if (baseDir.exists() && baseDir.isDirectory()) {
                    File[] subDirs = baseDir.listFiles();
                    if (subDirs != null) {
                        for (File sub : subDirs) {
                            if (sub.isDirectory() && sub.getName().toLowerCase().startsWith("python3")) {
                                File exe = new File(sub, "python.exe");
                                if (exe.exists()) {
                                    candidates.add(Collections.singletonList(exe.getAbsolutePath()));
                                }
                            } else if (sub.isFile() && "python.exe".equalsIgnoreCase(sub.getName())) {
                                candidates.add(Collections.singletonList(sub.getAbsolutePath()));
                            }
                        }
                    }
                }
            }
        } else {
            candidates.add(Collections.singletonList("/opt/homebrew/bin/python3"));
            candidates.add(Collections.singletonList("/usr/local/bin/python3"));
            candidates.add(Collections.singletonList("python3"));
            candidates.add(Collections.singletonList("python"));
            candidates.add(Collections.singletonList("/usr/bin/python3"));
        }

        for (List<String> cmd : candidates) {
            if (verifyPython3(cmd)) {
                return cmd;
            }
        }

        return null;
    }

    private static boolean verifyPython3(List<String> cmd) {
        try {
            List<String> testCmd = new ArrayList<>(cmd);
            testCmd.add("--version");
            Process p = new ProcessBuilder(testCmd).start();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line).append(" ");
            }
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) sb.append(line).append(" ");
            }
            int code = p.waitFor();
            if (code == 0) {
                String output = sb.toString().trim();
                return output.contains("Python 3") || output.contains("Python 3.");
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static boolean isPytestInstalled(File pythonBin) {
        if (pythonBin == null || !pythonBin.exists()) return false;
        try {
            Process p = new ProcessBuilder(pythonBin.getAbsolutePath(), "-m", "pytest", "--version").start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static void deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteDirectory(f);
                } else {
                    f.delete();
                }
            }
        }
        dir.delete();
    }

    public static void runTests(File testDir, String baseUrl, String token, String pytestArgs, TestRunCallback callback) {
        new Thread(() -> {
            if (testDir == null || !testDir.exists() || !new File(testDir, ".git").exists()) {
                callback.onOutput("Xatolik: Test repozitoriyasi hali lokalga yuklanmagan. Avval '🔗 Connect & Sync' tugmasini bosing.");
                callback.onRunCompleted(0, 0, 0, 0);
                return;
            }

            File venvDir = new File(testDir, ".venv");
            File pythonBin = getVenvPython(venvDir);
            File pytestBin = getVenvPytest(venvDir);

            boolean isEnvReady = venvDir.exists() && pythonBin != null && pythonBin.exists() &&
                    (pytestBin != null && pytestBin.exists() || isPytestInstalled(pythonBin));

            if (!isEnvReady) {
                // If .venv exists but python binary is missing or broken, clean it up
                if (venvDir.exists() && (pythonBin == null || !pythonBin.exists())) {
                    callback.onOutput("Eski yoki chala o'rnatilgan .venv papkasi tozalanmoqda...");
                    deleteDirectory(venvDir);
                }

                List<String> systemPython = findSystemPython();
                if (systemPython == null || systemPython.isEmpty()) {
                    if (isWindows()) {
                        callback.onOutput("⚠️ Windows tizimida Python 3 topilmadi.");
                        callback.onOutput("Python o'rnatish tanlovi ochilmoqda...");

                        int choice = PythonInstallerService.promptInstallationChoice(null);

                        boolean installAttempted = false;
                        if (choice == PythonInstallerService.CHOICE_WINGET) {
                            installAttempted = true;
                            callback.onOutput("\n=======================================================");
                            callback.onOutput("⚡ 1-variant tanlandi: Winget orqali avtomatik o'rnatish");
                            callback.onOutput("=======================================================");
                            boolean ok = PythonInstallerService.installViaWinget(callback::onOutput);
                            if (!ok) {
                                callback.onOutput("\n⚠️ Winget o'rnatish yakunlanmadi. 2-variant (rasmiy installer) sinab ko'rilmoqda...");
                                PythonInstallerService.downloadAndInstallOfficial(callback::onOutput);
                            }
                        } else if (choice == PythonInstallerService.CHOICE_OFFICIAL_INSTALLER) {
                            installAttempted = true;
                            callback.onOutput("\n=======================================================");
                            callback.onOutput("📥 2-variant tanlandi: python.org rasmiy installerini yuklab olish");
                            callback.onOutput("=======================================================");
                            PythonInstallerService.downloadAndInstallOfficial(callback::onOutput);
                        } else {
                            callback.onOutput("❌ Foydalanuvchi tomonidan Python o'rnatish bekor qilindi.");
                            callback.onOutput("Python 3 ni qo'lda o'rnatish uchun: https://www.python.org/downloads/");
                            callback.onOutput("Eslatma: O'rnatish paytida 'Add python.exe to PATH' belgisini qo'yishni unutmang.");
                            callback.onRunCompleted(0, 0, 0, 0);
                            return;
                        }

                        if (installAttempted) {
                            callback.onOutput("\n🔍 Yangi o'rnatilgan Python tekshirilmoqda...");
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException ignored) {}
                            systemPython = findSystemPython();

                            if (systemPython != null && !systemPython.isEmpty()) {
                                callback.onOutput("🎉 Python muvaffaqiyatli aniqlandi: " + String.join(" ", systemPython) + "\n");
                            } else {
                                callback.onOutput("❌ Python o'rnatildi, biroq tizim muhit o'zgaruvchilarini (PATH) yangilash uchun " +
                                        "IntelliJ IDEA ni qayta ishga tushirish (Restart IDE) talab etilishi mumkin.");
                                callback.onRunCompleted(0, 0, 0, 0);
                                return;
                            }
                        }
                    } else {
                        String osHelp = "Tizimda Python 3 topilmadi! Iltimos, Python 3 ni o'rnating.\n"
                                + "• macOS: brew install python3\n"
                                + "• Linux: sudo apt install python3 python3-venv python3-pip";
                        callback.onOutput("❌ Xatolik: " + osHelp);
                        callback.onRunCompleted(0, 0, 0, 0);
                        return;
                    }
                }

                callback.onOutput("Python virtual muhit yaratilmoqda (.venv)...");
                callback.onOutput("Ishlatilayotgan Python: " + String.join(" ", systemPython));

                try {
                    List<String> venvCmd = new ArrayList<>(systemPython);
                    venvCmd.add("-m");
                    venvCmd.add("venv");
                    venvCmd.add(".venv");

                    Process venvProc = new ProcessBuilder(venvCmd)
                            .directory(testDir)
                            .redirectErrorStream(true)
                            .start();
                    streamOutput(venvProc, callback);
                    int venvExit = venvProc.waitFor();

                    if (venvExit != 0) {
                        callback.onOutput("❌ Xatolik: Virtual muhit yaratishda xato yuz berdi (Exit code: " + venvExit + ")");
                        callback.onRunCompleted(0, 0, 0, 0);
                        return;
                    }

                    // Re-resolve python binary inside newly created .venv
                    pythonBin = getVenvPython(venvDir);
                    if (pythonBin == null || !pythonBin.exists()) {
                        callback.onOutput("❌ Xatolik: Virtual muhit yaratildi, ammo python fayli topilmadi: " +
                                (pythonBin != null ? pythonBin.getAbsolutePath() : "null"));
                        callback.onRunCompleted(0, 0, 0, 0);
                        return;
                    }

                    File reqFile = new File(testDir, "requirements.txt");
                    List<String> pipCmd = new ArrayList<>();
                    pipCmd.add(pythonBin.getAbsolutePath());
                    pipCmd.add("-m");
                    pipCmd.add("pip");
                    pipCmd.add("install");

                    if (reqFile.exists()) {
                        callback.onOutput("Kutubxonalar o'rnatilmoqda (pip install -r requirements.txt)...");
                        pipCmd.add("-r");
                        pipCmd.add("requirements.txt");
                    } else {
                        callback.onOutput("Kutubxonalar o'rnatilmoqda (pytest, requests, allure-pytest, python-dotenv)...");
                        pipCmd.add("pytest");
                        pipCmd.add("requests");
                        pipCmd.add("allure-pytest");
                        pipCmd.add("python-dotenv");
                    }

                    Process pipProc = new ProcessBuilder(pipCmd)
                            .directory(testDir)
                            .redirectErrorStream(true)
                            .start();
                    streamOutput(pipProc, callback);
                    int pipExit = pipProc.waitFor();

                    if (pipExit != 0) {
                        callback.onOutput("❌ Xatolik: pip kutubxonalarni o'rnatishda xatolik yuz berdi (Exit code: " + pipExit + ")");
                        callback.onRunCompleted(0, 0, 0, 0);
                        return;
                    }

                    callback.onOutput("✅ Virtual muhit va kutubxonalar muvaffaqiyatli tayyorlandi!\n");

                } catch (Exception e) {
                    LOG.error("Failed to setup virtualenv", e);
                    callback.onOutput("Virtual muhit o'rnatishda kutilmagan xatolik: " + e.getMessage());
                    callback.onRunCompleted(0, 0, 0, 0);
                    return;
                }
            }

            // Always resolve python binary for running tests
            pythonBin = getVenvPython(venvDir);
            if (pythonBin == null || !pythonBin.exists()) {
                callback.onOutput("❌ Xatolik: Python interpretatori topilmadi (.venv/Scripts/python.exe yoki .venv/bin/python)");
                callback.onRunCompleted(0, 0, 0, 0);
                return;
            }

            callback.onOutput("\n=======================================================");
            callback.onOutput("🚀 Testlar boshlanmoqda...");
            callback.onOutput("📁 Repozitoriya: " + testDir.getName());
            callback.onOutput("🌐 Base URL: " + baseUrl);
            callback.onOutput("🎯 Parametrlar: " + pytestArgs);
            callback.onOutput("🐍 Python: " + pythonBin.getAbsolutePath());
            callback.onOutput("=======================================================\n");

            List<String> cmd = new ArrayList<>();
            cmd.add(pythonBin.getAbsolutePath());
            cmd.add("-m");
            cmd.add("pytest");
            if (pytestArgs != null && !pytestArgs.trim().isEmpty()) {
                for (String arg : pytestArgs.split("\\s+")) {
                    if (!arg.trim().isEmpty()) {
                        cmd.add(arg.trim());
                    }
                }
            }
            cmd.add("-v");
            cmd.add("--disable-warnings");

            Map<String, TestRunResult> testResults = new LinkedHashMap<>();

            try {
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.directory(testDir);
                if (baseUrl != null && !baseUrl.trim().isEmpty()) {
                    pb.environment().put("BASE_URL", baseUrl.trim());
                }
                if (token != null && !token.trim().isEmpty()) {
                    String cleanToken = token.trim();
                    pb.environment().put("TOKEN", cleanToken);
                    pb.environment().put("OPEN_API_TOKEN", cleanToken);
                    pb.environment().put("AUTH_TOKEN", cleanToken);
                    pb.environment().put("BEARER_TOKEN", cleanToken);
                }

                // Force UTF-8 encoding across Windows, Mac and Linux
                pb.environment().put("PYTHONIOENCODING", "utf-8");
                pb.environment().put("PYTHONUTF8", "1");

                // Silence legacy LibreSSL / OpenSSL warnings from urllib3
                pb.environment().put("PYTHONWARNINGS", "ignore:urllib3 v2 only supports OpenSSL:Warning");

                pb.redirectErrorStream(true);
                currentProcess = pb.start();

                // Universal test outcome pattern
                Pattern testPattern = Pattern.compile("^(.+?\\.py(?:::.*?)?)\\s+(PASSED|FAILED|SKIPPED|ERROR)");

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

                // Parse logs/api_errors.log for detailed failure reasons if available
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
