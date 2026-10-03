package uz.uysot.tester.ui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import uz.uysot.tester.model.ApiErrorDetails;
import uz.uysot.tester.model.TestRunResult;
import uz.uysot.tester.model.TestSuiteOption;
import uz.uysot.tester.service.GitSyncService;
import uz.uysot.tester.service.TestRunnerService;
import uz.uysot.tester.state.UysotSettingsState;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class UysotToolWindowPanel {
    private final Project project;
    private final JPanel mainPanel;

    private JComboBox<String> urlComboBox;
    private JPasswordField tokenField;
    private JComboBox<TestSuiteOption> suiteComboBox;
    private JButton runButton;
    private JButton syncButton;
    private JButton stopButton;
    private JLabel statusLabel;
    private JProgressBar progressBar;

    private DefaultListModel<TestRunResult> listModel;
    private JBList<TestRunResult> resultList;
    private JTextArea detailTextArea;
    private JTextArea consoleTextArea;

    public UysotToolWindowPanel(Project project) {
        this.project = project;
        this.mainPanel = new JPanel(new BorderLayout());
        buildUI();
    }

    public JComponent getContent() {
        return mainPanel;
    }

    private void buildUI() {
        UysotSettingsState settings = UysotSettingsState.getInstance();

        // 1. Top Control Panel
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBorder(new EmptyBorder(8, 10, 8, 10));

        // URL row
        JPanel urlPanel = new JPanel(new BorderLayout(5, 5));
        urlPanel.add(new JBLabel("🌐 Base URL:"), BorderLayout.WEST);
        urlComboBox = new JComboBox<>(new String[]{
                "http://localhost:8080",
                "https://openapi.app-dev.uysot.uz",
                "https://api.service.app.uysot.uz",
                "http://localhost:8000",
                "http://localhost:3000"
        });
        urlComboBox.setEditable(true);
        if (settings.baseUrl != null && !settings.baseUrl.isEmpty()) {
            urlComboBox.setSelectedItem(settings.baseUrl);
        }
        urlPanel.add(urlComboBox, BorderLayout.CENTER);
        topPanel.add(urlPanel);
        topPanel.add(Box.createVerticalStrut(6));

        // Token row
        JPanel tokenPanel = new JPanel(new BorderLayout(5, 5));
        tokenPanel.add(new JBLabel("🔑 Open API Token:"), BorderLayout.WEST);
        tokenField = new JPasswordField(settings.token, 20);
        tokenPanel.add(tokenField, BorderLayout.CENTER);
        topPanel.add(tokenPanel);
        topPanel.add(Box.createVerticalStrut(6));

        // Suite row
        JPanel suitePanel = new JPanel(new BorderLayout(5, 5));
        suitePanel.add(new JBLabel("🎯 Test to'plami:"), BorderLayout.WEST);
        List<TestSuiteOption> suites = TestSuiteOption.getDefaultSuites();
        suiteComboBox = new JComboBox<>(suites.toArray(new TestSuiteOption[0]));
        if (settings.selectedSuiteIndex >= 0 && settings.selectedSuiteIndex < suites.size()) {
            suiteComboBox.setSelectedIndex(settings.selectedSuiteIndex);
        }
        suitePanel.add(suiteComboBox, BorderLayout.CENTER);
        topPanel.add(suitePanel);
        topPanel.add(Box.createVerticalStrut(8));

        // Buttons row
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        runButton = new JButton("▶ Ishga tushirish");
        runButton.setFont(runButton.getFont().deriveFont(Font.BOLD));
        runButton.setBackground(new JBColor(new Color(46, 125, 50), new Color(46, 125, 50)));

        syncButton = new JButton("🔄 Testlarni yangilash (Git Pull)");
        stopButton = new JButton("⏹ To'xtatish");
        stopButton.setEnabled(false);

        buttonPanel.add(runButton);
        buttonPanel.add(syncButton);
        buttonPanel.add(stopButton);
        topPanel.add(buttonPanel);
        topPanel.add(Box.createVerticalStrut(6));

        // Status row
        JPanel statusPanel = new JPanel(new BorderLayout(5, 2));
        statusLabel = new JBLabel("Tayyor");
        progressBar = new JProgressBar();
        progressBar.setVisible(false);
        statusPanel.add(statusLabel, BorderLayout.WEST);
        statusPanel.add(progressBar, BorderLayout.EAST);
        topPanel.add(statusPanel);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // 2. Tabs: Results & Console
        JBTabbedPane tabbedPane = new JBTabbedPane();

        // Tab 1: Test Results
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(300);

        listModel = new DefaultListModel<>();
        resultList = new JBList<>(listModel);
        resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showDetail(resultList.getSelectedValue());
            }
        });

        JBScrollPane listScroll = new JBScrollPane(resultList);
        splitPane.setLeftComponent(listScroll);

        detailTextArea = new JTextArea();
        detailTextArea.setEditable(false);
        detailTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        detailTextArea.setMargin(new Insets(8, 8, 8, 8));
        detailTextArea.setText("Chap paneldagi testni tanlang...");
        JBScrollPane detailScroll = new JBScrollPane(detailTextArea);
        splitPane.setRightComponent(detailScroll);

        tabbedPane.addTab("📊 Test Natijalari", splitPane);

        // Tab 2: Console
        consoleTextArea = new JTextArea();
        consoleTextArea.setEditable(false);
        consoleTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        consoleTextArea.setBackground(new JBColor(new Color(245, 245, 245), new Color(35, 35, 35)));
        consoleTextArea.setMargin(new Insets(8, 8, 8, 8));
        JBScrollPane consoleScroll = new JBScrollPane(consoleTextArea);
        tabbedPane.addTab("💻 Konsol / Log", consoleScroll);

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // Events
        runButton.addActionListener(e -> executeTests());
        syncButton.addActionListener(e -> executeSync());
        stopButton.addActionListener(e -> executeStop());
    }

    private void saveSettings() {
        UysotSettingsState settings = UysotSettingsState.getInstance();
        Object selectedUrl = urlComboBox.getSelectedItem();
        if (selectedUrl != null) {
            settings.baseUrl = selectedUrl.toString().trim();
        }
        settings.token = new String(tokenField.getPassword()).trim();
        settings.selectedSuiteIndex = suiteComboBox.getSelectedIndex();
    }

    private void executeSync() {
        saveSettings();
        setRunningState(true, "Testlar yangilanmoqda (Git Sync)...");
        consoleTextArea.append("\n>>> Git sync boshlandi...\n");

        GitSyncService.syncRepository(project, new GitSyncService.SyncCallback() {
            @Override
            public void onOutput(String line) {
                SwingUtilities.invokeLater(() -> consoleTextArea.append(line + "\n"));
            }

            @Override
            public void onComplete(boolean success, String message) {
                SwingUtilities.invokeLater(() -> {
                    setRunningState(false, message);
                    consoleTextArea.append(">>> " + message + "\n");
                    if (success) {
                        JOptionPane.showMessageDialog(mainPanel, message, "Muvaffaqiyatli", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(mainPanel, message, "Xatolik", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }
        });
    }

    private void executeTests() {
        saveSettings();
        listModel.clear();
        detailTextArea.setText("Testlar ishga tushirilmoqda...");
        consoleTextArea.setText("");

        TestSuiteOption suite = (TestSuiteOption) suiteComboBox.getSelectedItem();
        String pytestArgs = suite != null ? suite.getPytestArgs() : "pytest_uysot";

        setRunningState(true, "Testlar bajarilmoqda...");

        TestRunnerService.runTests(pytestArgs, new TestRunnerService.TestRunCallback() {
            @Override
            public void onOutput(String text) {
                SwingUtilities.invokeLater(() -> {
                    consoleTextArea.append(text + "\n");
                    consoleTextArea.setCaretPosition(consoleTextArea.getDocument().getLength());
                });
            }

            @Override
            public void onTestStarted(String testName) {
            }

            @Override
            public void onTestFinished(TestRunResult result) {
                SwingUtilities.invokeLater(() -> {
                    listModel.addElement(result);
                });
            }

            @Override
            public void onRunCompleted(int total, int passed, int failed, int skipped) {
                SwingUtilities.invokeLater(() -> {
                    String statusText = String.format("Yakunlandi: %d ta (✅ %d o'tdi, ❌ %d yiqildi, ⚠️ %d o'tkazildi)",
                            total, passed, failed, skipped);
                    setRunningState(false, statusText);
                });
            }
        });
    }

    private void executeStop() {
        TestRunnerService.stopCurrentRun();
        setRunningState(false, "To'xtatildi.");
        consoleTextArea.append("\n>>> Test foydalanuvchi tomonidan to'xtatildi.\n");
    }

    private void setRunningState(boolean running, String text) {
        runButton.setEnabled(!running);
        syncButton.setEnabled(!running);
        stopButton.setEnabled(running);
        progressBar.setVisible(running);
        progressBar.setIndeterminate(running);
        statusLabel.setText(text);
    }

    private void showDetail(TestRunResult result) {
        if (result == null) {
            detailTextArea.setText("");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=================================================================\n");
        sb.append("Test: ").append(result.getName()).append("\n");
        sb.append("Status: ").append(result.getStatus()).append("\n");
        sb.append("=================================================================\n\n");

        if (result.getStatus() == TestRunResult.Status.PASSED) {
            sb.append("✅ Ushbu test to'liq muvaffaqiyatli yakunlandi.\n");
        } else if (result.getStatus() == TestRunResult.Status.SKIPPED) {
            sb.append("⚠️ Test o'tkazib yuborildi.\n");
        } else {
            ApiErrorDetails err = result.getErrorDetails();
            if (err != null) {
                sb.append("⚠️ API XATOLIGI TAFSILOTLARI:\n\n");
                if (err.getRequestMethod() != null) {
                    sb.append("📌 So'rov:   ").append(err.getRequestMethod()).append(" ").append(err.getRequestUrl() != null ? err.getRequestUrl() : "").append("\n");
                }
                if (err.getParams() != null && !err.getParams().isEmpty() && !err.getParams().equals("—")) {
                    sb.append("🔍 Parametr: ").append(err.getParams()).append("\n");
                }
                if (err.getRequestBody() != null && !err.getRequestBody().isEmpty() && !err.getRequestBody().equals("—")) {
                    sb.append("📦 Body:\n").append(err.getRequestBody()).append("\n\n");
                }
                if (err.getStatusCode() > 0) {
                    sb.append("⚠️ Status:   ").append(err.getStatusCode()).append("\n\n");
                }
                if (err.getResponseBody() != null && !err.getResponseBody().isEmpty()) {
                    sb.append("📄 Server Javobi (Response):\n").append(err.getResponseBody()).append("\n");
                }
            } else {
                sb.append("❌ Tekshiruv sharti (Assert) bajarilmadi yoki API xato qaytardi.\n");
                sb.append("Batafsil ma'lumot uchun 'Konsol / Log' tabiga qarang.\n");
            }
        }

        detailTextArea.setText(sb.toString());
        detailTextArea.setCaretPosition(0);
    }
}
