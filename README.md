# Uysot API Tester - Universal IntelliJ IDEA & PyCharm Plugini

Backend dasturchilar uchun Uysot Open API va boshqa xizmatlarning avtomatlashtirilgan testlarini to'g'ridan-to'g'ri **IntelliJ IDEA** yoki **PyCharm** ichida ishga tushirish, AI (ChatGPT, GitHub Copilot, Claude) yordamida testlar yozish, lokalda tekshirish va Git repozitoriyasiga push qilish uchun mo'ljallangan universal plagin.

---

## 🎯 Asosiy imkoniyatlari

1. **Universal Repozitoriya Ulanishi (Multi-Repo & Dynamic Discovery):**
   - Istalgan QA test Git repozitoriyasini ulash (`https://github.com/...`).
   - Repozitoriyadagi testlarni avtomatik aniqlash (Dynamic Discovery) yoki `.tester.json` konfiguratsiyasidan o'qish.
   - Har bir loyiha o'zining alohida kesh papkasiga (`~/.uysot_tester_repos/<repo_nomi>`) tushadi.

2. **🤖 Loyiha Qoidalariga Asoslangan AI Prompt (AI_TEST_RULES.md):**
   - Har bir QA repozitoriyasining ildiziga `AI_TEST_RULES.md` joylashtiriladi (yoki `.tester.json` dagi `aiRulesFile` da ko'rsatiladi).
   - Plagin AI prompt yaratishda to'g'ridan-to'g'ri ushbu fayldagi qoidalarni (fayllar joylashuvi, majburiy importlar, 429 kutish qoidalari, `check_success`, `wait_request`, modul fixture'lari) oladi.
   - Natijada AI faqat va faqat ushbu loyiha arxitekturasiga mos test kodini yozadi, loyiha tuzilmasidan chetga chiqmaydi.
   - "AI Prompt" oynasida qoidalar faylini bevosita IDE da ochish va tahrirlash imkoniyati mavjud.

3. **➕ Yangi Test Faylini Yaratish:**
   - Bir tugma orqali yangi test faylini (`test_<nomi>.py`) to'g'ri papkada shablon bilan yaratish.
   - Fayl avtomatik ravishda IDE muharririda ochiladi — AI bergan kodni joylash va saqlash kifoya.

4. **📂 Lokal Test Papkasi:**
   - Dasturchi QA loyihasini alohida clone qilib yurmasdan, bitta tugma bilan uning lokal papkasini fayl boshqaruvchisida (Finder / Explorer) ochishi mumkin.

5. **🔄 Lokal Testlarni Darhol Aniqlash va Sinash:**
   - Yangi yozilgan `test_*.py` fayllari Git'ga push qilinmasdan avval ro'yxatda paydo bo'ladi.
   - Dasturchi o'zining mahalliy serverida (`http://localhost:8080`, `localhost:8000`) testni to'liq tekshirib olishi mumkin.

6. **📤 O'rnatilgan Git Push (Commit & Push dialogi):**
   - Testlar muvaffaqiyatli o'tgach, bitta tugma orqali o'zgartirilgan va yangi qo'shilgan fayllarni tanlash.
   - Commit xabarini kiritish va IntelliJ'ning saqlangan GitHub hisobi orqali to'g'ridan-to'g'ri masofaviy repozitoriyaga push qilish (terminal yoki parol kiritish shart emas).

7. **Xatoliklarni aniq tahlil qilish:**
   - Yiqilgan test tanlanganda:
     - 📌 **So'rov:** `POST /v1/open-api/lead`
     - 🔍 **Parametrlar:** `?page=1`
     - 📦 **Yuborilgan Body:** JSON formatda
     - ⚠️ **Status kod:** `400` yoki `500`
     - 📄 **Server Javobi:** Server qaytargan asl xato xabari.

---

## 🔌 O'rnatish va Avtomatik Yangilanishlar (Custom Repository)

IntelliJ IDEA yoki PyCharm da:
1. **Settings** (`Cmd + ,` yoki `Ctrl + Alt + S`) $\rightarrow$ **Plugins** bo'limiga o'ting.
2. Tepadagi ⚙️ (tishli g'ildirak) belgisini bosing $\rightarrow$ **Manage Plugin Repositories...** ni tanlang.
3. **`+`** tugmasini bosib, quyidagi havolani kiriting:
   ```text
   https://raw.githubusercontent.com/hakimbek-qa/uysot-api-tester-plugin/main/updatePlugins.xml
   ```
4. **OK** tugmasini bosing.
5. **Plugins $\rightarrow$ Marketplace** tabida:
   `Uysot Open API Tester`
   deb qidiring va **Install** tugmasini bosing!
6. IDE ni qayta ishga tushiring (Restart IDE).

---

## 🚀 Qanday ishlatiladi? (AI orqali test yozish oqimi)

1. IDE ning o'ng panelida **"Uysot API Tester"** yorlig'ini bosing.
2. **"🔗 Connect & Sync"** tugmasini bosing.
3. **"🤖 AI Prompt"** tugmasini bosib, tayyor ko'rsatmani ChatGPT / Copilot / Claude'ga yuboring va test kodini oling.
4. **"➕ Yangi test"** tugmasini bosib fayl oching va olingan test kodini unga joylang (`Ctrl + S`).
5. **"🎯 Test to'plami"** ro'yxatidan yangi testni tanlang va **"▶ Ishga tushirish"** orqali tekshiring.
6. Hamma testlar yashil (✅) bo'lgach, **"📤 Git Push"** tugmasi orqali o'zgarishlarni QA repozitoriyasiga push qiling!

---

## ⚙️ QA Repozitoriyalari uchun `.tester.json` formati (Ixtiyoriy)

Test repozitoriyasining root qismiga `.tester.json` fayli joylansa, plagin testlarni o'zbekcha chiroyli nomlar bilan chiqaradi:

```json
{
  "name": "Uysot Open API Testlari",
  "defaultBaseUrl": "https://openapi.app-dev.uysot.uz",
  "authType": "X-Auth-Token",
  "tokenLabel": "X-Auth-Token",
  "suites": [
    {
      "name": "🚀 Barcha testlar",
      "params": "pytest_uysot",
      "description": "Barcha testlarni to'liq ishga tushirish"
    },
    {
      "name": "👤 Lid flow",
      "params": "pytest_uysot/test_lead_flow.py",
      "description": "Lid yaratish va boshqarish testlari"
    }
  ]
}
```

*Agar yangi test fayllari qo'shilsa, plagin ularni avtomatik tarzda aniqlaydi va ro'yxatda `📄 [Lokal]` belgisi bilan ko'rsatadi.*

---

## 🚀 Yangi versiya chiqarish (CI/CD)

```bash
git tag v1.2.0
git push origin v1.2.0
```
GitHub Actions avtomatik ravishda plaginni yig'ib, reliz yaratadi va `updatePlugins.xml` orqali yangilanishni barcha dasturchilarga yetkazadi.
