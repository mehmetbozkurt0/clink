# Çevrimiçi oyun kurulumu (Plan 4) — Mehmet'in adımları

Uygulama tarafı hazır. Çevrimiçi oyunun gerçekten çalışması için aşağıdaki adımları senin yapman gerekiyor
(konsol, alan adı ve yayınlama işleri). Sıra önemli: 1 → 4 bitmeden iki telefonla test anlamlı olmaz.

Bu belgede hiçbir anahtar veya şifre yok; adımlarda da kimseyle paylaşman gereken bir şey yok.

## 1. Realtime Database'i oluştur

1. [Firebase konsolu](https://console.firebase.google.com) → Clink'in projesi.
2. Sol menü: **Build → Realtime Database → Create database**.
3. Konum: **Belgium (europe-west1)**.
4. Güvenlik: **Start in locked mode** (kilitli). Asıl kuralları 4. adımda biz yükleyeceğiz.

## 2. Misafir (anonim) girişi aç

Hesabı olmayan arkadaşların kodla katılabilmesi için:

**Build → Authentication → Sign-in method → Add new provider → Anonymous → Enable → Save.**

## 3. `google-services.json`'ı yeniden indir

Veritabanı oluşunca bu dosyaya veritabanı adresi (`firebase_url`) eklenir; eski dosyada yok.

1. **Project settings** (dişli) → **Your apps** → Android uygulaması → **google-services.json** indir.
2. Eskisinin yerine `androidApp/google-services.json` olarak koy.

Bu dosya `.gitignore`'da; git'e girmez, öyle kalmalı.

## 4. Kuralları ve web sayfalarını yayınla (Firebase CLI)

Bir kerelik kurulum:

1. [Node.js LTS](https://nodejs.org) kur.
2. PowerShell'de:

```powershell
npm install -g firebase-tools
```

```powershell
firebase login
```

Sonra repo kökünde çalıştır. Bu bilgisayardaki `.firebaserc` projeyi zaten gösteriyor; git'e girmez, o yüzden yeni bir bilgisayarda önce `firebase use --add` ile projeyi seçmen gerekir:

```powershell
firebase deploy --only database,firestore:rules,hosting
```

Bu komut üç şeyi yükler:

- `firebase/database.rules.json`: lobilerin güvenlik kuralları (kim neyi okuyup yazabilir);
- `firebase/firestore.rules`: Plan 3'teki profil ve içerik kuralları (değişmedi, aynısı tekrar gider);
- `hosting/public`: `clink-game.com` sayfaları ve Android'in uygulama bağlantısı dosyası (`.well-known/assetlinks.json`).

Kuralları değiştirdiğimde aynı komutu tekrar çalıştırman yeterli.

## 5. `clink-game.com` alan adını bağla

1. Alan adını bir kayıt firmasından al (henüz almadıysan).
2. Firebase konsolu → **Build → Hosting → Add custom domain** → `clink-game.com`.
3. Firebase'in verdiği **TXT** ve **A** kayıtlarını alan adı firmanın DNS ayarlarına gir.
4. Doğrulama ve sertifika birkaç saat sürebilir. Bittiğinde şu adres açılmalı:
   `https://clink-game.com/.well-known/assetlinks.json`

Başka bir alan adı seçersen bana söyle; üç yerde değişiyor (davet bağlantısı, Android manifesti, web sayfası).

### Uygulama bağlantısı (davet linki uygulamayı açsın)

Alan adı yayına girdikten sonra uygulamayı telefona **yeniden yükle**. Android, linkin uygulamaya ait olduğunu kurulumda
doğrular. Kontrol için:

```powershell
adb shell pm get-app-links com.mehmetbozkurt.clink
```

`clink-game.com: verified` görmelisin. Şu an `assetlinks.json` yalnızca proje debug anahtarının parmak izini içeriyor;
Play Store için yayın anahtarı oluşturduğunda onun SHA-256'sını da eklememiz gerekecek.

## 6. İki telefonla test

Bir telefon kurucu (Google hesabıyla girişli), diğeri misafir (hesapsız da olur).

- [ ] Kurucu: Oyna → oyun → **Online** → seviye → lobi açılıyor, 6 haneli kod görünüyor.
- [ ] **Davet et** → paylaşım menüsü açılıyor; mesajda kod ve link var.
- [ ] Misafir: linke dokununca uygulama **Kodla katıl** ekranında açılıyor (5. adımdan sonra); ya da Karşılama →
      **Kodla katıl** → kodu yaz.
- [ ] Yanlış kod → "Bu kodla açık bir lobi yok". Spicy lobide 18+ sorusu çıkıyor.
- [ ] İsim + avatar → Katıl → iki telefonda da oyuncu listesi, yeşil çevrimiçi noktası.
- [ ] Sohbet: iki taraftan mesaj, okunmamış pembe nokta.
- [ ] Doğruluk mu Cesaret mi: yalnızca sırası gelen düğmeleri görüyor, diğeri "… bekleniyor".
      İki telefon farklı dildeyse kart her telefonda kendi dilinde.
- [ ] Şişe Çevirmece: çeviren fırlatıyor, şişe iki telefonda aynı kişide duruyor; soran yazıyor, soru iki ekranda
      büyük kart; cevaplayan yazıyor ya da ceza alıyor.
- [ ] Bira Pong: yalnızca atan fırlatabiliyor, atış iki telefonda da oynuyor; maç bitince kurucu alttaki düğmeyle herkesi lobiye
      dönüyor.
- [ ] Misafir uçak moduna geçerse: sırası atlanıyor (Pong'da maç onu bekliyor); geri gelince devam ediyor.
- [ ] Kurucu uçak moduna geçerse: misafirde "Kurucu yeniden bağlanıyor · 60" geri sayımı; 60 saniye dolunca lobi
      kapanıyor. Erken dönerse oyun kaldığı yerden sürüyor.
- [ ] Kurucu bir oyuncuyu çıkarınca o kişi "Lobiden çıkarıldın" görüyor ve aynı kodla tekrar giremiyor.
- [ ] Kurucu lobiyi kapatınca misafir "Kurucu lobiyi kapattı" görüyor.

Bir şey ters giderse ekran görüntüsü ve hangi adım olduğunu yazman yeterli.

## Bilmen gerekenler

- **Ücretsiz plan (Spark) yeter:** test ve küçük gruplar için Realtime Database'in ücretsiz sınırı (aynı anda 100 bağlantı,
  1 GB veri) bol.
- **Eski lobiler silinmiyor:** kapanan lobiler veritabanında "closed" olarak kalıyor (küçük kayıtlar). İleride otomatik
  temizlik ekleriz.
- **Kart metinleri paylaşılmıyor:** lobide yalnızca kart kimlikleri dolaşıyor; her telefon metni kendi içeriğinden, kendi
  dilinde gösteriyor. Misafirin içeriği eskiyse kart yerine "Uygulamayı güncelle" yazıyor.
