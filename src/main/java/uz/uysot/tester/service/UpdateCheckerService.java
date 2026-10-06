package uz.uysot.tester.service;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.ide.startup.StartupActionScriptManager;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateCheckerService {
    private static final Logger LOG = Logger.getInstance(UpdateCheckerService.class);
    public static final String CURRENT_VERSION = "1.2.6";
    public static final String UPDATE_XML_URL = "https://raw.githubusercontent.com/hakimbek-qa/uysot-api-tester-plugin/main/updatePlugins.xml";
    public static final String RELEASES_PAGE_URL = "https://github.com/hakimbek-qa/uysot-api-tester-plugin/releases";

    public static void checkUpdates(Project project, Component parentComponent, boolean interactive) {
        new Thread(() -> {
            try {
                // Add timestamp query parameter to bypass Fastly/GitHub CDN cache
                String bypassUrl = UPDATE_XML_URL + "?t=" + System.currentTimeMillis();
                URL url = new URL(bypassUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "UysotApiTesterPlugin/" + CURRENT_VERSION);

                String latestVersion = null;
                String downloadUrl = null;

                if (conn.getResponseCode() == 200) {
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line).append("\n");
                        }
                    }

                    String xml = sb.toString();
                    // Remove XML prolog (<?xml ... ?>) so version="1.0" in XML header is never matched as plugin version
                    String cleanXml = xml.replaceFirst("(?s)<\\?xml[^>]*\\?>", "");

                    Matcher vm1 = Pattern.compile("<plugin[^>]*?\\bversion=\"([^\"]+)\"").matcher(cleanXml);
                    if (vm1.find()) {
                        latestVersion = vm1.group(1).trim();
                    } else {
                        Matcher vm2 = Pattern.compile("version=\"([^\"]+)\"").matcher(cleanXml);
                        if (vm2.find()) {
                            latestVersion = vm2.group(1).trim();
                        }
                    }

                    Matcher um1 = Pattern.compile("<plugin[^>]*?\\burl=\"([^\"]+)\"").matcher(cleanXml);
                    if (um1.find()) {
                        downloadUrl = um1.group(1).trim();
                    } else {
                        Matcher um2 = Pattern.compile("url=\"([^\"]+)\"").matcher(cleanXml);
                        if (um2.find()) {
                            downloadUrl = um2.group(1).trim();
                        }
                    }
                }

                // Fallback to GitHub API if latestVersion not found or was invalid
                if (latestVersion == null || latestVersion.equals("1.0")) {
                    String ghVersion = fetchLatestVersionFromGitHubApi();
                    if (ghVersion != null) {
                        latestVersion = ghVersion;
                        if (downloadUrl == null) {
                            downloadUrl = "https://github.com/hakimbek-qa/uysot-api-tester-plugin/releases/download/v" + ghVersion + "/uysot-api-tester-plugin-" + ghVersion + ".zip";
                        }
                    }
                }

                final String finalLatestVersion = latestVersion;
                final String finalDownloadUrl = downloadUrl;

                if (finalLatestVersion != null && isNewerVersion(finalLatestVersion, CURRENT_VERSION)) {
                    SwingUtilities.invokeLater(() -> {
                        String message = "🚀 Yangi versiya topildi!\n\n"
                                + "Hozirgi versiya: v" + CURRENT_VERSION + "\n"
                                + "Eng so'nggi versiya: v" + finalLatestVersion + "\n\n"
                                + "Eslatma: Oraliq versiyalarni bosqichma-bosqich o'rnatish shart emas.\n"
                                + "To'g'ridan-to'g'ri eng so'nggi relizga avtomatik yangilashni xohlaysizmi?";

                        Object[] options = {
                                "⚡ Eng so'nggi versiyaga yangilash (Avtomatik)",
                                "🌐 Brauzerda yuklab olish (.zip)",
                                "⚙️ Plugins oynasi",
                                "Yopish"
                        };
                        int choice = JOptionPane.showOptionDialog(
                                parentComponent,
                                message,
                                "Yangi versiya: v" + finalLatestVersion,
                                JOptionPane.DEFAULT_OPTION,
                                JOptionPane.INFORMATION_MESSAGE,
                                null,
                                options,
                                options[0]
                        );

                        if (choice == 0) {
                            String targetUrl = (finalDownloadUrl != null && !finalDownloadUrl.isEmpty())
                                    ? finalDownloadUrl
                                    : "https://github.com/hakimbek-qa/uysot-api-tester-plugin/releases/download/v" + finalLatestVersion + "/uysot-api-tester-plugin-" + finalLatestVersion + ".zip";
                            performAutoUpdate(project, parentComponent, finalLatestVersion, targetUrl);
                        } else if (choice == 1) {
                            String targetUrl = (finalDownloadUrl != null && !finalDownloadUrl.isEmpty())
                                    ? finalDownloadUrl
                                    : RELEASES_PAGE_URL;
                            BrowserUtil.browse(targetUrl);
                        } else if (choice == 2) {
                            ShowSettingsUtil.getInstance().showSettingsDialog(project, "Plugins");
                        }
                    });

                    Notification notification = new Notification(
                            "Uysot Notifications",
                            "🚀 Uysot API Tester: v" + finalLatestVersion + " mavjud!",
                            "Eng so'nggi versiya: v" + finalLatestVersion + " (Sizda: v" + CURRENT_VERSION + "). Oraliq versiyalarsiz to'g'ridan-to'g'ri yangilash mumkin.",
                            NotificationType.INFORMATION
                    );

                    notification.addAction(new NotificationAction("⚡ Darhol yangilash") {
                        @Override
                        public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                            notification.expire();
                            String targetUrl = (finalDownloadUrl != null && !finalDownloadUrl.isEmpty())
                                    ? finalDownloadUrl
                                    : "https://github.com/hakimbek-qa/uysot-api-tester-plugin/releases/download/v" + finalLatestVersion + "/uysot-api-tester-plugin-" + finalLatestVersion + ".zip";
                            performAutoUpdate(project, parentComponent, finalLatestVersion, targetUrl);
                        }
                    });

                    notification.addAction(new NotificationAction("Relizni ko'rish") {
                        @Override
                        public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                            BrowserUtil.browse(RELEASES_PAGE_URL);
                        }
                    });

                    Notifications.Bus.notify(notification, project);
                    return;
                }

                if (interactive) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(
                                parentComponent,
                                "✅ Sizda eng so'nggi versiya (v" + CURRENT_VERSION + ") o'rnatilgan!\n\n"
                                        + "GitHub'dagi eng so'nggi reliz: v" + (finalLatestVersion != null ? finalLatestVersion : CURRENT_VERSION),
                                "Uysot API Tester",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    });
                }

            } catch (Exception e) {
                LOG.warn("Update check failed", e);
                if (interactive) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(
                                parentComponent,
                                "Yangilanishlarni tekshirishda xatolik yuz berdi:\n" + e.getMessage(),
                                "Xatolik",
                                JOptionPane.ERROR_MESSAGE
                        );
                    });
                }
            }
        }).start();
    }

    public static void performAutoUpdate(Project project, Component parentComponent, String targetVersion, String downloadUrl) {
        ProgressManager.getInstance().run(new Task.Modal(project, "Uysot API Tester v" + targetVersion + " yangilanmoqda...", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                indicator.setText("Eng oxirgi versiya (v" + targetVersion + ") yuklab olinmoqda...");

                File tempZip = null;
                try {
                    File tempDir = new File(System.getProperty("java.io.tmpdir"), "uysot_update");
                    if (!tempDir.exists()) tempDir.mkdirs();
                    tempZip = new File(tempDir, "uysot-api-tester-plugin-" + targetVersion + ".zip");

                    URL url = new URL(downloadUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setInstanceFollowRedirects(true);
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(60000);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 UysotApiTesterPlugin/" + CURRENT_VERSION);

                    int code = conn.getResponseCode();
                    if (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP) {
                        String newUrl = conn.getHeaderField("Location");
                        conn = (HttpURLConnection) new URL(newUrl).openConnection();
                        conn.setConnectTimeout(15000);
                        conn.setReadTimeout(60000);
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0 UysotApiTesterPlugin/" + CURRENT_VERSION);
                    }

                    long totalBytes = conn.getContentLengthLong();
                    long downloadedBytes = 0;

                    try (InputStream in = new BufferedInputStream(conn.getInputStream());
                         OutputStream out = new BufferedOutputStream(new FileOutputStream(tempZip))) {
                        byte[] buffer = new byte[16384];
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            if (indicator.isCanceled()) {
                                tempZip.delete();
                                return;
                            }
                            out.write(buffer, 0, read);
                            downloadedBytes += read;
                            if (totalBytes > 0) {
                                double fraction = (double) downloadedBytes / totalBytes;
                                indicator.setFraction(fraction);
                                indicator.setText(String.format("Yuklanmoqda: %.1f MB / %.1f MB (%d%%)",
                                        downloadedBytes / (1024.0 * 1024.0),
                                        totalBytes / (1024.0 * 1024.0),
                                        (int) (fraction * 100)));
                            } else {
                                indicator.setText(String.format("Yuklanmoqda: %.1f MB...", downloadedBytes / (1024.0 * 1024.0)));
                            }
                        }
                    } finally {
                        conn.disconnect();
                    }

                    indicator.setText("O'rnatishga tayyorlanmoqda...");

                    PluginId pluginId = PluginId.getId("uz.uysot.api.tester");
                    IdeaPluginDescriptor descriptor = PluginManagerCore.getPlugin(pluginId);
                    Path currentPath = descriptor != null ? descriptor.getPluginPath() : null;

                    if (currentPath != null) {
                        StartupActionScriptManager.addActionCommand(new StartupActionScriptManager.DeleteCommand(currentPath));
                    }
                    StartupActionScriptManager.addActionCommand(new StartupActionScriptManager.UnzipCommand(tempZip.toPath(), PathManager.getPluginsDir()));

                    SwingUtilities.invokeLater(() -> {
                        int choice = JOptionPane.showOptionDialog(
                                parentComponent,
                                "🎉 Uysot API Tester v" + targetVersion + " muvaffaqiyatli yuklandi!\n\n" +
                                        "Eng so'nggi versiyaga to'liq o'tish uchun IDE ni qayta ishga tushirish (Restart) kerak.\n\n" +
                                        "Hozir qayta ishga tushirishni xohlaysizmi?",
                                "Yangilanish Tayyor — v" + targetVersion,
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.INFORMATION_MESSAGE,
                                null,
                                new String[]{"🔄 Hozir qayta ishga tushirish (Restart)", "Keyinroq"},
                                "🔄 Hozir qayta ishga tushirish (Restart)"
                        );

                        if (choice == 0) {
                            ApplicationManager.getApplication().restart();
                        }
                    });

                } catch (Exception ex) {
                    LOG.error("Failed to auto-update plugin", ex);
                    if (tempZip != null && tempZip.exists()) {
                        tempZip.delete();
                    }
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(
                                parentComponent,
                                "Avtomatik yangilashda xatolik yuz berdi: " + ex.getMessage() + "\n\n" +
                                        "Relizni brauzer orqali yuklab olishingiz mumkin.",
                                "Xatolik",
                                JOptionPane.ERROR_MESSAGE
                        );
                    });
                }
            }
        });
    }

    private static String fetchLatestVersionFromGitHubApi() {
        try {
            URL url = new URL("https://api.github.com/repos/hakimbek-qa/uysot-api-tester-plugin/releases/latest");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "UysotApiTesterPlugin");
            if (conn.getResponseCode() == 200) {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder b = new StringBuilder();
                    String line;
                    while ((line = r.readLine()) != null) b.append(line);
                    Matcher m = Pattern.compile("\"tag_name\"\\s*:\\s*\"v?([^\"]+)\"").matcher(b.toString());
                    if (m.find()) {
                        return m.group(1).trim();
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static void checkUpdatesInBackground(Project project, boolean notifyIfUpToDate) {
        checkUpdates(project, null, notifyIfUpToDate);
    }

    private static boolean isNewerVersion(String latest, String current) {
        if (latest == null || current == null) return false;
        String[] lParts = latest.replace("v", "").split("\\.");
        String[] cParts = current.replace("v", "").split("\\.");
        int len = Math.max(lParts.length, cParts.length);

        for (int i = 0; i < len; i++) {
            int l = i < lParts.length ? parseSafe(lParts[i]) : 0;
            int c = i < cParts.length ? parseSafe(cParts[i]) : 0;
            if (l > c) return true;
            if (l < c) return false;
        }
        return false;
    }

    private static int parseSafe(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
