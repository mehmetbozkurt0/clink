# Firebase kurulumu

Clink, hesaplar (Google girişi, anonim misafirler) ve kart içeriği için Firebase kullanır. Bu adımları Firebase konsolunda kendi Google hesabınla bir kez yapman yeterli. Sıra önemli: Google girişini ve SHA-1 parmak izini, `google-services.json` dosyasını indirmeden **önce** ekle, yoksa dosyada Google girişi için gereken bilgi eksik kalır.

Konsol: https://console.firebase.google.com

## 1. Proje

1. **Proje ekle** → adı: `clink`.
2. Google Analytics'i şimdilik **kapalı** bırak (Plan 5'te açacağız).

## 2. Blaze planı ve bütçe uyarısı

1. Sol alttaki plan rozetinden **Blaze**'e (kullandıkça öde) geç. Ücretsiz kotalar Spark ile aynı; Plan 4'teki gerçek zamanlı lobiler ve Plan 5'teki temizlik işleri için gerekiyor.
2. **Bütçe uyarısı** (ödemeyi durdurmaz, sadece e-postayla haber verir):
   1. Firebase konsolunda sol üstteki **dişli** → **Kullanım ve faturalandırma** (Usage and billing) → **Ayrıntılar ve ayarlar** (Details & settings) sekmesi.
   2. **Bütçeler ve uyarılar** (Budgets & alerts) kartında **İlk bütçeyi oluştur** (Create first budget). Bu seni Google Cloud konsolundaki bütçe sayfasına götürür.
      - Bu bağlantı yoksa: https://console.cloud.google.com/billing → faturalandırma hesabını seç → soldan **Bütçeler ve uyarılar** → **Bütçe oluştur**.
   3. **Kapsam:** ad `clink-aylik`, zaman aralığı **Aylık**, projeler **clink**, hizmetler **Tüm hizmetler** → İleri.
   4. **Tutar:** "Belirtilen tutar", ör. **5 USD** (hesabın para birimi TL ise ~200 TL) → İleri.
   5. **İşlemler:** eşikler **%50, %90, %100** (gerçekleşen harcama); **"Faturalandırma yöneticilerine ve kullanıcılarına e-posta uyarısı gönder"** işaretli → **Bitir**.
   - Bütçe ekranı açılmıyor ya da "izin yok" diyorsa: faturalandırma hesabında **Billing Account Administrator** rolün olmalı. Faturalandırma hesabını sen açtıysan bu rol zaten sende.

## 3. Giriş yöntemleri

**Authentication → Başlayın → Sign-in method**:
1. **Google** → Etkinleştir → proje için destek e-postasını seç → Kaydet.
2. **Anonim** (Anonymous) → Etkinleştir → Kaydet.

## 4. Firestore

Sol menü → **Derleme** (Build) → **Firestore Database** → **Veritabanı oluştur** (Create database / Add database). Sihirbazda sırayla:
1. **Sürüm (edition): Standard.** Enterprise seçme; o daha pahalı ve bize gereken özellikleri farklı.
2. **Veritabanı kimliği (Database ID): boş bırak** ya da `(default)` kalsın. Uygulama varsayılan veritabanını kullanır; başka bir ad verirsen bağlanamaz.
3. **Konum: `eur3 (Europe)`** (çok bölgeli Avrupa). Listede yoksa `europe-west1 (Belgium)` seç. Konum sonradan **değiştirilemez**.
4. **Güvenlik kuralları: Üretim modu** (production mode / "Start in production mode"). Bu, uygulamadan gelen tüm okuma ve yazmaları şimdilik kapatır; kuralları Görev 2'de birlikte yazıp yapıştıracağız. "Test modu"nu seçme: herkese 30 gün boyunca okuma ve yazma izni verir.
5. **Oluştur** (Create). Birkaç saniye sürer; sonunda boş bir "Veri" (Data) sekmesi görürsün. Koleksiyon ekleme, onları içerik aracı oluşturacak.

## 5. Android uygulaması

**Proje ayarları (dişli) → Genel → Uygulamalarınız → Android simgesi**:
1. Android paket adı: `com.mehmetbozkurt.clink`
2. Uygulama takma adı: `Clink`
3. **Hata ayıklama imzalama sertifikası SHA-1** (bu bilgisayarın debug anahtarı):
   `46:B7:AC:1D:F7:3A:80:6A:0F:3C:20:EC:D9:E5:67:CF:F3:6C:1D:F5`
4. **Uygulamayı kaydet** → `google-services.json` dosyasını indir → repo içinde `androidApp/google-services.json` olarak kaydet.
5. Konsolun sonraki "SDK ekle" adımlarını atla; onları ben yapıyorum.

Bu dosya commit'lenebilir: içinde gizli bilgi yok, Firebase onu paket adı ve SHA-1 ile sınırlar.

## 6. Yönetici anahtarı (içerik aktarma aracı için)

**Proje ayarları → Hizmet hesapları → Firebase Admin SDK → Yeni özel anahtar oluştur**:
1. İnen JSON dosyasını repo içinde `secrets/firebase-admin.json` olarak kaydet.
2. **Bu dosya gizlidir:** `secrets/` klasörü git'e girmez. Kimseyle paylaşma, bana içeriğini gösterme; araç onu doğrudan diskten okur.

## Bitti mi?

Şunlar hazırsa haber ver, gerisini ben yaparım:
- [ ] `androidApp/google-services.json` var.
- [ ] `secrets/firebase-admin.json` var.
- [ ] Google ve Anonim girişi açık, Firestore oluşturuldu.

Not: Yayın (release) sürümünü imzalarken Plan 5'te o anahtarın SHA-1'ini de buraya ekleyeceğiz.
