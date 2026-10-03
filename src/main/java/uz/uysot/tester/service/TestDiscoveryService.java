package uz.uysot.tester.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.openapi.diagnostic.Logger;
import uz.uysot.tester.model.TestSuiteOption;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class TestDiscoveryService {
    private static final Logger LOG = Logger.getInstance(TestDiscoveryService.class);

    public static class DiscoveryResult {
        private final List<TestSuiteOption> suites;
        private final String defaultBaseUrl;
        private final String manifestName;
        private final String authType;
        private final String tokenLabel;

        public DiscoveryResult(List<TestSuiteOption> suites, String defaultBaseUrl, String manifestName, String authType, String tokenLabel) {
            this.suites = suites;
            this.defaultBaseUrl = defaultBaseUrl;
            this.manifestName = manifestName;
            this.authType = authType != null ? authType : "X-Auth-Token";
            this.tokenLabel = tokenLabel != null ? tokenLabel : "X-Auth-Token";
        }

        public List<TestSuiteOption> getSuites() {
            return suites;
        }

        public String getDefaultBaseUrl() {
            return defaultBaseUrl;
        }

        public String getManifestName() {
            return manifestName;
        }

        public String getAuthType() {
            return authType;
        }

        public String getTokenLabel() {
            return tokenLabel;
        }
    }

    public static DiscoveryResult discover(File repoDir) {
        if (repoDir == null || !repoDir.exists()) {
            return new DiscoveryResult(TestSuiteOption.getDefaultSuites(), null, null, "X-Auth-Token", "X-Auth-Token (Open API)");
        }

        // 1. Check for .tester.json or tester-config.json
        File configFile = new File(repoDir, ".tester.json");
        if (!configFile.exists()) {
            configFile = new File(repoDir, "tester-config.json");
        }

        if (configFile.exists()) {
            DiscoveryResult fromConfig = loadFromConfig(configFile, repoDir);
            if (fromConfig != null && !fromConfig.getSuites().isEmpty()) {
                return fromConfig;
            }
        }

        // 2. Fallback: Automatic Discovery from repo directory
        return autoDiscover(repoDir);
    }

    private static DiscoveryResult loadFromConfig(File configFile, File repoDir) {
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8)) {
            JsonObject json = new Gson().fromJson(reader, JsonObject.class);
            if (json == null) return null;

            String name = json.has("name") ? json.get("name").getAsString() : null;
            String defaultBaseUrl = json.has("defaultBaseUrl") ? json.get("defaultBaseUrl").getAsString() : null;
            String authType = json.has("authType") ? json.get("authType").getAsString() : "X-Auth-Token";
            String tokenLabel = json.has("tokenLabel") ? json.get("tokenLabel").getAsString() : authType;

            List<TestSuiteOption> suites = new ArrayList<>();
            if (json.has("suites") && json.get("suites").isJsonArray()) {
                JsonArray array = json.getAsJsonArray("suites");
                for (JsonElement el : array) {
                    if (el.isJsonObject()) {
                        JsonObject obj = el.getAsJsonObject();
                        String suiteName = obj.has("name") ? obj.get("name").getAsString() : "Test to'plami";
                        String params = obj.has("params") ? obj.get("params").getAsString() : "";
                        if (params.isEmpty() && obj.has("args")) {
                            params = obj.get("args").getAsString();
                        }
                        String desc = obj.has("description") ? obj.get("description").getAsString() : "";
                        suites.add(new TestSuiteOption(suiteName, params, desc));
                    }
                }
            }

            if (!suites.isEmpty()) {
                // Also scan repoDir for any new or unregistered test_*.py files so developers can run local tests immediately
                if (repoDir != null && repoDir.exists()) {
                    Set<String> registeredPaths = new HashSet<>();
                    for (TestSuiteOption opt : suites) {
                        if (opt.getPytestArgs() != null) {
                            for (String part : opt.getPytestArgs().split("\\s+")) {
                                registeredPaths.add(part.replace('\\', '/'));
                            }
                        }
                    }

                    List<File> localTestFiles = new ArrayList<>();
                    findTestPyFiles(repoDir, localTestFiles);
                    localTestFiles.sort(Comparator.comparing(File::getName));
                    for (File f : localTestFiles) {
                        String relPath = repoDir.toPath().relativize(f.toPath()).toString().replace('\\', '/');
                        if (!registeredPaths.contains(relPath)) {
                            String displayName = f.getName();
                            if (displayName.endsWith(".py")) {
                                displayName = displayName.substring(0, displayName.length() - 3);
                            }
                            suites.add(new TestSuiteOption("📄 [Lokal] " + displayName, relPath, "Yangi lokal test: " + relPath));
                        }
                    }
                }

                return new DiscoveryResult(suites, defaultBaseUrl, name, authType, tokenLabel);
            }
        } catch (Exception e) {
            LOG.warn("Error reading tester config: " + configFile.getAbsolutePath(), e);
        }
        return null;
    }

    private static DiscoveryResult autoDiscover(File repoDir) {
        List<TestSuiteOption> suites = new ArrayList<>();

        // Detect default auth type for this repo
        String authType = "X-Auth-Token";
        String tokenLabel = "X-Auth-Token (Open API)";

        String repoDirName = repoDir.getName().toLowerCase();
        if (!repoDirName.contains("uysot") && !repoDirName.contains("open-api") && !repoDirName.contains("openapi")) {
            authType = "Bearer";
            tokenLabel = "Bearer Token";
        }

        // Potential test folders
        List<String> candidateDirNames = Arrays.asList("pytest_uysot", "tests", "test", "api_tests");
        File mainTestDir = null;
        String testDirRelative = null;

        for (String c : candidateDirNames) {
            File d = new File(repoDir, c);
            if (d.exists() && d.isDirectory()) {
                mainTestDir = d;
                testDirRelative = c;
                break;
            }
        }

        if (mainTestDir != null) {
            suites.add(new TestSuiteOption("🚀 Barcha testlar (" + testDirRelative + ")", testDirRelative, "Barcha testlarni to'liq ishga tushirish"));

            // Check subdirectories
            File[] subDirs = mainTestDir.listFiles(File::isDirectory);
            if (subDirs != null) {
                Arrays.sort(subDirs, Comparator.comparing(File::getName));
                for (File subDir : subDirs) {
                    if (subDir.getName().startsWith(".") || subDir.getName().startsWith("__")) continue;
                    File[] pyFiles = subDir.listFiles((dir, name) -> name.endsWith(".py"));
                    if (pyFiles != null && pyFiles.length > 0) {
                        String relPath = testDirRelative + "/" + subDir.getName();
                        suites.add(new TestSuiteOption("📁 " + subDir.getName() + " papkasi", relPath, "Modul: " + relPath));
                    }
                }
            }

            // Find all test_*.py files recursively or in main directory
            List<File> testFiles = new ArrayList<>();
            findTestPyFiles(mainTestDir, testFiles);
            testFiles.sort(Comparator.comparing(File::getName));

            for (File file : testFiles) {
                String relPath = repoDir.toPath().relativize(file.toPath()).toString().replace('\\', '/');
                String displayName = file.getName();
                if (displayName.endsWith(".py")) {
                    displayName = displayName.substring(0, displayName.length() - 3);
                }
                suites.add(new TestSuiteOption("📄 " + displayName, relPath, "Fayl: " + relPath));
            }
        } else {
            // No standard folder, search whole repo for test_*.py
            List<File> testFiles = new ArrayList<>();
            findTestPyFiles(repoDir, testFiles);
            if (!testFiles.isEmpty()) {
                suites.add(new TestSuiteOption("🚀 Barcha testlar", ".", "Barcha aniqlangan testlar"));
                testFiles.sort(Comparator.comparing(File::getName));
                for (File file : testFiles) {
                    String relPath = repoDir.toPath().relativize(file.toPath()).toString().replace('\\', '/');
                    suites.add(new TestSuiteOption("📄 " + file.getName(), relPath, "Fayl: " + relPath));
                }
            }
        }

        // Check for pytest markers in pytest.ini
        File pytestIni = new File(repoDir, "pytest.ini");
        if (pytestIni.exists()) {
            try {
                List<String> lines = Files.readAllLines(pytestIni.toPath(), StandardCharsets.UTF_8);
                boolean inMarkers = false;
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("markers =") || trimmed.startsWith("markers=")) {
                        inMarkers = true;
                        String after = trimmed.substring(trimmed.indexOf('=') + 1).trim();
                        addMarkerIfValid(after, suites);
                    } else if (inMarkers) {
                        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                            inMarkers = false;
                        } else if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                            addMarkerIfValid(trimmed, suites);
                        }
                    }
                }
            } catch (Exception e) {
                LOG.debug("Error reading pytest.ini markers", e);
            }
        }

        if (suites.isEmpty()) {
            return new DiscoveryResult(TestSuiteOption.getDefaultSuites(), null, null, authType, tokenLabel);
        }

        return new DiscoveryResult(suites, null, null, authType, tokenLabel);
    }

    private static void addMarkerIfValid(String markerLine, List<TestSuiteOption> suites) {
        if (markerLine == null || markerLine.trim().isEmpty()) return;
        String[] parts = markerLine.split(":", 2);
        String markerName = parts[0].trim().split("\\s+")[0];
        if (markerName.isEmpty()) return;
        String markerDesc = parts.length > 1 ? parts[1].trim() : "Marker: " + markerName;
        suites.add(new TestSuiteOption("🏷️ Marker: " + markerName, "-m " + markerName, markerDesc));
    }

    private static void findTestPyFiles(File dir, List<File> result) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File f : files) {
            String name = f.getName();
            if (name.startsWith(".") || name.startsWith("__") || "venv".equals(name) || ".venv".equals(name)) {
                continue;
            }
            if (f.isDirectory()) {
                findTestPyFiles(f, result);
            } else if (f.isFile() && (name.startsWith("test_") || name.endsWith("_test.py")) && name.endsWith(".py")) {
                result.add(f);
            }
        }
    }
}
