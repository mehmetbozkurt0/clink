# Firebase kurulumu

Clink, hesaplar (Google girişi, anonim misafirler) ve kart içeriği için Firebase kullanır. Bu adımları Firebase konsolunda kendi Google hesabınla bir kez yapman yeterli. Sıra önemli: Google girişini ve SHA-1 parmak izini, `google-services.json` dosyasını indirmeden **önce** ekle, yoksa dosyada Google girişi için gereken bilgi eksik kalır.

Konsol: https://console.firebase.google.com

## 1. Proje

1. **Proje ekle** → adı: `clink`.
2. Google Analytics'i şimdilik **kapalı** bırak (Plan 5'te açacağız).

## 2. Blaze planı ve bütçe uyarısı

1. Sol alttaki plan rozetinden **Blaze**'e (kullandıkça öde) geç. Ücretsiz kotalar Spark ile aynı; Plan 4'teki gerçek zamanlı lobiler ve Plan 5'teki temizlik işleri için gerekiyor.
2. Google Cloud faturalandırma ekranında bir **bütçe uyarısı** ekle (ör. aylık 5 $, %50 / %90 / %100 e-posta uyarısı). Bu ödemeyi durdurmaz, sadece haber verir.

## 3. Giriş yöntemleri

**Authentication → Başlayın → Sign-in method**:
1. **Google** → Etkinleştir → proje için destek e-postasını seç → Kaydet.
2. **Anonim** (Anonymous) → Etkinleştir → Kaydet.

## 4. Firestore

**Firestore Database → Veritabanı oluştur**:
1. **Üretim modu** (production mode). Güvenlik kurallarını Görev 2'de birlikte yazıp yapıştıracağız.
2. Konum: **eur3 (Europe)**. Konum sonradan değiştirilemez.

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
