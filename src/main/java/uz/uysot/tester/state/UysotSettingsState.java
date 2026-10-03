package uz.uysot.tester.state;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

@State(
    name = "uz.uysot.tester.state.UysotSettingsState",
    storages = @Storage("UysotApiTesterSettings.xml")
)
public class UysotSettingsState implements PersistentStateComponent<UysotSettingsState> {

    public String repoUrl = "https://github.com/hakimbek-qa/uysot-open-api-automation.git";
    public String clonePath = System.getProperty("user.home") + File.separator + ".uysot_api_tests";
    public String baseUrl = "https://openapi.app-dev.uysot.uz";
    public String token = "";
    public int selectedSuiteIndex = 0;

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
    }
}
