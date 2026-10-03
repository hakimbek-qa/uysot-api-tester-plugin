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
        void onComplete(boolean success, String message);
    }

    public static void syncRepository(Project project, SyncCallback callback) {
        new Thread(() -> {
            UysotSettingsState settings = UysotSettingsState.getInstance();
            File targetDir = new File(settings.clonePath);
            Git git = Git.getInstance();

            GitLineHandlerListener listener = new GitLineHandlerListener() {
                @Override
                public void onLineAvailable(String line, Key outputType) {
                    callback.onOutput(line);
                }
            };

            try {
                if (!targetDir.exists() || !new File(targetDir, ".git").exists()) {
                    callback.onOutput("Repository mavjud emas, IntelliJ Git orqali yuklab olinmoqda: " + settings.repoUrl);
                    targetDir.mkdirs();

                    File parentDir = targetDir.getParentFile();
                    String dirName = targetDir.getName();

                    GitCommandResult result = git.clone(project, parentDir, settings.repoUrl, dirName, listener);

                    if (result.success()) {
                        callback.onComplete(true, "Testlar muvaffaqiyatli yuklandi!");
                    } else {
                        String err = result.getErrorOutputAsJoinedString();
                        callback.onOutput("Xatolik tafsiloti: " + (err.isEmpty() ? result.toString() : err));
                        callback.onComplete(false, "Git clone muvaffaqiyatsiz bo'ldi (Exit code: " + result.getExitCode() + ").");
                    }
                } else {
                    callback.onOutput("Repository mavjud, eng so'nggi testlar tortilmoqda (git pull)...");

                    GitLineHandler handler = new GitLineHandler(project, targetDir, GitCommand.PULL);
                    handler.addParameters("origin", "main");
                    handler.setUrl(settings.repoUrl);
                    handler.addLineListener(listener);

                    GitCommandResult result = git.runCommand(handler);

                    if (result.success()) {
                        callback.onComplete(true, "Testlar muvaffaqiyatli yangilandi!");
                    } else {
                        String err = result.getErrorOutputAsJoinedString();
                        callback.onOutput("Xatolik tafsiloti: " + (err.isEmpty() ? result.toString() : err));
                        callback.onComplete(false, "Git pull muvaffaqiyatsiz bo'ldi.");
                    }
                }
            } catch (Exception e) {
                LOG.error("Git sync error", e);
                callback.onComplete(false, "Xatolik: " + e.getMessage());
            }
        }).start();
    }
}
