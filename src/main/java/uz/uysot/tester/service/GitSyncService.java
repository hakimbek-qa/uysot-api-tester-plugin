package uz.uysot.tester.service;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import git4idea.commands.*;
import uz.uysot.tester.state.UysotSettingsState;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GitSyncService {
    private static final Logger LOG = Logger.getInstance(GitSyncService.class);

    public interface SyncCallback {
        void onOutput(String line);
        void onComplete(boolean success, String message, File repoDir);
    }

    public static class ChangedFile {
        public enum Status {
            UNTRACKED, MODIFIED, ADDED, DELETED, RENAMED, OTHER
        }

        private final String relativePath;
        private final Status status;
        private boolean selected = true;

        public ChangedFile(String relativePath, Status status) {
            this.relativePath = relativePath;
            this.status = status;
        }

        public String getRelativePath() {
            return relativePath;
        }

        public Status getStatus() {
            return status;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        public String getStatusBadge() {
            switch (status) {
                case UNTRACKED:
                    return "[➕ Yangi]";
                case MODIFIED:
                    return "[✏️ O'zgartirilgan]";
                case ADDED:
                    return "[📄 Qo'shilgan]";
                case DELETED:
                    return "[🗑️ O'chirilgan]";
                case RENAMED:
                    return "[🔄 Nomlangan]";
                default:
                    return "[ℹ️ O'zgarish]";
            }
        }
    }

    public interface ChangedFilesCallback {
        void onResult(boolean success, List<ChangedFile> files, String error);
    }

    public interface CommitPushCallback {
        void onOutput(String line);
        void onComplete(boolean success, String message);
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

    public static void getChangedFiles(Project project, File repoDir, ChangedFilesCallback callback) {
        new Thread(() -> {
            if (repoDir == null || !repoDir.exists() || !new File(repoDir, ".git").exists()) {
                callback.onResult(false, Collections.emptyList(), "Git repozitoriyasi mavjud emas yoki hali yuklab olinmagan.");
                return;
            }

            try {
                Git git = Git.getInstance();
                GitLineHandler handler = new GitLineHandler(project, repoDir, GitCommand.STATUS);
                handler.addParameters("--porcelain");

                List<String> outputLines = new ArrayList<>();
                handler.addLineListener(new GitLineHandlerListener() {
                    @Override
                    public void onLineAvailable(String line, Key outputType) {
                        if (line != null && !line.trim().isEmpty()) {
                            outputLines.add(line);
                        }
                    }
                });

                GitCommandResult result = git.runCommand(handler);
                if (!result.success()) {
                    callback.onResult(false, Collections.emptyList(), result.getErrorOutputAsJoinedString());
                    return;
                }

                List<ChangedFile> files = new ArrayList<>();
                for (String line : outputLines) {
                    if (line.length() < 3) continue;
                    String statusCode = line.substring(0, 2);
                    String filePath = line.substring(3).trim();

                    if (filePath.contains(" -> ")) {
                        filePath = filePath.substring(filePath.indexOf(" -> ") + 4).trim();
                    }
                    if (filePath.startsWith("\"") && filePath.endsWith("\"") && filePath.length() > 1) {
                        filePath = filePath.substring(1, filePath.length() - 1);
                    }

                    ChangedFile.Status status;
                    if (statusCode.contains("??")) {
                        status = ChangedFile.Status.UNTRACKED;
                    } else if (statusCode.contains("A")) {
                        status = ChangedFile.Status.ADDED;
                    } else if (statusCode.contains("M")) {
                        status = ChangedFile.Status.MODIFIED;
                    } else if (statusCode.contains("D")) {
                        status = ChangedFile.Status.DELETED;
                    } else if (statusCode.contains("R")) {
                        status = ChangedFile.Status.RENAMED;
                    } else {
                        status = ChangedFile.Status.OTHER;
                    }

                    files.add(new ChangedFile(filePath, status));
                }

                callback.onResult(true, files, null);
            } catch (Exception e) {
                LOG.error("Git status error", e);
                callback.onResult(false, Collections.emptyList(), e.getMessage());
            }
        }).start();
    }

    public static void commitAndPush(Project project, File repoDir, String repoUrl, List<String> filePaths, String commitMessage, CommitPushCallback callback) {
        new Thread(() -> {
            try {
                Git git = Git.getInstance();
                GitLineHandlerListener listener = new GitLineHandlerListener() {
                    @Override
                    public void onLineAvailable(String line, Key outputType) {
                        callback.onOutput(line);
                    }
                };

                // 1. Git add
                callback.onOutput(">>> Git add o'zgarishlarni kiritish...");
                GitLineHandler addHandler = new GitLineHandler(project, repoDir, GitCommand.ADD);
                if (filePaths == null || filePaths.isEmpty()) {
                    addHandler.addParameters("-A");
                } else {
                    for (String path : filePaths) {
                        addHandler.addParameters(path);
                    }
                }
                addHandler.addLineListener(listener);
                GitCommandResult addResult = git.runCommand(addHandler);
                if (!addResult.success()) {
                    callback.onComplete(false, "Git add xatoligi: " + addResult.getErrorOutputAsJoinedString());
                    return;
                }

                // 2. Git commit
                callback.onOutput(">>> Git commit amalga oshirilmoqda...");
                GitLineHandler commitHandler = new GitLineHandler(project, repoDir, GitCommand.COMMIT);
                commitHandler.addParameters("-m", commitMessage);
                commitHandler.addLineListener(listener);
                GitCommandResult commitResult = git.runCommand(commitHandler);
                if (!commitResult.success()) {
                    callback.onComplete(false, "Git commit xatoligi: " + commitResult.getErrorOutputAsJoinedString());
                    return;
                }

                // 3. Git push
                callback.onOutput(">>> Git push masofaviy repozitoriyaga yuborilmoqda...");
                GitLineHandler pushHandler = new GitLineHandler(project, repoDir, GitCommand.PUSH);
                if (repoUrl != null && !repoUrl.trim().isEmpty()) {
                    pushHandler.setUrl(repoUrl.trim());
                }
                pushHandler.addLineListener(listener);
                GitCommandResult pushResult = git.runCommand(pushHandler);
                if (pushResult.success()) {
                    callback.onComplete(true, "O'zgarishlar Git repozitoriyasiga muvaffaqiyatli push qilindi!");
                } else {
                    String err = pushResult.getErrorOutputAsJoinedString();
                    callback.onComplete(false, "Git push xatoligi: " + (err.isEmpty() ? pushResult.toString() : err));
                }
            } catch (Exception e) {
                LOG.error("Commit and push error", e);
                callback.onComplete(false, "Xatolik yuz berdi: " + e.getMessage());
            }
        }).start();
    }
}
