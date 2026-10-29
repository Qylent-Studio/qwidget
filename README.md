# QWidget — Android widgetları

Bu sürüm **4×3 / 4×4 Spotify widgetları** ve **2×1 pil widgetı** içerir. Bunlar başlangıç boyutlarıdır; iki tür de ana ekranda yeniden boyutlandırılabilir. Spotify kapak/şarkı/sanatçı, kontroller, widget başına renk ve şarkı atlama kuralları korunur.
Önceki teknik kontrol ekranı uygulama içinden erişilebilir.

## Pil widgetı

- QWidget → **Pil · 2×1 ekle** veya ana ekran → Widgetlar → QWidget. Büyük yüzde, şarj durumu ve doluluk çubuğu gösterilir.
- Renk seçmek için pil widgetına dokunun. İki widget türünde de Grafit, Orman, Okyanus, Mürdüm, Kum ve Kırmızı bulunur; seçim yalnızca o widgetı değiştirir. Spotify/bildirim/Bluetooth izni gerekmez.
- İki widget türünün yüzeyi hafif saydam ve ince aydınlık kenarlıdır; yazılar saydamlaştırılmaz. Duvar kâğıdı yüzeyin arkasından görünür, fakat gerçek arka plan bulanıklığı uygulanmaz.
- **Hızlı şarj oluyor:** Android 13+ üzerinde Oppo'nun sistemden gelen canlı `chargefastcharger` bilgisi, mevcut şarj durumu ve güç kaynağıyla eşleşirse gösterilir. Akım/watt değerinden tahmin edilmez. OEM bilgisi yoksa, eski/sticky ise veya süreç yeni açılmış ve henüz canlı olay gelmemişse **Şarj oluyor** korunur. Kablo çıkarılınca veya güç kaynağı değişince eski hızlı şarj bilgisi temizlenir; diskte tutulmaz.
- Uygulama süreci yaşarken Android pil olaylarıyla güncellenir. Süreç kapalıyken Android'in 30 dakikalık widget yenilemesi yedektir; Doze/Oppo pil tasarrufu bunu daha da geciktirebilir. Sürekli servis, sık sorgulama ve anlık arka plan güncellemesi garantisi yoktur.
- Bilgi eksik veya geçersizse uydurma yüzde yerine `—` gösterilir.
- APK: `artifacts/qwidget-studio-debug.apk` (0.4.2-studio). Önceki sürümde gerçek cihazda ekleme, yüzde/şarj okuması, dokunarak ayar açma ve renk kalıcılığı denendi. Güncel cam/kırmızı görünüm, boyut değiştirme ve gerçek hızlı adaptör üzerinde canlı hızlı şarj geçişi Oppo'da ayrıca doğrulanmalı; bilgisayar USB bağlantısı hızlı şarj bildirmiyor.

## Ana uygulama ve boyutlandırma

- Ana ekran **Widget ekle**, **Widgetlarım**, **Otomasyonlar** ve **İzin durumu** bölümlerine ayrılmıştır. Spotify eklerken başlangıç boyutunu seçin; widget kartına dokunarak o widgetın rengine gidin.
- Renk ayarlarında iki sütunlu tema örnekleri ve kaydırmadan erişilen **Rengi kaydet / Vazgeç** alt çubuğu vardır. Mevcut renkler ve otomasyon kuralları korunur.
- Ana ekranda widgeta uzun basıp kenarlarından sürükleyin. Spotify sağlayıcıları en az 160×120dp, pil sağlayıcısı en az 70×48dp yeniden boyutlandırma alanı ister; gerçek hücre sınırlarını başlatıcı belirler. Spotify küçük alanda küçük kapak, başlık ve üç kontrolü koruyan kompakt düzene geçer.
- Pil yazıları alana göre değişir. Çok küçük alanda şarj metni/doluluk çizgisi gizlenebilir; tam durum widgetın erişilebilirlik açıklamasında korunur. Daha büyük görünüm için widgetı genişletin.
- Emülatörde ana ekran açılışı ve dört bölümün erişilebilirliği kontrol edildi; ekran görüntüsü `artifacts/qwidget-dashboard.png`. Ek küçük ekran/yazı ölçeği ve widget testleri Android 17 emülatörünün System UI/başlatıcı sorunlarına takıldı; Oppo boyut değiştirme testi yerine geçmez.

