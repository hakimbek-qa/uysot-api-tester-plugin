package uz.uysot.tester.service;

import com.intellij.ide.BrowserUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateCheckerService {
    private static final Logger LOG = Logger.getInstance(UpdateCheckerService.class);
    public static final String CURRENT_VERSION = "1.1.2";
    public static final String UPDATE_XML_URL = "https://raw.githubusercontent.com/hakimbek-qa/uysot-api-tester-plugin/main/updatePlugins.xml";
    public static final String RELEASES_PAGE_URL = "https://github.com/hakimbek-qa/uysot-api-tester-plugin/releases";

    public static void checkUpdates(Project project, Component parentComponent, boolean interactive) {
        new Thread(() -> {
            try {
                // Add timestamp query parameter to bypass Fastly/GitHub CDN 5-minute cache
                String bypassUrl = UPDATE_XML_URL + "?t=" + System.currentTimeMillis();
                URL url = new URL(bypassUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "UysotApiTesterPlugin/" + CURRENT_VERSION);

                if (conn.getResponseCode() == 200) {
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line).append("\n");
                        }
                    }

                    String xml = sb.toString();
                    Matcher versionMatcher = Pattern.compile("version=\"([^\"]+)\"").matcher(xml);
                    Matcher urlMatcher = Pattern.compile("url=\"([^\"]+)\"").matcher(xml);

                    String latestVersion = versionMatcher.find() ? versionMatcher.group(1).trim() : null;
                    String downloadUrl = urlMatcher.find() ? urlMatcher.group(1).trim() : null;

                    if (latestVersion != null && isNewerVersion(latestVersion, CURRENT_VERSION)) {
                        SwingUtilities.invokeLater(() -> {
                            String message = "🚀 Yangi versiya topildi!\n\n"
                                    + "Hozirgi versiya: v" + CURRENT_VERSION + "\n"
                                    + "Eng so'nggi versiya: v" + latestVersion + "\n\n"
                                    + "Yangilanishni qanday o'rnatmoqchisiz?";

                            Object[] options = {"🌐 Brauzerda yuklab olish (.zip)", "⚙️ Plugins oynasini ochish", "Yopish"};
                            int choice = JOptionPane.showOptionDialog(
                                    parentComponent,
                                    message,
                                    "Yangi versiya: v" + latestVersion,
                                    JOptionPane.YES_NO_CANCEL_OPTION,
                                    JOptionPane.INFORMATION_MESSAGE,
                                    null,
                                    options,
                                    options[0]
                            );

                            if (choice == 0) {
                                String targetUrl = (downloadUrl != null && !downloadUrl.isEmpty())
                                        ? downloadUrl
                                        : RELEASES_PAGE_URL;
                                BrowserUtil.browse(targetUrl);
                            } else if (choice == 1) {
                                ShowSettingsUtil.getInstance().showSettingsDialog(project, "Plugins");
                            }
                        });

                        Notification notification = new Notification(
                                "Uysot Notifications",
                                "🚀 Uysot API Tester: Yangi versiya mavjud!",
                                "Yangi versiya chiqdi: v" + latestVersion + " (Hozirgi: v" + CURRENT_VERSION + ").",
                                NotificationType.INFORMATION
                        );

                        notification.addAction(new NotificationAction("Yangilanishni ko'rish") {
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
                                    + "GitHub'dagi eng so'nggi reliz: v" + (latestVersion != null ? latestVersion : CURRENT_VERSION),
                                    "Uysot API Tester",
                                    JOptionPane.INFORMATION_MESSAGE
                            );
                        });
                    }
                    return;
                }

                int responseCode = conn.getResponseCode();
                if (interactive) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(
                                parentComponent,
                                "Serverdan javob olinmadi (HTTP " + responseCode + ").\nIltimos, internet aloqasini tekshiring.",
                                "Xatolik",
                                JOptionPane.WARNING_MESSAGE
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

    public static void checkUpdatesInBackground(Project project, boolean notifyIfUpToDate) {
        checkUpdates(project, null, notifyIfUpToDate);
    }

    private static boolean isNewerVersion(String latest, String current) {
        String[] lParts = latest.split("\\.");
        String[] cParts = current.split("\\.");
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
