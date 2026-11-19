# Content format

Kartlar tek bir JSON dosyasında durur:
`shared/src/commonMain/composeResources/files/content/baseline.json`

Şu an içinde test için 27 örnek kart var: 3 tip (Truth, Dare, Penalty) × 3 seviye (Casual, Hard, Spicy), her birinde 3 kart. Gerçek kartlar hazır olunca bunların yerine geçecek.

## Format

```json
{
  "version": 1,
  "packs": [
    {
      "id": "truth-casual-base", "type": "TRUTH", "level": "CASUAL", "access": "FREE",
      "cards": [
        { "id": "truth-casual-001", "text": { "tr": "Bu odadaki kimin telefonuna bakmak isterdin?", "en": "Whose phone in this room would you most like to look through?" } }
      ]
    }
  ]
}
```

- `version`: Şimdilik her zaman `1`. Uygulama başka bir sürüm görürse dosyayı reddeder.
- **Paket** (`packs` içindeki her öğe): Tek bir kart tipi ve tek bir seviyedeki kartları bir arada tutar.
  - `type`: `TRUTH`, `DARE` veya `PENALTY`.
  - `level`: `CASUAL`, `HARD` veya `SPICY`.
  - `access`: v1'de her zaman `FREE`. İleride ücretli paketler buradan ayrılacak.
  - Aynı tip ve seviyede birden fazla paket olabilir. Uygulama bunların kartlarını birleştirir.
- **Kart**: `id` ve `text` alanlarından oluşur. `text` içinde dil kodu ile o dildeki metin eşleşir (`tr`, `en`).

## Kurallar

1. **Kart ID'leri hiç değişmez ve tekrar kullanılmaz.** Online oyunda telefonlar birbirine kartın metnini değil ID'sini gönderir. Bir kartı silersen, yeni bir karta onun ID'sini verme. Önerilen biçim: `<tip>-<seviye>-NNN`, örneğin `dare-hard-014`.
2. **Her kartın hem Türkçesi hem İngilizcesi olsun.** Telefon hangi dildeyse kart o dilde gösterilir. O dilde metin yoksa İngilizcesi, o da yoksa hangi dil varsa o gösterilir. Yine de testler her kartta iki dilin de olmasını şart koşar.
3. **İçki yok.** Kartlarda içmeye dair hiçbir kelime ya da görev olmaz. Kaybeden ya da kartı reddeden kişi içmez, **ceza** (Penalty) çeker. Testler `içki`, `alkol`, `sarhoş`, `bira`, `drink`, `shot`, `beer` gibi kelimeleri yakalar ve bunlardan biri geçerse başarısız olur.
4. **Seviyelerin tonu:**
   - **Casual:** Herkesle oynanabilir, eğlencelidir ama kimseyi utandırmaz.
   - **Hard:** Daha kişisel sorular ve daha zorlu cezalar içerir.
   - **Spicy:** Flörtöz ve cesurdur, yetişkinler içindir. Ama müstehcen değildir, sadece ima eder.
5. **Her tip ve seviye için en az 3 kart olsun.** Testler bunu kontrol eder.

## Kontrol

Dosyayı değiştirdikten sonra testleri çalıştır:

```
./gradlew :shared:testAndroidHostTest --tests "com.mehmetbozkurt.clink.content.*"
```

Testler şunları kontrol eder: format bozuk mu, aynı ID iki kez kullanılmış mı, her kartta TR ve EN metni var mı, her tip ve seviyede en az 3 kart var mı, içkiyle ilgili bir kelime geçiyor mu.
