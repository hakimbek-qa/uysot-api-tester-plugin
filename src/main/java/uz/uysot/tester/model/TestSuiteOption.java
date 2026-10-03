package uz.uysot.tester.model;

import java.util.Arrays;
import java.util.List;

public class TestSuiteOption {
    private final String name;
    private final String pytestArgs;
    private final String description;

    public TestSuiteOption(String name, String pytestArgs, String description) {
        this.name = name;
        this.pytestArgs = pytestArgs;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getPytestArgs() {
        return pytestArgs;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name;
    }

    public static List<TestSuiteOption> getDefaultSuites() {
        return Arrays.asList(
            new TestSuiteOption("Barcha testlar (all)", "pytest_uysot", "Barcha 112 ta Open API testlari"),
            new TestSuiteOption("Faqat xavfsiz / Read-only", "pytest_uysot --ignore=pytest_uysot/test_lead_flow.py --ignore=pytest_uysot/test_lead_task_flow.py --ignore=pytest_uysot/test_lead_history_flow.py --ignore=pytest_uysot/test_payment_flow.py", "Ma'lumot yaratmaydigan faqat o'qish testlari"),
            new TestSuiteOption("Lid flow (test_lead_flow.py)", "pytest_uysot/test_lead_flow.py", "Lid yaratish, tahrirlash, kontaktlar va o'chirish"),
            new TestSuiteOption("To'lov flow (test_payment_flow.py)", "pytest_uysot/test_payment_flow.py", "To'lov yaratish, tekshirish va bekor qilish"),
            new TestSuiteOption("Shartnoma flow (test_contract_flow.py)", "pytest_uysot/test_contract_flow.py", "Shartnomalar, to'lov jadvali va to'lovlar"),
            new TestSuiteOption("Lid vazifalari (test_lead_task_flow.py)", "pytest_uysot/test_lead_task_flow.py", "Vazifa yaratish, yopish va o'chirish"),
            new TestSuiteOption("Lid tarixi (test_lead_history_flow.py)", "pytest_uysot/test_lead_history_flow.py", "Lid tarixi va hodisalar tekshiruvi"),
            new TestSuiteOption("Uy, xonadon, bron (test_house_flat_booking_flow.py)", "pytest_uysot/test_house_flat_booking_flow.py", "Uylar, xonadonlar va bron bog'liqliklari"),
            new TestSuiteOption("GET ma'lumotnomalar (test_get_apis.py)", "pytest_uysot/test_get_apis.py", "Valyuta, crm-field va ma'lumotnomalar"),
            new TestSuiteOption("Filter va sahifalash", "pytest_uysot/test_filter_and_get.py pytest_uysot/test_pagination.py", "Filter va pagination tekshiruvlari")
        );
    }
}
