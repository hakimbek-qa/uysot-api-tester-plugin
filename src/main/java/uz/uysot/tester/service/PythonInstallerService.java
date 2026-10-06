package uz.uysot.tester.service;

import com.intellij.openapi.diagnostic.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class PythonInstallerService {
    private static final Logger LOG = Logger.getInstance(PythonInstallerService.class);

    private static final String PYTHON_DOWNLOAD_URL_64 = "https://www.python.org/ftp/python/3.12.8/python-3.12.8-amd64.exe";
    private static final String PYTHON_DOWNLOAD_URL_ARM = "https://www.python.org/ftp/python/3.12.8/python-3.12.8-arm64.exe";
    private static final String PYTHON_DOWNLOAD_URL_32 = "https://www.python.org/ftp/python/3.12.8/python-3.12.8.exe";

    public static final int CHOICE_WINGET = 0;
    public static final int CHOICE_OFFICIAL_INSTALLER = 1;
    public static final int CHOICE_CANCEL = 2;

    public interface InstallOutputCallback {
        void onOutput(String text);
    }

    public static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    public static boolean isWingetAvailable() {
        if (!isWindows()) return false;
        try {
            Process p = new ProcessBuilder("winget", "--version").start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null) {
                File wingetExe = new File(localAppData, "Microsoft/WindowsApps/winget.exe");
                if (wingetExe.exists()) {
                    try {
                        Process p2 = new ProcessBuilder(wingetExe.getAbsolutePath(), "--version").start();
                        return p2.waitFor() == 0;
                    } catch (Exception ignored) {}
                }
            }
            return false;
        }
    }

    public static String getWingetExecutable() {
        try {
            Process p = new ProcessBuilder("winget", "--version").start();
            if (p.waitFor() == 0) {
                return "winget";
            }
        } catch (Exception ignored) {}

        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null) {
            File wingetExe = new File(localAppData, "Microsoft/WindowsApps/winget.exe");
            if (wingetExe.exists()) {
                return wingetExe.getAbsolutePath();
            }
        }
        return "winget";
    }

    public static String getPythonDownloadUrl() {
        String arch = System.getProperty("os.arch", "").toLowerCase();
        if (arch.contains("aarch64") || arch.contains("arm")) {
            return PYTHON_DOWNLOAD_URL_ARM;
        } else if (arch.contains("86") && !arch.contains("64")) {
            return PYTHON_DOWNLOAD_URL_32;
        }
        return PYTHON_DOWNLOAD_URL_64;
    }

    public static int promptInstallationChoice(Component parent) {
        final int[] result = new int[]{CHOICE_CANCEL};
        Runnable r = () -> {
            String[] options = new String[]{
                    "⚡ 1-variant: Winget avtomatik (Tavsiya)",
                    "📥 2-variant: Rasmiy Installer yuklash",
                    "❌ Bekor qilish"
            };

            String message = "Windows tizimingizda Python 3 topilmadi!\n\n" +
                    "Uysot API testlarini bajarish uchun Python 3 o'rnatilishi shart.\n" +
                    "Qaysi usul orqali o'rnatishni tanlaysiz?\n\n" +
                    "• 1-variant (⚡ Winget): Fondagi avtomatik o'rnatish (Windows 10/11 uchun qulay)\n" +
                    "• 2-variant (📥 Rasmiy Installer): python.org saytidan rasmiy installerni yuklab ishga tushirish\n";

            int chosen = JOptionPane.showOptionDialog(
                    parent,
                    message,
                    "Python 3 O'rnatish — Uysot Tester",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]
            );

            if (chosen == 0) {
                result[0] = CHOICE_WINGET;
            } else if (chosen == 1) {
                result[0] = CHOICE_OFFICIAL_INSTALLER;
            } else {
                result[0] = CHOICE_CANCEL;
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(r);
            } catch (Exception e) {
                LOG.error("Failed to show Python install options dialog", e);
            }
        }

        return result[0];
    }

    public static boolean installViaWinget(InstallOutputCallback callback) {
        callback.onOutput("⚡ Winget orqali Python 3.12 o'rnatish boshlanmoqda...");
        String winget = getWingetExecutable();

        List<String> cmd = new ArrayList<>();
        cmd.add(winget);
        cmd.add("install");
        cmd.add("-e");
        cmd.add("--id");
        cmd.add("Python.Python.3.12");
        cmd.add("--scope");
        cmd.add("user");
        cmd.add("--accept-package-agreements");
        cmd.add("--accept-source-agreements");

        callback.onOutput("Buyruq: " + String.join(" ", cmd));
        int exitCode = runProcessWithOutput(cmd, callback);

        if (exitCode != 0) {
            callback.onOutput("⚠️ --scope user bilan yakunlanmadi (Exit: " + exitCode + "). Standart rejimda qayta urinilmoqda...");
            List<String> fallbackCmd = new ArrayList<>();
            fallbackCmd.add(winget);
            fallbackCmd.add("install");
            fallbackCmd.add("-e");
            fallbackCmd.add("--id");
            fallbackCmd.add("Python.Python.3.12");
            fallbackCmd.add("--accept-package-agreements");
            fallbackCmd.add("--accept-source-agreements");
            exitCode = runProcessWithOutput(fallbackCmd, callback);
        }

        if (exitCode == 0) {
            callback.onOutput("✅ Winget orqali Python 3.12 muvaffaqiyatli o'rnatildi!");
            return true;
        } else {
            callback.onOutput("❌ Winget orqali o'rnatish yakunlanmadi (Exit code: " + exitCode + ")");
            return false;
        }
    }

    public static File downloadPythonInstaller(InstallOutputCallback callback) throws IOException {
        String downloadUrl = getPythonDownloadUrl();
        String fileName = downloadUrl.substring(downloadUrl.lastIndexOf('/') + 1);
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "uysot_installer");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        File targetFile = new File(tempDir, fileName);

        callback.onOutput("📥 python.org rasmiy installeri yuklab olinmoqda...");
        callback.onOutput("Havola: " + downloadUrl);
        callback.onOutput("Saqlash: " + targetFile.getAbsolutePath());

        URL url = new URL(downloadUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(60000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 UysotApiTesterPlugin/1.2.4");

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
            String newUrl = conn.getHeaderField("Location");
            conn = (HttpURLConnection) new URL(newUrl).openConnection();
            conn.setConnectTimeout(20000);
            conn.setReadTimeout(60000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 UysotApiTesterPlugin/1.2.4");
        }

        long totalBytes = conn.getContentLengthLong();
        long downloadedBytes = 0;

        try (InputStream in = new BufferedInputStream(conn.getInputStream());
             OutputStream out = new BufferedOutputStream(new FileOutputStream(targetFile))) {

            byte[] buffer = new byte[16384];
            int read;
            long lastReportedTime = System.currentTimeMillis();
            int lastPercent = -1;

            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                downloadedBytes += read;

                long now = System.currentTimeMillis();
                if (totalBytes > 0) {
                    int percent = (int) ((downloadedBytes * 100) / totalBytes);
                    if (percent != lastPercent && (percent % 10 == 0 || now - lastReportedTime > 1500)) {
                        callback.onOutput(String.format("Yuklanmoqda: %d%% (%.1f MB / %.1f MB)",
                                percent, downloadedBytes / (1024.0 * 1024.0), totalBytes / (1024.0 * 1024.0)));
                        lastPercent = percent;
                        lastReportedTime = now;
                    }
                } else if (now - lastReportedTime > 2000) {
                    callback.onOutput(String.format("Yuklanmoqda: %.1f MB...", downloadedBytes / (1024.0 * 1024.0)));
                    lastReportedTime = now;
                }
            }
        } finally {
            conn.disconnect();
        }

        callback.onOutput("✅ O'rnatuvchi fayli yuklab olindi (" + String.format("%.1f MB", targetFile.length() / (1024.0 * 1024.0)) + ")");
        return targetFile;
    }

    public static boolean runOfficialInstaller(File installerFile, InstallOutputCallback callback) {
        callback.onOutput("\n🚀 Rasmiy Python installeri ishga tushirilmoqda...");
        callback.onOutput("ℹ️ 'PrependPath=1' parametri qo'shilgan (PATH avtomatik sozlanadi).");
        callback.onOutput("👉 Ochilgan oynada 'Install Now' tugmasini bosing va o'rnatish yakunlanishini kuting...");

        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(installerFile.getAbsolutePath());
            cmd.add("PrependPath=1");
            cmd.add("Include_pip=1");

            Process process = new ProcessBuilder(cmd).start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                callback.onOutput("✅ Python o'rnatish jarayoni muvaffaqiyatli yakunlandi!");
                return true;
            } else {
                callback.onOutput("⚠️ O'rnatuvchi dastur yakunlandi (Exit code: " + exitCode + ")");
                return false;
            }
        } catch (Exception e) {
            LOG.error("Failed to execute official Python installer", e);
            callback.onOutput("❌ O'rnatuvchini ishga tushirishda xatolik: " + e.getMessage());
            return false;
        }
    }

    public static boolean downloadAndInstallOfficial(InstallOutputCallback callback) {
        try {
            File installer = downloadPythonInstaller(callback);
            return runOfficialInstaller(installer, callback);
        } catch (Exception e) {
            LOG.error("Failed to download and install official Python", e);
            callback.onOutput("❌ Rasmiy installerni yuklash yoki o'rnatishda xatolik: " + e.getMessage());
            return false;
        }
    }

    private static int runProcessWithOutput(List<String> cmd, InstallOutputCallback callback) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    callback.onOutput(line);
                }
            }
            return process.waitFor();
        } catch (Exception e) {
            LOG.error("Process execution failed: " + cmd, e);
            callback.onOutput("Xatolik: " + e.getMessage());
            return -1;
        }
    }
}
