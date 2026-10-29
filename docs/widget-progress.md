# Aşama 2 — Spotify widgetı ve renkler

Onay: konuşmadaki 4×3/4×4 Spotify widgetı, kapak → şarkı/sanatçı → kontroller, sağ ayar menüsü, renk seçimi. Bu aşama pil widgetı ve kalıcı atlama kurallarını içermez.

- [x] Saf sunum modeli ve kontrastlı tema paletleri: RED → GREEN (ilk test koşusu 11 test / 7 beklenen başarısız; son koşu 11/11).
- [x] Glance widgetları, bağımsız widget ayarları, olayla güncelleme, güvenli kontroller.
- [x] Yerel ayar ekranı ve ekleme akışı; mevcut teşhis ekranını koruma.
- [x] Test/lint/APK, bağımsız inceleme ve Oppo üzerinde temel kontrol.

Dosya sınırları: `core/theme` ortak renkler; `spotify/widget` sunum, Android oturum adaptörü ve Glance; `settings` widget başına ayarlar. Mevcut Spotify/Bluetooth probe davranışı korunur.

Doğrulama: birim testleri izin/boş oturum, oynat-duraklat ve desteklenmeyen komutlar, bozuk renk kaydı ve palet kontrastını kapsar. Cihaz testi ekleme, iki boyut, tema kalıcılığı, değişim güncellemesi ve kontrolleri kapsar.

Ruling: Git deposu yok; worktree/commit yok. UI/UX aramasındaki web vitrin/CTA/font önerileri Android widgetına uygun değil; yalnızca koyu yüzey, yüksek kontrast, yerel yazı tipi ve 48dp dokunma hedefleri uygulanır.
Ruling: Harici Figma projesi oluşturma yetkisi ve hedef dosya yok; dış proje oluşturulmaz. Tasarım cihazdaki gerçek Android widgetı üzerinde doğrulanır.
Ruling: Widget veri kaynağı her render'da canlı Spotify oturumunu okur; probe belleği kalıcı veri kaynağı yapılmaz. Kapak/şarkı bilgisi diske yazılmaz. Render/callback işleri uygulama ana döngüsünden ayrılır.

## Sonuçlar
- `scripts/verify.ps1`: BUILD SUCCESSFUL, 11 test / 0 hata, lint 0 hata / 25 uyarı, debug APK üretildi ve telefona kuruldu.
- İlk lint hatası: dahili Glance AppWidgetId API kullanımı. Kamuya açık GlanceAppWidgetManager API'lerine geçildi; yeniden kontrol geçti.
- Oppo: iki sağlayıcı gerçek ana ekrana eklendi, 4×3/4×4 görünümü kırpılmış ekran görüntüsüyle incelendi; widget üzerinden pause/play/next ve metadata/kapak güncellemesi gözlendi.
- 4×3 Okyanus teması kaydedilip tekrar açıldı; 4×4 Grafit kaldı. Ayar kaydı ve örnekler arası izolasyon doğrulandı.
- Gerçek cihazda ekleme sonrası dashboard listesinin eski kaldığı gözlendi; onResume/onNewIntent yeniden oluşturma ile ikinci widget eklenince listenin yenilendiği kontrol edildi.

## Bağımsız inceleme ve düzeltmeler
- Önemli bulgu: izin eksik durumundan açılan widget ayarlarında izin kurtarma düğmesi yoktu. Cihaz scripti önce FAIL gördü, düğme eklendikten sonra PASS. Aynı şekilde Spotify kurtarma düğmesi için FAIL → PASS yapıldı.
- Küçük bulgu: ayar önizlemesi ana thread'de medya okuyor/kapak ölçekliyordu. Coroutine IO işine taşındı; Glance snapshot yüklemesi de IO üzerinde.
- Yaşam döngüsü riski: ayrılma/izin kaldırma temizliği yalnızca süreç içi gecikmeli işti. Ayrılma sırasında WorkManager işi kuyruklanır. Ruling: zorla durdurma veya izin kaldırıp hemen süreç öldürme sonrasında host görüntüsünün anında silinmesi garanti edilmez; bu kombinasyon cihazda test edilmedi, README'de açıkça sınırlandı. Maliyet: bir sonraki sistem güncellemesine kadar son görüntü kalabilir.
- Ertelenen küçükler: Türkçe metinlerin tümünü string kaynaklarına taşıma ve daha yeni sürüm lint önerileri. Büyük font/diğer başlatıcı/uzun Doze kontrolleri henüz yok.

Ruling: Glance/Compose yalnızca widget katmanında; basit ayar ekranı yerel Android kontrolleri kullanır. Büyük uygulama UI bağımlılıkları eklenmez. WorkManager bağımlılığı olay tabanlı temizleme içindir; periyodik yoklama yok.

Tam ana ekran görüntüleri paylaşılmadı/saklanmadı; çıktı görselleri yalnızca QWidget alanına kırpıldı. Sonraki aşamaya geçilmedi.
