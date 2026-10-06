package uz.uysot.tester.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import uz.uysot.tester.model.ApiErrorDetails;
import uz.uysot.tester.model.TestRunResult;
import uz.uysot.tester.model.TestSuiteOption;
import uz.uysot.tester.service.AiPromptService;
import uz.uysot.tester.service.GitSyncService;
import uz.uysot.tester.service.TestDiscoveryService;
import uz.uysot.tester.service.TestRunnerService;
import uz.uysot.tester.service.UpdateCheckerService;
import uz.uysot.tester.state.UysotSettingsState;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.List;

public class UysotToolWindowPanel {
    private final Project project;
    private final JPanel mainPanel;

    private JComboBox<String> repoComboBox;
    private JButton connectButton;
    private JComboBox<String> urlComboBox;
    private JComboBox<String> authTypeComboBox;
    private JPasswordField tokenField;
    private JComboBox<TestSuiteOption> suiteComboBox;
    private JButton runButton;
    private JButton stopButton;
    private JButton refreshSuitesButton;
    private JButton aiPromptButton;
    private JButton newTestButton;
    private JButton openFolderButton;
    private JButton gitPushButton;
    private JLabel statusLabel;
    private JProgressBar progressBar;

    private DefaultListModel<TestRunResult> listModel;
    private JBList<TestRunResult> resultList;
    private JTextArea detailTextArea;
    private JTextArea consoleTextArea;
    private JBTabbedPane tabbedPane;

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

        // Row 1: Repository URL + Connect Button
        JPanel repoPanel = new JPanel(new BorderLayout(5, 5));
        repoPanel.add(new JBLabel("🔗 QA Repozitoriyasi:"), BorderLayout.WEST);

        DefaultComboBoxModel<String> repoModel = new DefaultComboBoxModel<>();
        for (String url : settings.recentRepoUrls) {
            repoModel.addElement(url);
        }
        repoComboBox = new JComboBox<>(repoModel);
        repoComboBox.setEditable(true);
        if (settings.repoUrl != null) {
            repoComboBox.setSelectedItem(settings.repoUrl);
        }
        repoPanel.add(repoComboBox, BorderLayout.CENTER);

        connectButton = new JButton("🔗 Connect & Sync");
        connectButton.setToolTipText("Repozitoriyani yuklab olish / yangilash va testlarni aniqlash");
        repoPanel.add(connectButton, BorderLayout.EAST);
        topPanel.add(repoPanel);
        topPanel.add(Box.createVerticalStrut(6));

        // Row 2: Base URL
        JPanel urlPanel = new JPanel(new BorderLayout(5, 5));
        urlPanel.add(new JBLabel("🌐 Server Base URL:  "), BorderLayout.WEST);
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

        // Row 3: Auth Token (X-Auth-Token / Bearer / etc.)
        JPanel tokenPanel = new JPanel(new BorderLayout(5, 5));
        JPanel authLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        authLabelPanel.add(new JBLabel("🔑 Auth Token:"));
        authTypeComboBox = new JComboBox<>(new String[]{
                "X-Auth-Token",
                "Bearer",
                "X-Open-Api-Token",
                "API Key"
        });
        if (settings.authType != null && !settings.authType.isEmpty()) {
            authTypeComboBox.setSelectedItem(settings.authType);
        }
        authLabelPanel.add(authTypeComboBox);
        tokenPanel.add(authLabelPanel, BorderLayout.WEST);

        tokenField = new JPasswordField(settings.token, 20);
        tokenField.setToolTipText("Open API / Backend autentifikatsiya tokeni");
        tokenPanel.add(tokenField, BorderLayout.CENTER);
        topPanel.add(tokenPanel);
        topPanel.add(Box.createVerticalStrut(6));

        // Row 4: Test Suite
        JPanel suitePanel = new JPanel(new BorderLayout(5, 5));
        suitePanel.add(new JBLabel("🎯 Test to'plami:     "), BorderLayout.WEST);
        suiteComboBox = new JComboBox<>();
        suitePanel.add(suiteComboBox, BorderLayout.CENTER);

