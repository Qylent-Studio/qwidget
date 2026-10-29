# Aşama 3 — Spotify kuralları ve Bluetooth

Onaylanan kapsam: widget sağ menüsünden o anki şarkıya şimdi/N kez/her zaman atlama; kural listesi/düzenle/kaldır; seçili Bluetooth aygıtında kaldığı yerden devam ettirme. Pil widgetı bu aşamada yok.

- [x] Güvenilir Spotify track kimliği, kalıcı kurallar ve teyitli sayaç: RED → GREEN.
- [x] Dinleyici entegrasyonu, tekrar/başarısız komut/oturum değişimi koruması.
- [x] Widget menüsü ve kural yönetimi.
- [x] Bluetooth ses profili bağlantısı, sıralı olay işleme, tekrar engeli ve görünür durum (kod/test).
- [x] Test/lint/APK, bağımsız inceleme ve Oppo şarkı kuralı denemeleri.
- [x] Yeni ses profili yolunda seçili kulaklığı yeniden bağlayınca Spotify'ın devam etmesi kullanıcı tarafından doğrulandı. LE Audio ayrıca doğrulanmadı.

Ruling: N = 1…999. Kullanıcıya açıkça belirtilecek: şarkı şu anda çalıyorsa ilk atlama hemen denenir ve N buna dahildir. Duraklatılmış şarkı kendiliğinden başlatılmaz.
Ruling: Yalnızca doğrulanabilir `spotify:track:<22 karakter>` kimliği; ad/sanatçı eşleşmesi yok. Reklam, eksik kimlik, podcast ve yerel dosyada otomatik kural yok.
Ruling: Komut sonrası aynı oturumda 8 saniye içinde farklı güvenilir şarkı görülürse sayaç azalır. Teyit belirsizse sayacı azaltmamak tercih edilir. Tekrarlanan metadata ve pause/play yeniden komut üretmez.
Ruling: Son deneme işareti yerelde saklanır; süreç yeniden doğduğunda aynı gözlemlenmiş şarkıya tekrar komut gönderilmez. Bilinmeyen aralıkta şarkı gidip geri geldiyse bir atlama kaçabilir; bu, çift atlamaya tercih edilir. Dinleme geçmişi tutulmaz; yalnızca kullanıcının kuralları ve tek tekrar-engel işareti saklanır.
Ruling: En fazla 5 otomatik atlama / 30 saniye; döngüde müziği durmaksızın atlama engellenir. Başarısız komut aynı olay için otomatik tekrar denenmez.
Ruling: Bluetooth için ACL yerine klasik A2DP ve LE Audio ses profili bağlantısı izlenir; tek seçili aygıt ve mevcut Spotify oturumu. Soğuk başlatma veya tekrar deneme döngüsü yok.
Ruling: Git deposu yok; worktree, commit ve dış servis oluşturulmaz. Mevcut yerel ekran/Glance tasarımı korunur; yeni görsel sistem kurulmaz.

## Doğrulama
- Saf motor/depolama testleri: ilk koşu 28 test / 14 beklenen başarısız → 28/28. Bluetooth kapısı: 33 test / 4 beklenen başarısız → 33/33.
- İnceleme düzeltmeleri: oturum/bütçe testleri ve askıya alınmış disconnect→connect sırası için 36 test / 3 beklenen başarısız → 36/36.
- Son `scripts/verify.ps1`: BUILD SUCCESSFUL; 36 test / 0 başarısız; lint 0 hata / 30 uyarı. APK `artifacts/qwidget-automation-debug.apk`.
- Oppo: güvenilir şarkı kimliği bulundu. N=2 kaydı ilk otomatik geçişte 1'e düştü; şarkı geri getirildiğinde tekrar atlandı ve kural kalktı. Geçiş durumunda “Atlama doğrulandı” görüldü.
- “Hep atla” kaydı oluşturuldu; uygulama zorla durdurulup açılınca kayıt korundu. Düzenleme ile sayılı kurala çevrildi ve onaylı kaldırma denendi. Yalnızca bu testte oluşturulan kurallar kaldırıldı.
- Duraklatılmış şarkıya N=1 eklenince şarkı değişmedi/başlamadı. Oynatma sürdürülünce otomatik geçiş ve kuralın tükenmesi gözlendi.
- Geçersiz 0 girdisinde sayı diyaloğu kapanmadı; geçerli sayıya geçince kayıt yapıldı.
- İnceleme sırasında telefon yeniden başlatılmadı; fiziksel Bluetooth yeniden bağlama, LE Audio aygıtı, uzun Doze ve izin aralığı testleri bekliyor. Önceki ACL prototipinin testi yeni akışı doğrulamış sayılmadı.

## İnceleme bulguları / kararlar
- Otomatik komut artık gözlemlenen şarkı **ve oturum** ile eşleşen, hâlâ çalıyor durumdaki kontrolcüye gönderilir. Oturum/şarkı/durum eşleşmesi testi RED → GREEN; yanlış oturuma komut yerine iptal.
- Süreç yeniden oluştuğunda bütçe sıfırlanmasın diye son denemeden kalan 30 saniyelik pencere, aynı açılış kimliği içinde korunur. Bekleme testi RED → GREEN. Ruling: yeniden açılışta muhafazakâr bekleme, kısa süreli bir atlamayı geciktirebilir; döngüye tercih edilir.
- Bluetooth yayınları bağımsız IO işleri yerine tek Channel tüketicisine geliş sırasıyla girer. İlk iş askıdayken ikinci olayın erken yürütülmediği gerçek kapı testi RED → GREEN.
- Ruling: Bluetooth bağlantı profilleri diskte geri yüklenmez. Bu sistem yayınları sticky değildir; süreç/açılış/izin aralığında kaçan disconnect nedeniyle eski profilin bağlantıyı süresiz bastırması önlenir. Profil kapısı süreç içinde tutulur; OEM olay kaçırmasına tam garanti yoktur. Bu entegrasyonun yeniden başlatma testi yapılmadı.
- Kullanıcı kuralları/tek son-deneme işareti dışında dinleme geçmişi, kimlik belirsizliğinde ad/sanatçı eşleştirmesi ve sürekli retry eklenmedi.

Ertelenen küçükler: Türkçe metin kaynaklaştırma ve yeni bağımlılık sürümü lint önerileri. Büyük yazı/diğer başlatıcı testleri sonraki kalite kontrollerinde. Pil aşamasına geçilmedi.

Son kullanıcı kontrolü: yeni kurulan sürümde Spotify duraklatılıp seçili kulaklık yeniden bağlandığında otomatik devam ettiği bildirildi. Temel aşama 3 kapsamı tamamlandı; uzun Doze/LE Audio/telefon yeniden başlatma sınırlamaları sürüyor.
