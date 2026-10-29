# Launcher ikonu

Kapsam: grafit zemin üzerinde adaçayı yeşili, widget çerçevesi biçiminde özgün bir "Q" işareti; adaptive icon (vektör ön plan + zemin katmanı, monochrome destekli), gerekli mipmap yoğunlukları ve manifest bağlantıları. Üçüncü taraf (Spotify vb.) logo benzerliği veya marka taklidi kapsam dışı.

## Tasarım kararı

- Zemin: grafit degrade (`#232B2E` → `#0F1416`) ve sol üstte `%10` opaklıkta adaçayı ışıması. Uygulamanın mevcut Grafit temasıyla aynı ailedendir.
- İşaret: `#B8F39B` (mevcut accent) yuvarlatılmış kare halka + sağ alt köşede kısa kuyruk. Halka widget yüzeyini, kuyruk "Q" harfini kurar; 48dp'de tek bakışta okunur.
- Sanat 108×108 viewport içinde 66dp güvenli alanda kalır (en uzak nokta merkezden ≈30,2 birim). Yuvarlak, kare ve squircle maskelerde kırpılmaz.
- Monochrome katman aynı geometrinin tek renkli hâlidir; Android 13+ temalı simgelerde sistem tarafından renklendirilir.
- Marka riski: hiçbir üçüncü taraf logosu, kelime markası, dalga/nota motifi veya kurumsal renk kombinasyonu kullanılmadı; işaret yalnızca uygulamanın kendi paletinden türetildi. Vektörler ve rasterler aynı sabitlerden üretilir; dış ikon fontu veya stok varlık yoktur.

## Dosyalar

- `app/src/main/res/drawable/ic_launcher_background.xml`, `ic_launcher_foreground.xml`, `ic_launcher_monochrome.xml`
- `app/src/main/res/mipmap-anydpi/ic_launcher.xml`, `ic_launcher_round.xml` (adaptive). minSdk 31 olduğundan `-v26` niteleyicisi gerekmez ve lint'in `ObsoleteSdkInt` uyarısı oluşmaz; minSdk 26'nın altına inilirse klasör `mipmap-anydpi-v26` olarak yeniden adlandırılmalıdır.
- `app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.png` ve `ic_launcher_round.png` (48/72/96/144/192 px; yuvarlatılmış köşeli, köşe pikselleri saydam). Çalışma anında adaptive XML seçildiği için bunlar API 26 öncesi ve araç uyumluluğu içindir.
- `app/src/main/AndroidManifest.xml`: `android:icon="@mipmap/ic_launcher"`, `android:roundIcon="@mipmap/ic_launcher_round"`
- `scripts/make-icon-assets.py`: geometrinin tek kaynağı; vektörleri, mipmap PNG'lerini ve maskeli önizlemeleri üretir (yalnızca Python standart kütüphanesi).
- `scripts/check-icon-resources.ps1`: derlenmiş APK'da manifest `icon`/`roundIcon` çözümlemesini, adaptive `<background>/<foreground>/<monochrome>` katmanlarını ve yoğunluk PNG'lerini denetler.

`drawable/ic_music.xml` önceki widget yer tutucusu olarak korunur; launcher ikonu artık bu dosyaya bağlı değildir.

## Doğrulama kanıtı

Aşağıdaki çıktılar yerel doğrulama kanıtıdır; `artifacts/` yayına dahil edilmez.

- `scripts/verify.ps1`: BUILD SUCCESSFUL, 74 birim testi / 0 hata, lint 0 hata / 30 uyarı (tamamı önceden var olan uyarılar; ikon değişikliği yeni uyarı eklemedi).
- `scripts/check-icon-resources.ps1`: geçti - `android:icon` → `mipmap/ic_launcher`, `android:roundIcon` → `mipmap/ic_launcher_round`; adaptive katmanlar `drawable/ic_launcher_background|foreground|monochrome` olarak çözülüyor; beş yoğunlukta PNG APK içinde.
- Önizlemeler: `artifacts/icon-preview-{48,96,192}-{round,square,squircle}.png` ve toplu sayfa `artifacts/icon-preview-grid.png`.
- Cihaz: Oppo A5 5G (Android 16), `adb install -r artifacts/qwidget-studio-debug.apk` başarılı; başlatıcıda ikon ColorOS squircle maskesiyle doğru göründü (`artifacts/launcher-home.png`, `artifacts/launcher-icon-crop.png`), uygulama açılışı korunmuş davranışla çalıştı (`artifacts/app-launched.png`).

## Sınırlar

- Önizleme rasterleri `scripts/make-icon-assets.py` içindeki geometriyle üretilir (4×4 süper örnekleme, bağımlılık yok); Android'in kendi çizimi cihaz ekran görüntüsüyle ayrıca doğrulandı.
- Farklı başlatıcıların (ör. yuvarlak maskeli Pixel) ve temalı ikon görünümü ayrı cihazlarda denenmedi; monochrome katmanın APK'da çözüldüğü denetlendi, görsel temalı ikon testi yapılmadı.