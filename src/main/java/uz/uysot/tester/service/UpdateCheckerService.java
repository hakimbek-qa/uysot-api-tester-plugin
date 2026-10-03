package uz.uysot.tester.service;

import com.intellij.ide.BrowserUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateCheckerService {
    private static final Logger LOG = Logger.getInstance(UpdateCheckerService.class);
    public static final String CURRENT_VERSION = "1.1.0";
    public static final String UPDATE_XML_URL = "https://raw.githubusercontent.com/hakimbek-qa/uysot-api-tester-plugin/main/updatePlugins.xml";
    public static final String RELEASES_PAGE_URL = "https://github.com/hakimbek-qa/uysot-api-tester-plugin/releases";

    public static void checkUpdatesInBackground(Project project, boolean notifyIfUpToDate) {
        new Thread(() -> {
            try {
                URL url = new URL(UPDATE_XML_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() == 200) {
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line).append("\n");
                        }
                    }

                    Matcher matcher = Pattern.compile("version=\"([^\"]+)\"").matcher(sb.toString());
                    if (matcher.find()) {
                        String latestVersion = matcher.group(1).trim();

                        if (isNewerVersion(latestVersion, CURRENT_VERSION)) {
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
                    }
                }

                if (notifyIfUpToDate) {
                    Notification notification = new Notification(
                            "Uysot Notifications",
                            "Uysot API Tester",
                            "Sizda eng so'nggi versiya (v" + CURRENT_VERSION + ") o'rnatilgan.",
                            NotificationType.INFORMATION
                    );
                    Notifications.Bus.notify(notification, project);
                }

            } catch (Exception e) {
                LOG.debug("Update check failed", e);
                if (notifyIfUpToDate) {
                    Notification notification = new Notification(
                            "Uysot Notifications",
                            "Uysot API Tester",
                            "Yangilanishlarni tekshirishda xatolik yuz berdi.",
                            NotificationType.WARNING
                    );
                    Notifications.Bus.notify(notification, project);
                }
            }
        }).start();
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
