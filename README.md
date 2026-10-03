# Uysot Open API Tester - IntelliJ IDEA Plugini

Backend dasturchilar uchun Uysot Open API avtomatlashtirilgan testlarini to'g'ridan-to'g'ri **IntelliJ IDEA** ichida ishga tushirish, mahalliy serverni (`http://localhost:8080`) sinash va xatoliklarni tahlil qilish uchun mo'ljallangan maxsus plagin.

---

## 🎯 Asosiy imkoniyatlari

1. **Mahalliy backendni (`localhost`) testlash:**
   - Dasturchi kodini git'ga push qilmasdan turib, o'z kompyuteridagi `http://localhost:8080`, `localhost:8000` yoki masofaviy `dev/staging` serverlarini testdan o'tkazishi mumkin.
2. **Fon rejimida testlarni yuklab olish (Git Sync):**
   - QA test repozitoriyasi (`https://github.com/hakimbek-qa/uysot-open-api-automation.git`) dasturchining kompyuteriga yashirin papkaga (`~/.uysot_api_tests`) tushadi.
   - Dasturchi test kodlarini ochishi yoki fayllar bilan bosh qotirishi shart emas.
   - Bitta **"🔄 Testlarni yangilash (Git Pull)"** tugmasi orqali eng so'nggi testlar yuklab olinadi.
3. **Qulay interfeys (Tool Window):**
   - IntelliJ IDEA ning o'ng yon panelida ochiladi.
   - Kerakli test to'plamini tanlash (Lid flow, To'lov flow, Shartnomalar, Barcha testlar va h.k.).
4. **Xatoliklarni aniq tahlil qilish:**
   - Yiqilgan test tanlanganda:
     - 📌 **So'rov:** `POST /v1/open-api/lead`
     - 🔍 **Parametrlar:** `?page=1`
     - 📦 **Yuborilgan Body:** JSON formatda
     - ⚠️ **Status kod:** `400` yoki `500`
     - 📄 **Server Javobi:** Server qaytargan asl xato xabari.

---

## 🛠️ Plaginni yig'ish (Build)

Loyihada plagin zip faylini hosil qilish uchun terminalda quyidagi buyruqni bering:

```bash
# loyiha papkasiga kiring
cd /Users/user/IdeaProjects/uysot-api-tester-plugin

# plaginni yig'ish
gradle buildPlugin
# yoki
./gradlew buildPlugin
```

Yig'ilgan fayl quyidagi manzilda tayyor bo'ladi:
📁 `build/distributions/uysot-api-tester-plugin-1.0.0.zip`

---

## 🔌 IntelliJ IDEA ga o'rnatish

1. IntelliJ IDEA ni oching.
2. **Settings** (yoki macOS'da `Cmd + ,`) $\rightarrow$ **Plugins** bo'limiga o'ting.
3. Tepadagi ⚙️ (tishli g'ildirak) belgisini bosing va **"Install Plugin from Disk..."** ni tanlang.
4. Yig'ilgan `uysot-api-tester-plugin-1.0.0.zip` faylini tanlang va **OK** ni bosing.
5. IntelliJ IDEA ni qayta ishga tushiring (Restart IDE).

---

## 🚀 Qanday ishlatiladi?

1. IntelliJ IDEA ning o'ng panelida **"Uysot API Tester"** yorlig'ini bosing.
2. **Base URL** ni tanlang (masalan, `http://localhost:8080` yoki `https://openapi.app-dev.uysot.uz`).
3. **Open API Token** ni kiriting (bir marta kiritilsa eslab qolinadi).
4. **"🔄 Testlarni yangilash (Git Pull)"** tugmasini bosing (birinchi marta QA reponi avtomatik klonlaydi).
5. Kerakli test to'plamini tanlab, **"▶ Ishga tushirish"** tugmasini bosing!
