# Oyun Performans Asistanı

Android sistem kaynaklarını (RAM, pil, termal durum, ekran yenileme hızı,
arka plan uygulamaları) yöneterek oyun deneyimini iyileştirmeyi hedefleyen
bir yardımcı uygulama.

## Ne yapar
- **Oyun Modu**: İzin verirsen bildirimleri sessize alır (Rahatsız Etme) ve
  seçtiğin uygulamaların arka plan önbelleğini temizlemeyi dener.
- **Cihaz Durumu**: Boş RAM, pil seviyesi/sıcaklığı, termal durum ve CPU
  çekirdek/frekans bilgisini gösterir.
- **Ekran Yenileme Hızı**: Cihazın desteklediği modları listeler ve bu
  uygulamanın kendi ekranı için en yüksek hızı dener.
- **Hedef Uygulamalar**: Oyun modu açıldığında önbelleği temizlenmeye
  çalışılacak uygulamaları seçebilirsin.

Tüm bunlar Android'in resmi, genel (public) API'leri üzerinden yapılır.
Root gerekmez.

## Ne yapmaz (önemli sınırlar)
- **Hiçbir oyunun dosyasına, config'ine, belleğine veya sürecine dokunmaz.**
  PUBG Mobile dahil hiçbir oyunu hackleyip değiştirmez.
- **Başka bir uygulamanın ekran yenileme hızını zorlayamaz.** Android, bir
  uygulamanın başka bir uygulamanın render davranışını kontrol etmesine
  izin vermez. "Bu uygulamada dene" butonu sadece kendi ekranımız için
  çalışır; PUBG'yi 120Hz'e zorlamak için cihazın Ayarlar'ındaki (bazı
  cihazlarda Geliştirici Seçenekleri'ndeki) sistem çapında "yüksek
  yenileme hızını zorla" seçeneğini senin açman gerekir — uygulamadaki
  kısayol butonu seni oraya götürür.
- **Arka plan uygulamalarını "force stop" edemez.** Android 8'den beri
  hiçbir normal uygulama başka bir uygulamayı tam olarak durduramaz.
  `killBackgroundProcesses` çağrısı sadece sistemin zaten önbelleğe
  almaya uygun gördüğü süreçler için bir "ipucu"dur; etkisi cihazdan
  cihaza değişir ve garanti değildir.
- **Bir oyunun yazılımsal olarak sabitlenmiş FPS sınırını kaldıramaz.**
  Eğer PUBG belirli bir cihaz/chipset için 30 FPS sınırını kendi kodunda
  sabitlemişse (donanım yetersizliğinden değil), bu uygulama o sınırı
  aşamaz — sadece cihazın kaynaklarını (RAM, termal, arka plan yükü)
  iyileştirerek oyunun **potansiyelini** serbest bırakmaya çalışır.

Bu uygulama, telefon üreticilerinin zaten sunduğu "Game Booster / Game
Turbo" tarzı araçlarla aynı kategoridedir.

## Bilgisayar olmadan APK derleme (GitHub Actions — ücretsiz)

Bu projede `.github/workflows/build.yml` hazır geliyor. Android Studio
kurmadan, tamamen GitHub'ın sunucularında APK üretebilirsin:

1. **GitHub'da ücretsiz hesap aç** (github.com) ve yeni, **public** bir
   repo oluştur (private repo da olur ama ücretsiz dakika limiti public'te
   daha rahat).
2. Bu klasördeki tüm dosyaları o repo'ya yükle. Tarayıcıdan yapmak
   istersen: repo sayfasında **"Add file > Upload files"** ile bu zip'in
   içindekileri (klasör yapısını koruyarak) sürükle-bırak yap, sonra
   **Commit changes**.
   (Bilgisayardan yapıyorsan: `git init`, `git add .`,
   `git commit -m "ilk yükleme"`, `git remote add origin <repo-url>`,
   `git push -u origin main`.)
3. Yükleme bitince repo'da üstteki **"Actions"** sekmesine git.
   "APK Derle" adında bir iş otomatik başlamış olacak (birkaç dakika
   sürer). Bitmesini bekle (yeşil tik).
4. Tamamlanan işin üzerine tıkla, en altta **"Artifacts"** kısmında
   `oyun-performans-asistani-debug-apk` göreceksin — indir, içinden
   `.apk` dosyasını çıkar.
5. `.apk` dosyasını telefonuna aktar (WhatsApp'a kendine gönder, Google
   Drive, veya USB kablo), telefonda dosyaya dokunup kur. "Bilinmeyen
   kaynaklardan yükleme" izni isteyebilir, tek seferlik onaylaman yeterli.

Bunun için hiçbir kurulum, bilgisayar veya ödeme gerekmez — sadece bir
GitHub hesabı ve internet.

## Kurulum (Android Studio ile, bilgisayarın varsa)
1. Android Studio'da **File > Open** ile bu klasörü aç.
2. Gradle Sync otomatik başlayacaktır. Gradle wrapper dosyaları projede
   yer almıyor (binary dosya olduğu için); Android Studio ilk açılışta
   bunu kendisi tamamlar veya senden onay ister. Sürüm uyumsuzluğu
   uyarısı çıkarsa önerilen güncellemeyi kabul edebilirsin.
3. `minSdk = 26` (Android 8.0+), `targetSdk = 34`.
4. Bir cihaza veya emülatöre **Run** ile yükle.

## İzinler
- **Bildirim erişimi (DND)**: Uygulama içinden "Bildirim İzni Ver"
  butonuyla sistem ayarına yönlendirilirsin, manuel onay gerekir.
- **Bildirim gönderme (Android 13+)**: Oyun Modu'nu ilk açtığında sorulur.
- **Uygulama görünürlüğü**: Yüklü uygulamaları listelemek için `<queries>`
  bildirimi kullanılır (Android 11+ standart yöntemi).

## Yapı
```
app/src/main/java/com/gameperf/assistant/
├── MainActivity.kt          Ana ekran / dashboard
├── GameModeService.kt       DND + canlı bildirim + önbellek temizleme denemesi
├── PerformanceMonitor.kt    RAM / pil / termal / CPU okuma
├── RefreshRateHelper.kt     Ekran modu bilgisi + kendi pencereye uygulama
├── AppListActivity.kt       Hedef uygulama seçim ekranı
├── InstalledAppAdapter.kt   RecyclerView adaptörü
└── PrefsHelper.kt           SharedPreferences sarmalayıcı
```
