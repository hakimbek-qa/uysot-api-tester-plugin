package uz.uysot.tester.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class AiPromptService {
    private static final Logger LOG = Logger.getInstance(AiPromptService.class);

    /**
     * Loyiha ichidagi AI qoidalari ko'rsatilgan Markdown faylini topish.
     * Prioritet:
     * 1. .tester.json dagi "aiRulesFile"
     * 2. AI_TEST_RULES.md
     * 3. AI_RULES.md
     * 4. TEST_RULES.md
     * 5. CLAUDE.md
     * 6. .cursorrules / .github/copilot-instructions.md
     */
    public static File findAiRulesFile(File repoDir) {
        if (repoDir == null || !repoDir.exists()) return null;

        // 1. Check .tester.json or tester-config.json
        File configFile = new File(repoDir, ".tester.json");
        if (!configFile.exists()) {
            configFile = new File(repoDir, "tester-config.json");
        }
        if (configFile.exists()) {
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8)) {
                JsonObject json = new Gson().fromJson(reader, JsonObject.class);
                if (json != null && json.has("aiRulesFile")) {
                    String specifiedFile = json.get("aiRulesFile").getAsString().trim();
                    File customRules = new File(repoDir, specifiedFile);
                    if (customRules.exists() && customRules.isFile()) {
                        return customRules;
                    }
                }
            } catch (Exception e) {
                LOG.debug("Error checking aiRulesFile in config: " + configFile.getAbsolutePath(), e);
            }
        }

        // 2. Standard candidate list
        String[] candidates = new String[]{
                "AI_TEST_RULES.md",
                "AI_RULES.md",
                "TEST_RULES.md",
                "CLAUDE.md",
                ".cursorrules",
                ".github/copilot-instructions.md"
        };

        for (String candidate : candidates) {
            File file = new File(repoDir, candidate);
            if (file.exists() && file.isFile()) {
                return file;
            }
        }

        return null;
    }

    /**
     * AI uchun prompt yaratish.
     * Loyihadagi maxsus Markdown qoidalar fayli o'qilib, undan zarracha chetga chiqmagan holda
     * test yozish talabi bilan prompt tuziladi.
     */
    public static String generatePrompt(File repoDir, String baseUrl, String authType) {
        File rulesFile = findAiRulesFile(repoDir);

        if (rulesFile != null && rulesFile.exists()) {
            try {
                String rulesContent = new String(Files.readAllBytes(rulesFile.toPath()), StandardCharsets.UTF_8);
                StringBuilder sb = new StringBuilder();

                sb.append("# 🤖 AI TEST GENERATOR VAZIFASI\n\n");
                sb.append("Siz tajribali QA Automation muhandisisiz.\n");
                sb.append("Quyidagi API endpoint uchun Python pytest test kodini yozib berishingiz kerak.\n\n");
                sb.append("⚠️ QAT'IY TALAB:\n");
                sb.append("Ushbu loyihaning o'ziga xos qoidalari, arxitekturasi va andozalari mavjud.\n");
                sb.append("Siz faqat va faqat quyida keltirilgan `").append(rulesFile.getName()).append("` fayli qoidalariga rioya qilishingiz shart!\n");
                sb.append("Loyiha strukturasi, fayllar joylashuvi va qoidalaridan zarracha chetga chiqish taqiqlanadi.\n\n");
                sb.append("================================================================================\n");
                sb.append("📄 LOYIHA QOIDALARI (").append(rulesFile.getName()).append(" faylidan o'qildi):\n");
                sb.append("================================================================================\n\n");
                sb.append(rulesContent.trim()).append("\n\n");
                sb.append("================================================================================\n");
                sb.append("🎯 SIZNING TOPSHIRIG'INGIZ (Yangi test):\n");
                sb.append("================================================================================\n\n");
                sb.append("Yuqoridagi loyiha qoidalariga 100% rioya qilgan holda, quyidagi endpoint uchun to'liq test ssenariysini yozib bering:\n");
                sb.append("1. Endpoint: [BU YERGA ENDPOINT MANZILI VA METODINI YOZING, masalan: POST /leads]\n");
                sb.append("2. Parametrlar / Body: [YUBORILADIGAN JSON YOKI QUERY PARAMETRLAR]\n");
                sb.append("3. Kutilayotgan javob: [QANDAY MA'LUMOT VA MAYDONLAR TEKSHIRILISHI KERAK]\n");

                return sb.toString();
            } catch (Exception e) {
                LOG.warn("Error reading rules file: " + rulesFile.getAbsolutePath(), e);
            }
        }

        // Fallback: Default generic prompt if no markdown rules file exists
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
        sb.append("- Asinxron operatsiyalar uchun: `wait_request(response)` ishlatiladi.\n");
        sb.append("- Til: Barcha docstring, izohlar va assert xabarlari O'ZBEK tilida yozilishi shart.\n\n");
        sb.append("## 3. TOPSHIRIQ:\n");
        sb.append("1. Endpoint: [ENDPOINT MANZILINI YOZING]\n");
        sb.append("2. Body / Params: [PARAMETRLAR]\n");
        sb.append("3. Kutilayotgan natija: [TEKSHIRISHLAR]\n\n");
        sb.append("💡 Eslatma: Ushbu loyihada 'AI_TEST_RULES.md' fayli topilmadi. Loyihangizda 'AI_TEST_RULES.md' faylini yaratsangiz, AI aynan siz kiritgan loyiha qoidalariga bo'ysunadi.\n");

        return sb.toString();
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
