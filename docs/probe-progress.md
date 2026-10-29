# Aşama 1 yürütme kaydı

Referans: konuşmada onaylanan tasarım ve beş aşamalı plan; kapsam yalnızca ilk aşama.

- [x] Spotify/Bluetooth güvenlik sınırları için RED → GREEN birim testleri (3 başarısız → 3 başarılı).
- [x] Minimal yerel test ekranı, Spotify oturum dinleyicisi ve Bluetooth alıcısı.
- [x] Testler, lint, debug APK ve bağımsız kod incelemesi.
- [ ] Oppo gerçek cihaz kontrolü (cihaz bağlantısı bekleniyor).
- Cihaz bağlantısı sağlandı: Android 16 / API 36; `adb install -r` → Success, test Activity ön planda ve uygulama süreci çalışıyor. Spotify paketi mevcut. İzinler ADB ile zorla verilmedi; kullanıcı etkileşimi bekleniyor.
- Kullanıcı bildirim erişimini verdi ve şarkı/kapak görüntüleme ile kontrolleri doğruladı. ADB UI üzerinden duraklat → çalmıyor, oynat → çalıyor, sonraki → şarkı değişimi ve sayaç artışı doğrulandı. Dinleyici bağlı ve medya kimliği mevcut.
- Ekran uyutuldu; sonraki medya tuşu sırasında cihaz yeniden uyandı. Değişim gözlendi fakat ekran kapalı sürekli takip/Doze kanıtı sayılmadı. Bluetooth seçimi henüz yok. Şarkı adları ve Bluetooth kimlikleri kalıcı kayda alınmadı.
- Kullanıcı Bluetooth aygıtını seçip anahtarı açtıktan sonra bağlantı kesme/yeniden bağlama ile Spotify'ın devam ettiğini bildirdi. Bu sonuç kullanıcı tarafından doğrulanmıştır; ADB ile ayrıca gözlemlenmedi. Doğal ekran kapalı geçiş, uzun Doze ve izin kaldırma testleri bekliyor.
- Kullanıcı doğal ekran kapalı şarkı geçişleri sonrasında değişim sayacının arttığını ve güncel şarkının doğru göründüğünü doğruladı. Temel fizibilite kontrolü geçti; uzun Doze/yeniden başlatma/izin kaldırma hâlâ test edilmedi. Sonraki ürün aşamasına geçiş için kullanıcı onayı bekleniyor.

Ruling: Klasör Git deposu değil; worktree/commit oluşturulmayacak. Çalışma doğrudan kullanıcının boş projesinde yapılacak.
Ruling: Görsel ürün tasarımı değil, geçici teknik kontrol ekranı hazırlanacak; Compose/Glance ve asıl widgetlar sonraki aşamalarda. Minimum SDK 31 bu Android 16 odaklı probe içindir.
Ruling: API anahtarı, hesap veya erişilebilirlik servisi kullanılmayacak. Bildirim erişiminden sadece Spotify medya oturumuna erişilecek, bildirim metinleri okunmayacak.

Ruling: Windows Unicode klasöründe test sınıfı bulunamadığı gözlendi; yalnızca derleme çıktıları `QWIDGET_BUILD_ROOT` ile ASCII geçici yola yönlendirildi. Proje taşınmadı. Farklı ortamda bu değişken olmadan varsayılan build yolu geçerli.

Doğrulama: `scripts/verify.ps1` → Gradle BUILD SUCCESSFUL; 3 test, 0 başarısız; lint 0 hata / 22 uyarı; debug APK artifacts klasörüne kopyalandı. İlk lint çalışmasında API 33 Parcelable çağrısı SDK 31 desteğiyle uyumsuz bulundu; sürüm koşulu eklenip yeniden kontrol edildi.

Bağımsız inceleme: Kritik/önemli bulgu yok. Ertelenen küçük bulgular:
- Dinleyicide erişim hatası bazı akışlarda boş oturum gibi görünebilir (komut yolunda ayrı izin hatası var).
- Teşhis sayacı metadata kaybından sonra aynı şarkıyı yeniden değişim olarak sayabilir; kalıcı atlama kuralı veya kesin oynatma sayısı olarak kullanılmamalı.

Diğer ertelenen küçükler: geçici ekranın çeviri kaynaklarına taşınması, özel ikon, daha yeni Gradle/hedef Android değerlendirmesi. Target SDK 36, kullanıcının Android 16 cihazı için bilinçli seçildi. Asıl ürün görsel tasarımı yapılmadı.

Sınırlı doğrulama: Android servisleri/arayüz cihazda çalıştırılmadı; yalnızca saf politika testleri ve derleme/statik analiz geçti. Süreç ölümü, ACL-ses profili gecikmesi, mevcut oturum olmadan başlatma üretim garantisi kapsamı dışında.
