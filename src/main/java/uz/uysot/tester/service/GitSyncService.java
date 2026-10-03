package uz.uysot.tester.service;

import com.intellij.openapi.diagnostic.Logger;
import uz.uysot.tester.state.UysotSettingsState;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class GitSyncService {
    private static final Logger LOG = Logger.getInstance(GitSyncService.class);

    public interface SyncCallback {
        void onOutput(String line);
        void onComplete(boolean success, String message);
    }

    public static void syncRepository(SyncCallback callback) {
        new Thread(() -> {
            UysotSettingsState settings = UysotSettingsState.getInstance();
            File targetDir = new File(settings.clonePath);

            try {
                if (!targetDir.exists() || !new File(targetDir, ".git").exists()) {
                    callback.onOutput("Repository mavjud emas, yuklab olinmoqda: " + settings.repoUrl);
                    targetDir.mkdirs();

                    // 1-urinish: Belgilangan URL bilan clone qilish
                    boolean success = runProcess(new ProcessBuilder("git", "clone", settings.repoUrl, targetDir.getAbsolutePath()), callback);

                    // Agar HTTPS bo'lsa va xato bersa, avtomatik SSH bilan sinab ko'ramiz
                    if (!success && settings.repoUrl.startsWith("https://github.com/")) {
                        String sshUrl = settings.repoUrl.replace("https://github.com/", "git@github.com:");
                        callback.onOutput("\n[Avtomatik urinish] HTTPS xato berdi. SSH orqali sinab ko'rilmoqda: " + sshUrl);
                        deleteDirectory(targetDir);
                        targetDir.mkdirs();

                        success = runProcess(new ProcessBuilder("git", "clone", sshUrl, targetDir.getAbsolutePath()), callback);
                        if (success) {
                            settings.repoUrl = sshUrl;
                            callback.onOutput("SSH orqali muvaffaqiyatli yuklandi!");
                        }
                    } else if (!success && settings.repoUrl.startsWith("git@github.com:")) {
                        String httpsUrl = settings.repoUrl.replace("git@github.com:", "https://github.com/");
                        callback.onOutput("\n[Avtomatik urinish] SSH xato berdi. HTTPS orqali sinab ko'rilmoqda: " + httpsUrl);
                        deleteDirectory(targetDir);
                        targetDir.mkdirs();

                        success = runProcess(new ProcessBuilder("git", "clone", httpsUrl, targetDir.getAbsolutePath()), callback);
                        if (success) {
                            settings.repoUrl = httpsUrl;
                            callback.onOutput("HTTPS orqali muvaffaqiyatli yuklandi!");
                        }
                    }

                    if (success) {
                        callback.onComplete(true, "Testlar muvaffaqiyatli yuklandi!");
                    } else {
                        callback.onComplete(false, "Git avtorizatsiya xatosi. Iltimos terminalda 1 marta quyidagi buyruqlardan birini bering:\n" +
                                "git clone git@github.com:hakimbek-qa/uysot-open-api-automation.git " + settings.clonePath +
                                "\nyoki\ngit clone https://github.com/hakimbek-qa/uysot-open-api-automation.git " + settings.clonePath);
                    }
                } else {
                    callback.onOutput("Repository mavjud, eng so'nggi testlar tortilmoqda (git pull)...");
                    ProcessBuilder pb = new ProcessBuilder("git", "pull", "origin", "main");
                    pb.directory(targetDir);
                    boolean success = runProcess(pb, callback);
                    if (success) {
                        callback.onComplete(true, "Testlar muvaffaqiyatli yangilandi!");
                    } else {
                        callback.onComplete(false, "Git pull xatolik bilan tugadi.");
                    }
                }
            } catch (Exception e) {
                LOG.error("Git sync error", e);
                callback.onComplete(false, "Xatolik: " + e.getMessage());
            }
        }).start();
    }

    private static boolean runProcess(ProcessBuilder pb, SyncCallback callback) throws Exception {
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                callback.onOutput(line);
            }
        }

        int exitCode = process.waitFor();
        return exitCode == 0;
    }

    private static void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteDirectory(f);
                }
            }
        }
        dir.delete();
    }
}