        refreshSuitesButton = new JButton("🔄");
        refreshSuitesButton.setToolTipText("Lokal papkadagi yangi va mavjud testlarni qayta aniqlash");
        suitePanel.add(refreshSuitesButton, BorderLayout.EAST);
        topPanel.add(suitePanel);
        topPanel.add(Box.createVerticalStrut(8));

        // Row 5: Action Buttons (Run / Stop / Version)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        runButton = new JButton("▶ Ishga tushirish");
        runButton.setFont(runButton.getFont().deriveFont(Font.BOLD));
        runButton.setBackground(new JBColor(new Color(46, 125, 50), new Color(46, 125, 50)));

        stopButton = new JButton("⏹ To'xtatish");
        stopButton.setEnabled(false);

        buttonPanel.add(runButton);
        buttonPanel.add(stopButton);

        JButton updateCheckButton = new JButton("ℹ️ v" + UpdateCheckerService.CURRENT_VERSION);
        updateCheckButton.setToolTipText("Yangilanishlarni tekshirish");
        updateCheckButton.addActionListener(e -> UpdateCheckerService.checkUpdates(project, mainPanel, true));
        buttonPanel.add(updateCheckButton);

        topPanel.add(buttonPanel);
        topPanel.add(Box.createVerticalStrut(6));

