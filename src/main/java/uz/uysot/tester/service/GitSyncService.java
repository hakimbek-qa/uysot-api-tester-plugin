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
                ProcessBuilder pb;
                if (!targetDir.exists() || !new File(targetDir, ".git").exists()) {
                    callback.onOutput("Repository mavjud emas, yuklab olinmoqda (git clone): " + settings.repoUrl);
                    targetDir.mkdirs();
                    pb = new ProcessBuilder("git", "clone", settings.repoUrl, targetDir.getAbsolutePath());
                } else {
                    callback.onOutput("Repository mavjud, eng so'nggi testlar tortilmoqda (git pull)...");
                    pb = new ProcessBuilder("git", "pull", "origin", "main");
                    pb.directory(targetDir);
                }

                pb.redirectErrorStream(true);
                Process process = pb.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        callback.onOutput(line);
                    }
                }

                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    callback.onComplete(true, "Testlar muvaffaqiyatli yangilandi!");
                } else {
                    callback.onComplete(false, "Git amali xato bilan tugadi. Exit code: " + exitCode);
                }
            } catch (Exception e) {
                LOG.error("Git sync error", e);
                callback.onComplete(false, "Xatolik: " + e.getMessage());
            }
        }).start();
    }
}
