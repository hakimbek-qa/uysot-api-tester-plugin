package uz.uysot.tester.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.Nullable;
import uz.uysot.tester.service.GitSyncService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GitCommitPushDialog extends DialogWrapper {
    private final List<GitSyncService.ChangedFile> changedFiles;
    private final List<JBCheckBox> fileCheckBoxes = new ArrayList<>();
    private JTextArea commitMessageArea;

    public GitCommitPushDialog(@Nullable Project project, List<GitSyncService.ChangedFile> changedFiles) {
        super(project, true);
        this.changedFiles = changedFiles;
        setTitle("📤 Git'ga push qilish (Commit & Push)");
        setOKButtonText("🚀 Commit & Push");
        setCancelButtonText("Bekor qilish");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(8, 10));
        mainPanel.setPreferredSize(new Dimension(580, 420));

        // Top info + select all toggle
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        JBLabel titleLabel = new JBLabel(String.format("<html><b>O'zgarishlar (%d ta fayl):</b> Git repozitoriyasiga kiritish uchun fayllarni belgilang:</html>", changedFiles.size()));
        topPanel.add(titleLabel, BorderLayout.WEST);

        JButton toggleSelectButton = new JButton("Barchasini tanlash/bekor");
        toggleSelectButton.addActionListener(e -> {
            boolean anySelected = fileCheckBoxes.stream().anyMatch(AbstractButton::isSelected);
            boolean newState = !anySelected;
            for (JBCheckBox cb : fileCheckBoxes) {
                cb.setSelected(newState);
            }
        });
        topPanel.add(toggleSelectButton, BorderLayout.EAST);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Center: File list with checkboxes
        JPanel filesListPanel = new JPanel();
        filesListPanel.setLayout(new BoxLayout(filesListPanel, BoxLayout.Y_AXIS));
        filesListPanel.setBorder(new EmptyBorder(6, 6, 6, 6));

        for (GitSyncService.ChangedFile cf : changedFiles) {
            JBCheckBox cb = new JBCheckBox(cf.getStatusBadge() + " " + cf.getRelativePath(), true);
            cb.putClientProperty("filePath", cf.getRelativePath());
            fileCheckBoxes.add(cb);
            filesListPanel.add(cb);
            filesListPanel.add(Box.createVerticalStrut(3));
        }

        JBScrollPane scrollPane = new JBScrollPane(filesListPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Fayllar ro'yxati"));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Bottom: Commit Message input
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.add(new JBLabel("Commit xabari:"), BorderLayout.NORTH);

        commitMessageArea = new JTextArea("test: yangi API test ssenariylari qo'shildi", 3, 20);
        commitMessageArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        commitMessageArea.setLineWrap(true);
        commitMessageArea.setWrapStyleWord(true);
        commitMessageArea.setMargin(new Insets(6, 6, 6, 6));

        JBScrollPane msgScroll = new JBScrollPane(commitMessageArea);
        bottomPanel.add(msgScroll, BorderLayout.CENTER);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        return mainPanel;
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (getSelectedFilePaths().isEmpty()) {
            return new ValidationInfo("Kamida bitta faylni tanlang!");
        }
        if (getCommitMessage().trim().isEmpty()) {
            return new ValidationInfo("Commit xabarini kiriting!", commitMessageArea);
        }
        return null;
    }

    public List<String> getSelectedFilePaths() {
        List<String> selected = new ArrayList<>();
        for (JBCheckBox cb : fileCheckBoxes) {
            if (cb.isSelected()) {
                String path = (String) cb.getClientProperty("filePath");
                if (path != null) {
                    selected.add(path);
                }
            }
        }
        return selected;
    }

    public String getCommitMessage() {
        return commitMessageArea != null ? commitMessageArea.getText().trim() : "";
    }
}