        // Row 6: AI & Developer Tools Toolbar
        JPanel devToolsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));

        aiPromptButton = new JButton("🤖 AI Prompt");
        aiPromptButton.setToolTipText("AI (Copilot/Claude/ChatGPT) uchun tayyor test yozish ko'rsatmasini tayyorlash");

        newTestButton = new JButton("➕ Yangi test");
        newTestButton.setToolTipText("Yangi test fayli shablonini yaratish va muharrirda ochish");

        openFolderButton = new JButton("📂 Test papkasi");
        openFolderButton.setToolTipText("Lokal testlar papkasini fayl boshqaruvchisida ochish");

        gitPushButton = new JButton("📤 Git Push");
        gitPushButton.setToolTipText("Lokalda qo'shilgan yoki o'zgartirilgan testlarni Git repozitoriyasiga push qilish");

        devToolsPanel.add(aiPromptButton);
        devToolsPanel.add(newTestButton);
        devToolsPanel.add(openFolderButton);
        devToolsPanel.add(gitPushButton);

        topPanel.add(devToolsPanel);
        topPanel.add(Box.createVerticalStrut(6));

        // Row 7: Status & Progress
        JPanel statusPanel = new JPanel(new BorderLayout(5, 2));
        statusLabel = new JBLabel("Tayyor");
        progressBar = new JProgressBar();
        progressBar.setVisible(false);
        statusPanel.add(statusLabel, BorderLayout.WEST);
        statusPanel.add(progressBar, BorderLayout.EAST);
        topPanel.add(statusPanel);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // 2. Tabs: Results & Console
        tabbedPane = new JBTabbedPane();

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

        // Initial test suite discovery if repo directory exists
        loadSuitesForCurrentRepo(false);

        // Events
        connectButton.addActionListener(e -> executeConnectAndSync());
        runButton.addActionListener(e -> executeTests());
        stopButton.addActionListener(e -> executeStop());
        refreshSuitesButton.addActionListener(e -> loadSuitesForCurrentRepo(true));

        aiPromptButton.addActionListener(e -> executeAiPrompt());
        newTestButton.addActionListener(e -> executeNewTest());
        openFolderButton.addActionListener(e -> executeOpenFolder());
        gitPushButton.addActionListener(e -> executeGitPush());

        repoComboBox.addActionListener(e -> {
            String selected = getSelectedRepoUrl();
            if (selected != null && !selected.isEmpty()) {
                File dir = UysotSettingsState.getRepoDirectory(selected);
                if (dir.exists() && new File(dir, ".git").exists()) {
                    loadSuitesForCurrentRepo(false);
                }
            }
        });

        UpdateCheckerService.checkUpdates(project, mainPanel, false);
    }

    private String getSelectedRepoUrl() {
        Object item = repoComboBox.getEditor().getItem();
        return item != null ? item.toString().trim() : "";
    }

    private void loadSuitesForCurrentRepo(boolean notifyUser) {
        String repoUrl = getSelectedRepoUrl();
        File repoDir = UysotSettingsState.getRepoDirectory(repoUrl);

        if (!repoDir.exists() || !new File(repoDir, ".git").exists()) {
            DefaultComboBoxModel<TestSuiteOption> emptyModel = new DefaultComboBoxModel<>();
            for (TestSuiteOption suite : TestSuiteOption.getDefaultSuites()) {
                emptyModel.addElement(suite);
            }
            suiteComboBox.setModel(emptyModel);
            if (notifyUser) {
                JOptionPane.showMessageDialog(mainPanel,
                        "Repozitoriya hali lokalga yuklanmagan. Avval '🔗 Connect & Sync' tugmasini bosing.",
                        "Repozitoriya topilmadi", JOptionPane.WARNING_MESSAGE);
            }
            return;
        }

        TestDiscoveryService.DiscoveryResult result = TestDiscoveryService.discover(repoDir);
        DefaultComboBoxModel<TestSuiteOption> model = new DefaultComboBoxModel<>();
        for (TestSuiteOption opt : result.getSuites()) {
            model.addElement(opt);
        }
        suiteComboBox.setModel(model);

        if (result.getDefaultBaseUrl() != null && !result.getDefaultBaseUrl().isEmpty()) {
            urlComboBox.setSelectedItem(result.getDefaultBaseUrl());
        }

        if (result.getAuthType() != null && !result.getAuthType().isEmpty()) {
            authTypeComboBox.setSelectedItem(result.getAuthType());
        }

        statusLabel.setText("Tayyor (" + result.getSuites().size() + " ta to'plam)");
        if (notifyUser) {
            JOptionPane.showMessageDialog(mainPanel,
                    "Test to'plamlari muvaffaqiyatli aniqlandi (" + result.getSuites().size() + " ta to'plam).",
                    "Muvaffaqiyatli", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void saveSettings() {
        UysotSettingsState settings = UysotSettingsState.getInstance();
        String repoUrl = getSelectedRepoUrl();
        if (!repoUrl.isEmpty()) {
            settings.addRecentRepoUrl(repoUrl);
        }

        Object selectedUrl = urlComboBox.getSelectedItem();
        if (selectedUrl != null) {
            settings.baseUrl = selectedUrl.toString().trim();
        }
        Object selectedAuth = authTypeComboBox.getSelectedItem();
        if (selectedAuth != null) {
            settings.authType = selectedAuth.toString();
        }
        settings.token = new String(tokenField.getPassword()).trim();
        settings.selectedSuiteIndex = suiteComboBox.getSelectedIndex();
    }

    private void executeConnectAndSync() {
        saveSettings();
        String repoUrl = getSelectedRepoUrl();
        if (repoUrl.isEmpty()) {
            JOptionPane.showMessageDialog(mainPanel, "Repozitoriya havolasini kiriting!", "Xatolik", JOptionPane.ERROR_MESSAGE);
            return;
        }

        setRunningState(true, "Git orqali ulanmoqda...");
        consoleTextArea.append("\n>>> Git sync boshlandi: " + repoUrl + "\n");

        GitSyncService.syncRepository(project, repoUrl, new GitSyncService.SyncCallback() {
            @Override
            public void onOutput(String line) {
                SwingUtilities.invokeLater(() -> consoleTextArea.append(line + "\n"));
            }

            @Override
            public void onComplete(boolean success, String message, File repoDir) {
                SwingUtilities.invokeLater(() -> {
                    setRunningState(false, message);
                    consoleTextArea.append(">>> " + message + "\n");
                    if (success && repoDir != null) {
                        loadSuitesForCurrentRepo(false);
                        JOptionPane.showMessageDialog(mainPanel,
                                message + "\nTestlar to'plami muvaffaqiyatli yuklandi!",
                                "Muvaffaqiyatli", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(mainPanel, message, "Xatolik", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }
        });
    }

    private void executeAiPrompt() {
        String repoUrl = getSelectedRepoUrl();
        File repoDir = UysotSettingsState.getRepoDirectory(repoUrl);
        if (!repoDir.exists() || !new File(repoDir, ".git").exists()) {
            JOptionPane.showMessageDialog(mainPanel,
                    "Repozitoriya hali lokalga yuklanmagan. Avval '🔗 Connect & Sync' tugmasini bosing.",
                    "Repozitoriya topilmadi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Object selectedUrl = urlComboBox.getSelectedItem();
        String baseUrl = selectedUrl != null ? selectedUrl.toString().trim() : "";
        Object selectedAuth = authTypeComboBox.getSelectedItem();
        String authType = selectedAuth != null ? selectedAuth.toString() : "X-Auth-Token";

        File rulesFile = AiPromptService.findAiRulesFile(repoDir);
        String prompt = AiPromptService.generatePrompt(repoDir, baseUrl, authType);
        AiPromptDialog dialog = new AiPromptDialog(project, rulesFile, prompt);
        if (dialog.showAndGet()) {
            consoleTextArea.append(">>> AI Prompt xotiraga nusxalandi (Clipboard).\n");
            String fileName = rulesFile != null ? rulesFile.getName() : "AI_TEST_RULES.md";
            JOptionPane.showMessageDialog(mainPanel,
                    "✅ AI Prompt nusxalandi!\n" +
                    "Ko'rsatmalar '" + fileName + "' faylidagi loyiha qoidalariga 100% moslangan.\n" +
                    "Uni ChatGPT, GitHub Copilot yoki Claude'ga yuboring.",
                    "Muvaffaqiyatli", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void executeNewTest() {
        String repoUrl = getSelectedRepoUrl();
        File repoDir = UysotSettingsState.getRepoDirectory(repoUrl);
        if (!repoDir.exists() || !new File(repoDir, ".git").exists()) {
            JOptionPane.showMessageDialog(mainPanel,
                    "Repozitoriya hali lokalga yuklanmagan. Avval '🔗 Connect & Sync' tugmasini bosing.",
                    "Repozitoriya topilmadi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String testName = Messages.showInputDialog(mainPanel,
                "Yangi test nomi yoki ssenariysini kiriting (masalan: order_creation yoki payment_flow):",
                "➕ Yangi Test Fayli Yaratish",
                Messages.getQuestionIcon());

        if (testName == null || testName.trim().isEmpty()) {
            return;
        }

        try {
            File createdFile = AiPromptService.createNewTestFile(repoDir, testName.trim());
            AiPromptService.openFileInEditor(project, createdFile);
            loadSuitesForCurrentRepo(false);
            consoleTextArea.append(">>> Yangi test fayli yaratildi va muharrirda ochildi: " + createdFile.getAbsolutePath() + "\n");
            JOptionPane.showMessageDialog(mainPanel,
                    "✅ " + createdFile.getName() + " fayli yaratildi va muharrirda ochildi!\n" +
                            "AI yozib bergan test kodini faylga joylang va saqlang (Ctrl+S / Cmd+S).",
                    "Fayl yaratildi", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(mainPanel, "Fayl yaratishda xatolik: " + ex.getMessage(), "Xatolik", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void executeOpenFolder() {
        String repoUrl = getSelectedRepoUrl();
        File repoDir = UysotSettingsState.getRepoDirectory(repoUrl);
        if (!repoDir.exists() || !new File(repoDir, ".git").exists()) {
            JOptionPane.showMessageDialog(mainPanel,
                    "Repozitoriya hali lokalga yuklanmagan. Avval '🔗 Connect & Sync' tugmasini bosing.",
                    "Repozitoriya topilmadi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        AiPromptService.openRepoInFileManager(repoDir);
        consoleTextArea.append(">>> Lokal test papkasi ochildi: " + repoDir.getAbsolutePath() + "\n");
    }

    private void executeGitPush() {
        String repoUrl = getSelectedRepoUrl();
        File repoDir = UysotSettingsState.getRepoDirectory(repoUrl);
        if (!repoDir.exists() || !new File(repoDir, ".git").exists()) {
            JOptionPane.showMessageDialog(mainPanel,
                    "Repozitoriya hali lokalga yuklanmagan. Avval '🔗 Connect & Sync' tugmasini bosing.",
                    "Repozitoriya topilmadi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        setRunningState(true, "Git holati tekshirilmoqda...");
        consoleTextArea.append("\n>>> Git o'zgarishlari tekshirilmoqda...\n");

        GitSyncService.getChangedFiles(project, repoDir, (success, files, error) -> {
            SwingUtilities.invokeLater(() -> {
                setRunningState(false, "Tayyor");
                if (!success) {
                    JOptionPane.showMessageDialog(mainPanel,
                            "Git status tekshirishda xatolik: " + error,
                            "Xatolik", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (files.isEmpty()) {
                    JOptionPane.showMessageDialog(mainPanel,
                            "Hech qanday o'zgarish topilmadi.\nBarcha testlar Git repozitoriyasi bilan to'liq sinxronlashgan.",
                            "O'zgarishlar yo'q", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                GitCommitPushDialog dialog = new GitCommitPushDialog(project, files);
                if (dialog.showAndGet()) {
                    List<String> selectedPaths = dialog.getSelectedFilePaths();
                    String commitMsg = dialog.getCommitMessage();

                    setRunningState(true, "Git'ga push qilinmoqda...");
                    consoleTextArea.append(">>> Tanlangan fayllar: " + selectedPaths + "\n");
                    consoleTextArea.append(">>> Commit xabari: " + commitMsg + "\n");

                    GitSyncService.commitAndPush(project, repoDir, repoUrl, selectedPaths, commitMsg, new GitSyncService.CommitPushCallback() {
                        @Override
                        public void onOutput(String line) {
                            SwingUtilities.invokeLater(() -> consoleTextArea.append(line + "\n"));
                        }

                        @Override
                        public void onComplete(boolean pushSuccess, String message) {
                            SwingUtilities.invokeLater(() -> {
                                setRunningState(false, message);
                                consoleTextArea.append(">>> " + message + "\n");
                                if (pushSuccess) {
                                    loadSuitesForCurrentRepo(false);
                                    JOptionPane.showMessageDialog(mainPanel,
                                            "✅ O'zgarishlar muvaffaqiyatli Git repozitoriyasiga push qilindi!",
                                            "Muvaffaqiyatli", JOptionPane.INFORMATION_MESSAGE);
                                } else {
                                    JOptionPane.showMessageDialog(mainPanel, message, "Xatolik", JOptionPane.ERROR_MESSAGE);
                                }
                            });
                        }
                    });
                }
            });
        });
    }

    private void executeTests() {
        saveSettings();
        listModel.clear();
        detailTextArea.setText("Testlar ishga tushirilmoqda...");
        consoleTextArea.setText("");

        TestSuiteOption suite = (TestSuiteOption) suiteComboBox.getSelectedItem();
        String pytestArgs = suite != null ? suite.getPytestArgs() : "pytest_uysot";

        String repoUrl = getSelectedRepoUrl();
        File repoDir = UysotSettingsState.getRepoDirectory(repoUrl);

        UysotSettingsState settings = UysotSettingsState.getInstance();
        File venvDir = new File(repoDir, ".venv");
        if (!venvDir.exists()) {
            tabbedPane.setSelectedIndex(1);
        }

        setRunningState(true, "Testlar bajarilmoqda...");

        TestRunnerService.runTests(repoDir, settings.baseUrl, settings.token, pytestArgs, new TestRunnerService.TestRunCallback() {
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
                    if (total > 0) {
                        tabbedPane.setSelectedIndex(0);
                    }
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
        connectButton.setEnabled(!running);
        runButton.setEnabled(!running);
        stopButton.setEnabled(running);
        refreshSuitesButton.setEnabled(!running);
        aiPromptButton.setEnabled(!running);
        newTestButton.setEnabled(!running);
        gitPushButton.setEnabled(!running);
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
