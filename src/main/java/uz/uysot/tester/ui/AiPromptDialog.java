package uz.uysot.tester.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.Nullable;
import uz.uysot.tester.service.AiPromptService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;

public class AiPromptDialog extends DialogWrapper {
    private final Project project;
    private final File rulesFile;
    private final String promptText;
    private JTextArea textArea;

    public AiPromptDialog(@Nullable Project project, @Nullable File rulesFile, String promptText) {
        super(project, true);
        this.project = project;
        this.rulesFile = rulesFile;
        this.promptText = promptText;
        setTitle("🤖 AI Test Qoidalari va Prompt");
        setOKButtonText("📋 Nusxalash (Clipboard)");
        setCancelButtonText("Yopish");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setPreferredSize(new Dimension(680, 480));

        // Top info header
        JPanel headerPanel = new JPanel(new BorderLayout(8, 4));
        headerPanel.setBorder(new EmptyBorder(0, 0, 6, 0));

        if (rulesFile != null && rulesFile.exists()) {
            JBLabel infoLabel = new JBLabel(String.format(
                    "<html><b>📌 Loyiha qoidalari fayli:</b> <code>%s</code><br>" +
                    "<span style='color: gray;'>AI faqat ushbu loyiha qoidalaridan chiqmagan holda test yozadi.</span></html>",
                    rulesFile.getName()
            ));
            headerPanel.add(infoLabel, BorderLayout.CENTER);

            JButton openRulesBtn = new JButton("✏️ " + rulesFile.getName() + " ni ochish");
            openRulesBtn.setToolTipText("Loyiha qoidalari faylini IDE muharririda ochish va tahrirlash");
            openRulesBtn.addActionListener(e -> {
                if (project != null) {
                    AiPromptService.openFileInEditor(project, rulesFile);
                }
            });
            headerPanel.add(openRulesBtn, BorderLayout.EAST);
        } else {
            JBLabel infoLabel = new JBLabel(
                    "<html><b>⚠️ Loyiha qoidalari fayli (AI_TEST_RULES.md) topilmadi.</b><br>" +
                    "<span style='color: gray;'>Standart ko'rsatmalar ishlatilmoqda. Loyiha ildiziga <code>AI_TEST_RULES.md</code> qo'shsangiz, AI faqat ushbu loyiha qoidalariga bo'ysunadi.</span></html>"
            );
            headerPanel.add(infoLabel, BorderLayout.CENTER);
        }

        panel.add(headerPanel, BorderLayout.NORTH);

        // Center text area
        textArea = new JTextArea(promptText);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setEditable(false);
        textArea.setMargin(new Insets(8, 8, 8, 8));
        textArea.setBackground(new JBColor(new Color(245, 245, 245), new Color(43, 43, 43)));

        JBScrollPane scrollPane = new JBScrollPane(textArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    @Override
    protected void doOKAction() {
        AiPromptService.copyToClipboard(textArea.getText());
        super.doOKAction();
    }
}