## Widget kullanımı

1. QWidget'i açıp **Spotify ekle** düğmesinden **4×3** veya **4×4** seçin, ana ekran ekleme penceresini onaylayın. Alternatif: ana ekran → Widgetlar → QWidget.
2. Bildirim erişimini Android ayarlarında kendiniz verin ve Spotify'da bir şarkı başlatın.
3. Widget sağ üstündeki **⋮ → Renk ve görünüm** üzerinden altı temadan birini seçip **Rengi kaydet** düğmesine dokunun. Yalnızca o widget değişir.
4. Aynı ekranda bildirim erişimi ve Spotify'ı açma kısayolları bulunur. İzin/oturum yokken müzik kontrolleri etkin değildir; eski şarkı diskte tutulmaz.

Hücre boyutlarını başlatıcı belirler; sağlayıcılar 4×3 ve 4×4 önerir, yeniden boyutlandırmaya izin verir. Oppo'da iki boyut eklendi ve görüntülendi. Renk kalıcılığı ve farklı widgetların renk bağımsızlığı doğrulandı.

## Şarkı kuralları

- **⋮ → Şimdi atla** yalnızca mevcut şarkıya sonraki komutunu gönderir.
- **N kez atla:** 1–999. Şarkı çalıyorsa ilk atlama hemen denenir ve N'ye dahildir; duraklatılmış şarkı kendiliğinden başlatılmaz.
- **Hep atla:** kaldırılana kadar geçerlidir. Kurallar Spotify genelindedir; widget boyutu veya renginden bağımsızdır.
- Aynı menü veya uygulamadaki **Atlama kuralları ve Bluetooth** ekranından kalan sayıyı değiştirin, her zaman kuralına çevirin veya onaylayarak kaldırın. Ana anahtar tüm otomatik atlamayı kapatır, kuralları silmez.
- Yalnızca güvenilir `spotify:track:` kimliği kullanılır. Reklam, podcast, yerel dosya veya eksik kimlikte otomatik kural eklenmez; ad/sanatçıdan tahmin yapılmaz.
- Sayaç, komuttan sonra aynı oturumda 8 saniye içinde farklı güvenilir şarkı çalarken görülürse azalır. Komut reddi, geçiş belirsizliği veya süreç ölümü sayacı azaltmaz. Aynı kimlikli tekrarı kesin teyit etmek mümkün olmayabilir.
- Pause/play, yinelenen metadata ve süreç yeniden açılışı aynı gözlemlenen şarkıya tekrar komut göndermez. Kaçırılmış ara olaylarda bir atlama kaçırmak, yanlış şarkıyı/çift atlamayı göze almaya tercih edilmiştir.
- Döngü güvenliği: 30 saniyede en fazla 5 otomatik deneme. Süreç yeniden doğarsa son denemeden kalan 30 saniyelik pencere boyunca temkinli beklenir; kural kaydı korunur.

## Bluetooth

- Aynı menüden eşleştirilmiş aygıtı seçin ve devam ettirme anahtarını açın. Yalnızca seçili aygıtın **A2DP veya LE Audio ses profili** bağlantısı tetikler; ACL bağlantısı kullanılmaz.
- Olaylar geliş sırasıyla işlenir, aynı bağlantının birden fazla profil olayı tekrar başlatmaz. Eski kalıcı profil işaretleri yeniden yüklenmez; yeniden başlatma/izin aralığı sonrası kaçırılmış disconnect yüzünden süresiz engel oluşması önlenir.
- Mevcut Spotify oturumu gerekir; soğuk başlatma ve sürekli komut tekrar döngüsü yok. Durum ekranındaki “komut gönderildi” gerçek sesin başladığına dair garanti değildir.
- Yeni A2DP/LE yolu için fiziksel kulaklık yeniden bağlama testi bekleniyor. Önceki ACL prototipinin kullanıcı testi bu yeni akışın doğrulaması yerine geçmez.

## Launcher ikonu

