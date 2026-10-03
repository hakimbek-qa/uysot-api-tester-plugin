# Uysot API Tester - Universal IntelliJ IDEA & PyCharm Plugini

Backend dasturchilar uchun Uysot Open API va boshqa xizmatlarning avtomatlashtirilgan testlarini to'g'ridan-to'g'ri **IntelliJ IDEA** yoki **PyCharm** ichida ishga tushirish, mahalliy serverni (`http://localhost:8080`) sinash va xatoliklarni tahlil qilish uchun mo'ljallangan universal plagin.

---

## 🎯 Asosiy imkoniyatlari

1. **Universal Repozitoriya Ulanishi (Multi-Repo & Dynamic Discovery):**
   - Istalgan QA test Git repozitoriyasini ulash (`https://github.com/...`).
   - Repozitoriyadagi testlarni avtomatik aniqlash (Dynamic Discovery) yoki `.tester.json` konfiguratsiyasidan o'qish.
   - Har bir loyiha o'zining alohida kesh papkasiga (`~/.uysot_tester_repos/<repo_nomi>`) tushadi.
2. **Mahalliy backendni (`localhost`) testlash:**
   - Dasturchi kodini git'ga push qilmasdan turib, o'z kompyuteridagi `http://localhost:8080`, `localhost:8000` yoki masofaviy dev/staging serverlarini testdan o'tkazishi mumkin.
3. **Standart Bearer Token autentifikatsiyasi:**
   - Token kiritiladi va test muhitiga avtomatik ravishda `OPEN_API_TOKEN`, `AUTH_TOKEN` va `BEARER_TOKEN` orqali uzatiladi.
4. **Xatoliklarni aniq tahlil qilish:**
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

## 🚀 Qanday ishlatiladi?

1. IDE ning o'ng panelida **"Uysot API Tester"** yorlig'ini bosing.
2. **QA Repozitoriyasi** maydoniga Git linkni kiriting (standart: `https://github.com/hakimbek-qa/uysot-open-api-automation.git`).
3. **"🔗 Connect & Sync"** tugmasini bosing (repo yuklanadi va testlar dinamik aniqlanadi).
4. **Server Base URL** ni tanlang (`http://localhost:8080`).
5. **Bearer Token** ni kiriting.
6. Kerakli test to'plamini tanlab, **"▶ Ishga tushirish"** tugmasini bosing!

---

## ⚙️ QA Repozitoriyalari uchun `.tester.json` formati (Ixtiyoriy)

Test repozitoriyasining root qismiga `.tester.json` fayli joylansa, plagin testlarni o'zbekcha chiroyli nomlar bilan chiqaradi:

```json
{
  "name": "Mening Servisim API Testlari",
  "defaultBaseUrl": "http://localhost:8080",
  "authType": "bearer",
  "tokenEnv": "OPEN_API_TOKEN",
  "suites": [
    {
      "name": "🚀 Barcha testlar",
      "params": "tests",
      "description": "Barcha testlarni to'liq ishga tushirish"
    },
    {
      "name": "👤 Autentifikatsiya flow",
      "params": "tests/test_auth.py",
      "description": "Login, register va profil testlari"
    }
  ]
}
```

*Agar `.tester.json` bo'lmasa, plagin repodagi barcha `test_*.py` fayllari va papkalarni avtomatik o'zi topib ro'yxatga chiqaradi.*

---

## 🚀 Yangi versiya chiqarish (CI/CD)

```bash
git tag v1.1.0
git push origin v1.1.0
```
GitHub Actions avtomatik yig'ib, reliz yaratadi va barcha foydalanuvchilarga yangilanish yetkazadi.
