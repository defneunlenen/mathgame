# 🧮 Defne Matematik — Proje Dokümantasyonu

> 3 yaş civarı çocukların temel matematik becerilerini eğlenceli bir şekilde geliştirecek Android oyunu.

---

## İçindekiler

- [Teknolojiler](#teknolojiler)
- [Proje Yapısı](#proje-yapısı)
- [Oyun Özellikleri](#oyun-özellikleri)
- [Nasıl Derlenir](#nasıl-derlenir)
- [Nasıl Yüklerim (Deploy)](#nasıl-yüklerim-deploy)
- [Ekran Akışı](#ekran-akışı)
- [Veritabanı](#veritabanı)
- [Ses Efektleri](#ses-efektleri)
- [Hızlı Değişiklik Rehberi](#hızlı-değişiklik-rehberi)

---

## Teknolojiler

| Katman | Teknoloji |
|--------|-----------|
| **Dil** | Kotlin |
| **Build** | Gradle 8.14.3 (Kotlin DSL) |
| **Android SDK** | compileSdk 36, minSdk 26, targetSdk 36 |
| **UI** | View Binding, ConstraintLayout, Material Components |
| **Veritabanı** | Room (SQLite) — skor kaydı, top-10 listesi |
| **Ses** | SoundPool — doğru/yanlış cevaplarda anlık ses |
| **Derleme** | Docker — Android SDK dahil portatif build ortamı |

---

## Proje Yapısı

```
math-game/
├── Dockerfile                        # Portatif Android build container'ı
├── android-build.sh                  # Docker ile build + run komutu
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties # Gradle 8.14.3
├── gradlew                           # Gradle wrapper script
├── build.gradle.kts                  # Root Gradle config
├── settings.gradle.kts               # Proje adı, repo'lar
├── gradle.properties                 # JVM args, Kotlin config
├── .gitignore
└── app/
    ├── build.gradle.kts              # App Gradle — dependencies, Android config
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml       # 4 Activity kayıtlı
        ├── java/com/mathgame/
        │   ├── MainActivity.kt            # Ana menü — işlem seçimi
        │   ├── GameActivity.kt            # Oyun ekranı — soru, cevap, ses, efekt
        │   ├── ResultActivity.kt          # Tur sonuçları — puan, yıldız
        │   ├── LeaderboardActivity.kt     # Puan tablosu — en yüksek 10 skor
        │   ├── MathGame.kt                # Soru motoru — 5 işlem tipi
        │   └── Database.kt                # Room entity, DAO, database
        └── res/
            ├── drawable/               # Gradient arka planlar, renkli butonlar
            ├── layout/
            │   ├── activity_main.xml         # Ana menü
            │   ├── activity_game.xml         # Oyun ekranı
            │   ├── activity_result.xml       # Sonuç ekranı
            │   ├── activity_leaderboard.xml  # Puan tablosu
            │   └── item_score.xml            # Liste item template
            ├── mipmap-anydpi/             # Uygulama ikonu
            ├── raw/                      # Ses dosyaları (WAV)
            ├── values/
            │   ├── colors.xml            # Renk paleti
            │   ├── strings.xml           # String kaynakları
            │   └── themes.xml            # Tema, OpButton stili
            └── xml/
                └── network_security_config.xml
```

---

## Oyun Özellikleri

### İşlem Tipleri

| Tip | Aralık | Format |
|-----|--------|--------|
| 🧮 **Toplama** | 1–100 | Alt alta: `  54\n+61\n────` |
| 📐 **Çıkarma** | 1–100 | Alt alta: `  93\n-27\n────` |
| ✖️ **Çarpma** | 2–10 | Yatay: `7 × 8 = ?` |
| ➗ **Bölme** | 2–10 | Yatay: `56 / 8 = ?` (tam bölünen) |
| 🎯 **Yuvarlama** | 1–1000 | Onluk/yüzlüğe yuvarlama |
| 🎲 **Karışık** | Hepsi | Her soru rastgele tip |

### Seçenek Sistemi
- 4 seçenek (2×2 grid, her biri farklı renkte: mavi, mor, turuncu, yeşil)
- **Yazma alanı**: Altta EditText ile manüel cevap da yazılabilir
- Doğru cevap yeşil, yanlış kırmızı parlar
- Yanlışta doğru cevap da gösterilir

### Ses Efektleri
- **Doğru**: Neşeli üç ton (C5→E5→G5) — `sound_correct.wav`
- **Yanlış**: Düşen buzzer — `sound_wrong.wav`
- `SoundPool` ile anlık oyun (gecikme yok)

### Görsel Efektler
- 🎊 Confetti parçacıkları (doğru cevapta)
- 📳 Titreşimli buton (yanlış cevapta)
- Gradient renkli arka planlar, animasyonlu geçişler
- Yıldız derecelendirme (1-3 yıldız)

### Skor Sistemi
- Her doğru: 100 puan
- Room veritabanında kayıtlı
- Top-10 en yüksek skor listesi

---

## Nasıl Derlenir

### Gereksinimler

- **Docker** (yalnızca gerekli, Android SDK yüklü olması gerekmez)
- Host platform: Apple Silicon (Rosetta çevirisi otomatik)

### Build Komutları

```bash
# Debug APK oluştur (ilk çalıştırmada ~1GB SDK indirilir)
./android-build.sh assembleDebug

# Release APK oluştur
./android-build.sh assembleRelease

# Test çalıştır
./android-build.sh test

# Tüm build task'ları
./android-build.sh build

# Container içine shell gir
./android-build.sh --shell
```

### APK Çıktı Yolları

```
app/build/outputs/apk/debug/app-debug.apk      # Debug
app/build/outputs/apk/release/app-release.apk  # Release
```

### Gradle Cache

Docker named volume'leri ile kalıcı cache:
- `android-agent-gradle` — Gradle dependencies (incremental build)
- `android-agent-android-home` — Debug keystore

İlk build ~10-15dk sürebilir. Sonraki build'ler cache sayesinde 20-30sn.

---

## Nasıl Yüklerim (Deploy)

### USB ile Cihaz/Tablet

```bash
# Cihazı USB ile bağla, USB Debugging aç
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### APK Transfer

```bash
# APK'yı cihaza kopyala (dosya yöneticisi ile yükle)
adb push app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/
```

---

## Ekran Akışı

```
┌─────────────────┐
│  MAIN ACTIVITY  │  ← Ana menü
│                 │     6 renkli işlem butonu
│  [Toplama] [+]  │     5/10/15/20/30 soru seçimi
│  [Çarpma] [÷]   │     🏆 Puan Tablosu butonu
│  [Yuvar.] [🎲]  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  GAME ACTIVITY  │  ← Oyun ekranı
│                 │     İlerleme çubuğu + soru numarisi
│    [Tip Badge]  │     Soru metni (altalta/yatay)
│                 │     ✅ 4 renkli buton + ✏️ yazma alanı
│  [102] [108]    │     Doğru: confetti + ding ses
│  [ 99] [115]    │     Yanlış: shake + buzzer ses
│  ✏️ [____] [OK] │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ RESULT ACTIVITY │  ← Tur sonuçları
│                 │     ⭐ yıldız, puan, %
│    ⭐⭐⭐ 800    │     [Ana Menü] [Puan Tablosu]
│   8/10 (80%)    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   LEADERBOARD   │  ← En yüksek 10 skor
│                 │     #1 altın, #2 gümüş, #3青铜
│  #1  1000 skor  │     Zamanda gösterimi
│  #2   900 skor  │
└─────────────────┘
```

---

## Veritabanı

### Schema

| Tablo | Sütun | Tip |
|-------|-------|-----|
| `scores` | `id` | Long, PK, auto-increment |
| | `score` | Int (puan) |
| | `correctCount` | Int (doğru sayısı) |
| | `totalQuestions` | Int (toplam soru) |
| | `accuracy` | Float (%) |
| | `operation` | String (işlem tipi) |
| | `timestamp` | Long (UTC ms) |

### DAO Metotları

```kotlin
@Insert
fun insert(entry: ScoreEntry)

@Query("SELECT * FROM scores ORDER BY score DESC LIMIT :limit")
suspend fun getTopScores(limit: Int): List<ScoreEntry>
```

---

## Ses Efektleri

| Dosya | R.raw ID | Açıklama |
|-------|----------|----------|
| `res/raw/sound_correct.wav` | `R.raw.sound_correct` | C5→E5→G5 chime |
| `res/raw/sound_wrong.wav` | `R.raw.sound_wrong` | Düşen buzzer |

Üretimi: `python3` ile WAV üretildi, 1 channel, 22050Hz, 16bit.

---

## Hızlı Değişiklik Rehberi

### Yeni Soru Tipi Ekle
1. `MathGame.kt` → `generateXxx()` fonksiyonu ekle
2. `MathGame.kt` → `OPERATION_XXX` sabiti ekle
3. `MathGame.kt` → `generateQuestions()` içinde when'e ekle
4. `MainActivity.kt` → `operationButtons` listesine ekle
5. `GameActivity.kt` → soru tip gösteriminde badge rengi ekle
6. `colors.xml` → yeni bir `op_xxx` rengi ekle
7. `activity_main.xml` → yeni buton ekle

### Soru Aralığı Değiştir
`MathGame.kt` → ilgili `generateXxx()` fonksiyonundaki `Random.nextInt(min, max)` değerlerini değiştir.

### Ses Değiştir
1. Python ile yeni WAV üret
2. `app/src/main/res/raw/` dizinine kaydet
3. `GameActivity.kt` → `initSounds()` içinde `R.raw.xxx` olarak load et

### Renk Değiştir
`colors.xml` → renk kodunu değiştir, drawable'ları veya buton renklerini güncelle.

### Soru Sayısı Seçenekleri Değiştir
`MainActivity.kt` → `ARRAY_QTY` array'ini ve `qty` list'ini güncelle.

### UI Değiştir
Layout dosyaları `app/src/main/res/layout/` içinde XML olarak, drawables `res/drawable/` içinde.

---

## Docker İmleci

| Detay | Değer |
|-------|-------|
| **İsim** | `android-agent-build` |
| **Base** | `eclipse-temurin:17-jdk-noble` (amd64) |
| **Gradle** | 8.14.3 |
| **Android SDK** | platform-36, build-tools 36.0.0 + 35.0.0 |
| **Volume** | `/project` mount noktası |

```bash
# İmleği sıfırla ve yeniden oluştur
docker build -t android-agent-build .
```

---

## Notlar

- Min SDK 26 → adaptive icon desteği + tablet uyumluluğu
- `aapt2FromMavenOverride` — Apple Silicon'da Rosetta ile aapt2 bypass
- File event warning'lar Docker bind mount ile ilgili, build'i etkilemez