- Adaptive icon grafit degrade zemin, adaçayı yeşili (`#B8F39B`) yuvarlatılmış kare widget çerçevesi ve sağ alt köşedeki kısa kuyruktan oluşur; işaret "Q" olarak okunur. Android 13+ temalı simgeler için ayrı monochrome katmanı vardır.
- Kaynaklar: `res/drawable/ic_launcher_background.xml`, `ic_launcher_foreground.xml`, `ic_launcher_monochrome.xml` ve `res/mipmap-anydpi/ic_launcher*.xml`; minSdk 31 olduğu için çalışma anında her yoğunlukta adaptive XML seçilir, `mipmap-mdpi…xxxhdpi/ic_launcher*.png` (48–192 px) API 26 öncesi ve araç uyumluluğu içindir. Manifest `android:icon` ve `android:roundIcon` bu kaynaklara bağlıdır.
- Geometri tek kaynaktan üretilir: `python scripts/make-icon-assets.py` (ek bağımlılık yok) vektörleri, mipmap PNG'leri ve maskeli 48/96/192 önizlemelerini yazar; önizlemeler `artifacts/icon-preview-*.png`.
- APK denetimi: `powershell -ExecutionPolicy Bypass -File scripts/check-icon-resources.ps1` manifest bağlantısını, adaptive/monochrome kaynaklarını ve yoğunluk PNG'lerini denetler.
- Güncel derlemenin başlatıcı çizimi Pixel Launcher'lı emülatörde (Android 17, 420 dpi) 48dp'de doğrulandı: `docs/images/launcher-icon-pixel-launcher-48dp.png`. Fiziksel cihaz (Oppo) teyidi güncel derleme için yapılmadı; ayrıntı ve sınırlar `docs/launcher-icon.md`.
- Üçüncü taraf logo, kelime markası veya dalga/nota gibi marka motifleri kullanılmaz; ikon yalnızca uygulamanın kendi grafit/adaçayı paletinden türetilir.

## Derleme

- Android SDK 36 / Android Studio ve Gradle 9.3.1 destekli JDK (17 veya üstü).
- PowerShell: `powershell -ExecutionPolicy Bypass -File scripts/verify.ps1`
- Testler + debug APK + Android lint çalışır. APK: `artifacts/qwidget-studio-debug.apk` (eski dosya adları uyumluluk için de kopyalanır). İkon denetimi: `scripts/check-icon-resources.ps1`; maskeli ikon önizlemeleri: `scripts/make-icon-assets.py`.
- Script Unicode Windows proje yollarına karşı derleme çıktılarını kullanıcıya ait geçici dizine yönlendirir. `QWIDGET_BUILD_ROOT` ile değiştirilebilir.
- Başka ortamda `JAVA_HOME`, `ANDROID_HOME` ayarlayıp `gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug` kullanılabilir.

## Oppo A5 5G / Android 16 teknik kontrol listesi

1. APK'yı telefona kopyalayıp açın veya USB hata ayıklamayı açıp bilgisayara yetki verin; `adb install -r artifacts/qwidget-automation-debug.apk`. **Bluetooth ve teknik kontrol** düğmesi önceki probe ekranını açar.
2. Uygulamadan **Bildirim erişimini aç** seçeneğine dokunun ve QWidget'e erişimi kendiniz verin. Android, dışarıdan yüklenen uygulamada önce uygulama bilgi ekranından **Kısıtlanmış ayarlara izin ver** isteyebilir. Yalnızca güvendiğiniz kendi derlemeniz için izin verin.
3. Spotify'ı açın, bir şarkı çalın ve probe'a dönün. Dinleyici bağlı, şarkı/sanatçı/kapak ve çalıyor durumu kontrol edilir. Kapak ve medya kimliği Spotify tarafından sağlanmayabilir.
4. Önceki, duraklat, oynat, sonraki düğmelerini deneyin. **Komut gönderildi** bir başarı onayı değildir: sesin ve durumun değiştiğini doğrulayın.
5. Değişim sayısını not edin. Ekranı kapatın, iki şarkı geçmesini bekleyin. Probe'a dönün; sayaç ve son değişim saati güncellenmiş olmalı. Sayaç yalnızca süreç ömründeki teşhistir, kalıcı şarkı kimliği değildir.
6. Android'de kulaklığı eşleştirin; probe'da Bluetooth izni verin, aygıt seçin ve devam ettirme anahtarını açın. Spotify'ı duraklatın, ekranı kapatın, bağlantıyı kesip yeniden bağlayın. Müziğin gerçekten başladığını doğrulayın. Farklı bir aygıt bağlantısı müziği başlatmamalı.
7. Yeni sürüm ses profili bağlantısını kullanır; yine de cihaz/Spotify komutu reddedebilir. Sürekli tekrar yapılmaz. A2DP ve varsa LE Audio gerçek cihaz sonuçlarını ayrı not edin.
8. Aynı denemeleri Oppo pil kısıtlaması açık ve uygulama için arka plan kullanımına izin verilmiş halde karşılaştırın. Telefonu yeniden başlatıp ve bildirim/Bluetooth izinlerini kaldırıp tekrar kontrol edin.

