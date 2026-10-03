package uz.uysot.tester.ui;

import com.intellij.openapi.options.Configurable;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;
import uz.uysot.tester.state.UysotSettingsState;

import javax.swing.*;
import java.awt.*;

public class UysotSettingsConfigurable implements Configurable {
    private JBTextField repoUrlField;
    private JBTextField clonePathField;

    @Nls(capitalized = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "Uysot API Tester";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        panel.add(new JBLabel("Test Repozitoriya URL (Git):"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        repoUrlField = new JBTextField();
        panel.add(repoUrlField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        panel.add(new JBLabel("Lokal saqlash yo'li:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        clonePathField = new JBTextField();
        panel.add(clonePathField, gbc);

        // Spacer to push things up
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        panel.add(new JPanel(), gbc);

        reset();
        return panel;
    }

    @Override
    public boolean isModified() {
        UysotSettingsState settings = UysotSettingsState.getInstance();
        return !repoUrlField.getText().trim().equals(settings.repoUrl) ||
                !clonePathField.getText().trim().equals(settings.clonePath);
    }

    @Override
    public void apply() {
        UysotSettingsState settings = UysotSettingsState.getInstance();
        settings.repoUrl = repoUrlField.getText().trim();
        settings.clonePath = clonePathField.getText().trim();
    }

    @Override
    public void reset() {
        UysotSettingsState settings = UysotSettingsState.getInstance();
        repoUrlField.setText(settings.repoUrl != null ? settings.repoUrl : "");
        clonePathField.setText(settings.clonePath != null ? settings.clonePath : "");
    }
}
