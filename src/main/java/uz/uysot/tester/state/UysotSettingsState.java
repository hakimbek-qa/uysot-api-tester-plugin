package uz.uysot.tester.state;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@State(
    name = "uz.uysot.tester.state.UysotSettingsState",
    storages = @Storage("UysotApiTesterSettings.xml")
)
public class UysotSettingsState implements PersistentStateComponent<UysotSettingsState> {

    public static final String DEFAULT_REPO_URL = "https://github.com/hakimbek-qa/uysot-open-api-automation.git";

    public String repoUrl = DEFAULT_REPO_URL;
    public List<String> recentRepoUrls = new ArrayList<>();
    public String baseUrl = "https://openapi.app-dev.uysot.uz";
    public String token = "";
    public String clonePath = "";
    public int selectedSuiteIndex = 0;

    public UysotSettingsState() {
        if (recentRepoUrls == null) {
            recentRepoUrls = new ArrayList<>();
        }
        if (recentRepoUrls.isEmpty()) {
            recentRepoUrls.add(DEFAULT_REPO_URL);
        }
    }

    public File getCurrentRepoDir() {
        return getRepoDirectory(this.repoUrl);
    }

    public static File getRepoDirectory(String url) {
        if (url == null || url.trim().isEmpty()) {
            url = DEFAULT_REPO_URL;
        }
        String cleanUrl = url.trim();

        // Extract repository slug name (e.g. uysot-open-api-automation)
        String slug = cleanUrl;
        if (slug.endsWith(".git")) {
            slug = slug.substring(0, slug.length() - 4);
        }
        int lastSlash = Math.max(slug.lastIndexOf('/'), slug.lastIndexOf('\\'));
        if (lastSlash >= 0 && lastSlash < slug.length() - 1) {
            slug = slug.substring(lastSlash + 1);
        }
        slug = slug.replaceAll("[^a-zA-Z0-9._-]", "_");

        // Backward compatibility: If ~/.uysot_api_tests exists for default repo, use it
        if ("uysot-open-api-automation".equalsIgnoreCase(slug)) {
            File legacyDir = new File(System.getProperty("user.home"), ".uysot_api_tests");
            if (legacyDir.exists() && new File(legacyDir, ".git").exists()) {
                return legacyDir;
            }
        }

        File baseDir = new File(System.getProperty("user.home"), ".uysot_tester_repos");
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
        return new File(baseDir, slug);
    }

    public void addRecentRepoUrl(String url) {
        if (url == null || url.trim().isEmpty()) return;
        String trimmed = url.trim();
        if (recentRepoUrls == null) {
            recentRepoUrls = new ArrayList<>();
        }
        recentRepoUrls.remove(trimmed);
        recentRepoUrls.add(0, trimmed);
        if (recentRepoUrls.size() > 10) {
            recentRepoUrls = new ArrayList<>(recentRepoUrls.subList(0, 10));
        }
        this.repoUrl = trimmed;
    }

    public static UysotSettingsState getInstance() {
        return ApplicationManager.getApplication().getService(UysotSettingsState.class);
    }

    @Nullable
    @Override
    public UysotSettingsState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull UysotSettingsState state) {
        XmlSerializerUtil.copyBean(state, this);
        if (this.recentRepoUrls == null) {
            this.recentRepoUrls = new ArrayList<>();
        }
        if (this.recentRepoUrls.isEmpty() && this.repoUrl != null) {
            this.recentRepoUrls.add(this.repoUrl);
        }
    }
}
