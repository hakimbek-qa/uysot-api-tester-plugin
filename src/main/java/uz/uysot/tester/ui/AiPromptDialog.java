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

public class AiPromptDialog extends DialogWrapper {
    private final String promptText;
    private JTextArea textArea;

    public AiPromptDialog(@Nullable Project project, String promptText) {
        super(project, true);
        this.promptText = promptText;
        setTitle("🤖 AI Test Yozish Prompti");
        setOKButtonText("📋 Nusxalash (Clipboard)");
        setCancelButtonText("Yopish");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setPreferredSize(new Dimension(650, 450));

        JBLabel infoLabel = new JBLabel("<html><b>AI uchun tayyor ko'rsatma:</b> Quyidagi matnni nusxalab, ChatGPT, GitHub Copilot yoki Claude'ga yuboring. AI ushbu repozitoriya talablariga mos kod yozib beradi.</html>");
        infoLabel.setBorder(new EmptyBorder(0, 0, 6, 0));
        panel.add(infoLabel, BorderLayout.NORTH);

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
