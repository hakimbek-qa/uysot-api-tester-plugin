package uz.uysot.tester.service;

import com.intellij.ide.actions.RevealFileAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class AiPromptService {
    private static final Logger LOG = Logger.getInstance(AiPromptService.class);

    public static String generatePrompt(File repoDir, String baseUrl, String authType) {
        StringBuilder sb = new StringBuilder();

        sb.append("# 🤖 AI TEST GENERATOR PROMPTI\n\n");
        sb.append("Siz tajribali QA Automation mutaxassisisiz. Quyidagi API endpoint uchun Python pytest test kodini yozib bering.\n\n");

        sb.append("## 1. REPOZITORIYA SOZLAMALARI:\n");
        sb.append("- Freymvork: pytest + requests\n");
        sb.append("- Server Base URL: ").append(baseUrl != null && !baseUrl.isEmpty() ? baseUrl : "https://openapi.app-dev.uysot.uz").append("\n");
        sb.append("- Autentifikatsiya turi: ").append(authType != null && !authType.isEmpty() ? authType : "X-Auth-Token").append("\n");
        sb.append("- Asosiy importlar:\n");
        sb.append("  ```python\n");
        sb.append("  import requests\n");
        sb.append("  import pytest\n");
        sb.append("  from config import BASE_URL, HEADERS\n");
        sb.append("  from helpers import check_success, wait_request\n");
        sb.append("  ```\n\n");

        sb.append("## 2. JAVOB VA XATOLIKLARNI TEKSHIRISH QOIDALARI:\n");
        sb.append("- Har bir testda `requests.get/post/put/delete` so'rovlari `headers=HEADERS` bilan yuboriladi.\n");
        sb.append("- So'rov muvaffaqiyatini tekshirish uchun: `data = check_success(response)` chaqiriladi.\n");
        sb.append("  `check_success` avtomatik ravishda `response.status_code == 200` va `accept is True` ekanini tekshiradi hamda ichki `data` obyektini qaytaradi.\n");
        sb.append("- Asinxron yozish operatsiyalari (masalan, lid yoki vazifa yaratish/o'chirish) uchun: `wait_request(response)` ishlatiladi.\n");
        sb.append("- Til: Barcha docstring, izohlar va assert xabarlari O'ZBEK tilida yozilishi shart.\n\n");

        // Include sample test if found
        String sampleCode = extractSampleTest(repoDir);
        if (sampleCode != null && !sampleCode.trim().isEmpty()) {
            sb.append("## 3. REPOZITORIYADAN NAMUNAVIY TEST:\n");
            sb.append("```python\n");
            sb.append(sampleCode.trim()).append("\n");
            sb.append("```\n\n");
        }

        // Include repository guide if CLAUDE.md exists
        String guide = extractGuide(repoDir);
        if (guide != null && !guide.trim().isEmpty()) {
            sb.append("## 4. LOYIHA TALABLARI VA ESLATMALAR:\n");
            sb.append(guide.trim()).append("\n\n");
        }

        sb.append("## 5. YANGI TEST TOPSHIRIG'I:\n");
        sb.append("Quyidagi endpoint(lar) bo'yicha to'liq test ssenariysini yozib bering:\n");
        sb.append("1. Endpoint: [BU YERGA ENDPOINT MANZILI VA METODINI YOZING, masalan: POST /leads]\n");
        sb.append("2. Request Body / Query Params: [PARAMETRLAR VA BODY SHABLONI]\n");
        sb.append("3. Kutilayotgan natija: [QANDAY MA'LUMOT VA MAYDONLAR TEKSHIRILISHI KERAK]\n");

        return sb.toString();
    }

    private static String extractSampleTest(File repoDir) {
        if (repoDir == null || !repoDir.exists()) return null;

        File[] candidateDirs = new File[]{
                new File(repoDir, "pytest_uysot"),
                new File(repoDir, "tests"),
                repoDir
        };

        for (File dir : candidateDirs) {
            if (!dir.exists() || !dir.isDirectory()) continue;
            File[] files = dir.listFiles((d, name) -> name.startsWith("test_") && name.endsWith(".py"));
            if (files != null && files.length > 0) {
                // Prefer test_get_apis.py or the first test file
                File sampleFile = files[0];
                for (File f : files) {
                    if (f.getName().contains("get_apis") || f.getName().contains("lead")) {
                        sampleFile = f;
                        break;
                    }
                }

                try {
                    List<String> lines = Files.readAllLines(sampleFile.toPath(), StandardCharsets.UTF_8);
                    StringBuilder sb = new StringBuilder();
                    int count = 0;
                    for (String line : lines) {
                        sb.append(line).append("\n");
                        count++;
                        if (count >= 40) break; // keep sample concise
                    }
                    return sb.toString();
                } catch (Exception e) {
                    LOG.debug("Error reading sample test: " + sampleFile.getName(), e);
                }
            }
        }
        return null;
    }

    private static String extractGuide(File repoDir) {
        if (repoDir == null || !repoDir.exists()) return null;
        File claudeMd = new File(repoDir, "CLAUDE.md");
        if (claudeMd.exists()) {
            try {
                List<String> lines = Files.readAllLines(claudeMd.toPath(), StandardCharsets.UTF_8);
                StringBuilder sb = new StringBuilder();
                int count = 0;
                for (String line : lines) {
                    sb.append(line).append("\n");
                    count++;
                    if (count >= 30) break;
                }
                return sb.toString();
            } catch (Exception e) {
                LOG.debug("Error reading CLAUDE.md", e);
            }
        }
        return null;
    }

    public static void copyToClipboard(String text) {
        try {
            StringSelection selection = new StringSelection(text);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);
        } catch (Exception e) {
            LOG.warn("Error copying to clipboard", e);
        }
    }

    public static File createNewTestFile(File repoDir, String rawName) throws IOException {
        if (repoDir == null || !repoDir.exists()) {
            throw new IOException("Repozitoriya papkasi topilmadi.");
        }

        String name = rawName.trim().toLowerCase();
        if (name.endsWith(".py")) {
            name = name.substring(0, name.length() - 3);
        }
        name = name.replaceAll("[^a-z0-9_]", "_");
        if (!name.startsWith("test_")) {
            name = "test_" + name;
        }

        File targetDir = new File(repoDir, "pytest_uysot");
        if (!targetDir.exists() || !targetDir.isDirectory()) {
            targetDir = new File(repoDir, "tests");
            if (!targetDir.exists() || !targetDir.isDirectory()) {
                targetDir = repoDir;
            }
        }

        File targetFile = new File(targetDir, name + ".py");
        if (targetFile.exists()) {
            return targetFile;
        }

        String cleanName = name.replace("test_", "");
        String template = String.format(
                "\"\"\"\n" +
                "%s API testlari\n" +
                "AI tomonidan yoki dasturchi tomonidan yaratilgan test ssenariysi.\n" +
                "\"\"\"\n" +
                "import requests\n" +
                "import pytest\n" +
                "from config import BASE_URL, HEADERS\n" +
                "from helpers import check_success\n\n\n" +
                "def test_%s_example():\n" +
                "    \"\"\"Namunaviy test: endpoint so'rovi va tekshiruvi\"\"\"\n" +
                "    # Misol uchun:\n" +
                "    # response = requests.get(f\"{BASE_URL}/misol\", headers=HEADERS)\n" +
                "    # data = check_success(response)\n" +
                "    # assert data is not None\n" +
                "    pass\n",
                cleanName, cleanName
        );

        Files.write(targetFile.toPath(), template.getBytes(StandardCharsets.UTF_8));
        return targetFile;
    }

    public static void openFileInEditor(Project project, File file) {
        if (project == null || file == null || !file.exists()) return;
        VirtualFile vf = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
        if (vf != null) {
            FileEditorManager.getInstance(project).openFile(vf, true);
        }
    }

    public static void openRepoInFileManager(File repoDir) {
        if (repoDir == null || !repoDir.exists()) return;
        try {
            if (RevealFileAction.isSupported()) {
                RevealFileAction.openDirectory(repoDir);
            } else if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(repoDir);
            }
        } catch (Exception e) {
            LOG.warn("Failed to open directory: " + repoDir.getAbsolutePath(), e);
        }
    }
}