Telefon üzerinde bilgi/kontrol ve iki widget boyutu denendi. Kullanıcı ayrıca Bluetooth devam ettirme ve doğal ekran kapalı geçiş takibini doğruladı. Bunlar uzun Doze, yeniden başlatma, izin kaldırıp hemen süreç öldürme garantisi değildir.

## Sınırlar ve gizlilik

- Spotify'ın **mevcut** medya oturumu kontrol edilir. Spotify tamamen kapalıysa arka planda soğuk başlatma yok.
- Sürekli foreground service, ekran kapalı polling, Spotify API anahtarı veya erişilebilirlik servisi yok. Glance ve olay tabanlı WorkManager kullanılır; bildirim dinleyicisi ayrıldığında kalıcı güncelleme işi kuyruklanır.
- Bildirim erişimi geniş bir Android yetkisidir, ancak kod bildirim içeriklerini okumaz; yalnızca Spotify medya oturumunu seçer.
- Kapak ve anlık çalma verisi diske yazılmaz. Kullanıcının özellikle oluşturduğu kurallar için şarkı kimliği/adı/sanatçısı, kalan sayı ve sürüm yerel ayarlarda tutulur; dinleme geçmişi yoktur. Son deneme işareti ve güvenlik penceresi tekrar engeli içindir. Seçilen Bluetooth aygıtı ve açık/kapalı tercihi de yereldir. Yedekleme/aktarım kapalıdır.
- Yinelenen Bluetooth olayı engeli süreç içindedir; profil yayınları sticky değildir. OEM olay kaçırmaları, uzun uyku ve fiziksel bağlantı geçişleri için tam garanti yoktur.
- 74 birim testi: pil yüzdesi/durum/geçersiz veri, güvenilir hızlı şarj/geçiş/eski bilgi temizliği, saydam yüzey kontrastı, tema kalıcılığı ve boyuta/yazı ölçeğine uyum; paket/bağlantı filtresi, sunum/kontrast, kural serileştirme/değiştirme/teyitli sayaç, süreç işareti/bekleme penceresi, oturum eşleşmesi ve Bluetooth olay sırası. `scripts/check-widget-settings.ps1` açık Spotify renk ayar ekranını; `scripts/check-battery-settings.ps1` pil ayarını; `scripts/check-dashboard.ps1` ana uygulamanın dört bölümünü kontrol eder.
- Zorla durdurma, izin kaldırıp hemen süreç öldürme veya başlatıcı önbelleği nedeniyle ana ekrandaki son görüntünün anında temizlenmesi garanti edilmez. Kuyruklanan iş sistem tarafından çalıştırılana veya sonraki güncellemeye kadar eski görüntü kalabilir; komut yolunda erişim tekrar kontrol edilir.
- En büyük sistem yazı boyutu, diğer başlatıcılar, yatay ekran ve uzun süreli Doze test edilmedi. Android lint uyarıları ağırlıkla Türkçe ekran metinlerinin kaynaklaştırılması ve yeni sürüm önerileridir; sıfır hata sonucu, sıfır uyarı iddiası değildir.
