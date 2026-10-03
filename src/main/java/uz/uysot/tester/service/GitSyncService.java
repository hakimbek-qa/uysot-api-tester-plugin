package uz.uysot.tester.service;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import git4idea.commands.*;
import uz.uysot.tester.state.UysotSettingsState;

import java.io.File;

public class GitSyncService {
    private static final Logger LOG = Logger.getInstance(GitSyncService.class);

    public interface SyncCallback {
        void onOutput(String line);
        void onComplete(boolean success, String message, File repoDir);
    }

    public static void syncRepository(Project project, String repoUrl, SyncCallback callback) {
        new Thread(() -> {
            if (repoUrl == null || repoUrl.trim().isEmpty()) {
                callback.onComplete(false, "Repozitoriya havolasi kiritilmagan!", null);
                return;
            }

            String cleanUrl = repoUrl.trim();
            File targetDir = UysotSettingsState.getRepoDirectory(cleanUrl);
            Git git = Git.getInstance();

            GitLineHandlerListener listener = new GitLineHandlerListener() {
                @Override
                public void onLineAvailable(String line, Key outputType) {
                    callback.onOutput(line);
                }
            };

            try {
                if (!targetDir.exists() || !new File(targetDir, ".git").exists()) {
                    callback.onOutput("Repozitoriya lokalda mavjud emas, IntelliJ Git orqali yuklab olinmoqda (clone)...");
                    callback.onOutput("Manzil: " + cleanUrl);
                    callback.onOutput("Papka: " + targetDir.getAbsolutePath());
                    targetDir.mkdirs();

                    File parentDir = targetDir.getParentFile();
                    String dirName = targetDir.getName();

                    GitCommandResult result = git.clone(project, parentDir, cleanUrl, dirName, listener);

                    if (result.success()) {
                        callback.onComplete(true, "Testlar muvaffaqiyatli yuklab olindi!", targetDir);
                    } else {
                        String err = result.getErrorOutputAsJoinedString();
                        callback.onOutput("Xatolik tafsiloti: " + (err.isEmpty() ? result.toString() : err));
                        callback.onComplete(false, "Git clone muvaffaqiyatsiz bo'ldi (Exit code: " + result.getExitCode() + ").", targetDir);
                    }
                } else {
                    callback.onOutput("Repozitoriya mavjud, yangilanishlar tortilmoqda (git pull)...");
                    callback.onOutput("Papka: " + targetDir.getAbsolutePath());

                    GitLineHandler handler = new GitLineHandler(project, targetDir, GitCommand.PULL);
                    handler.setUrl(cleanUrl);
                    handler.addLineListener(listener);

                    GitCommandResult result = git.runCommand(handler);

                    if (result.success()) {
                        callback.onComplete(true, "Testlar muvaffaqiyatli yangilandi!", targetDir);
                    } else {
                        String err = result.getErrorOutputAsJoinedString();
                        callback.onOutput("Xatolik tafsiloti: " + (err.isEmpty() ? result.toString() : err));
                        callback.onComplete(false, "Git pull muvaffaqiyatsiz bo'ldi.", targetDir);
                    }
                }
            } catch (Exception e) {
                LOG.error("Git sync error", e);
                callback.onComplete(false, "Xatolik: " + e.getMessage(), targetDir);
            }
        }).start();
    }
}
