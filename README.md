# VÚB Notifier v1.1 — Queue + opravený package name

## Čo je nové oproti predošlej verzii

### 1. Opravený package name (kritická oprava)
Appka predtým filtrovala notifikácie podľa `sk.vub.mobilebanking` — nesprávny
package name. Skutočný balík VÚB Banking appky je **`sk.vub.banking`**.
Preto sa žiadna notifikácia nikdy neodoslala.

### 2. Offline queue (store-and-forward)
Ak v momente príchodu notifikácie nie je k dispozícii sieť (alebo server
neodpovedá), notifikácia sa **neztratí**:
1. Pri príchode sa hneď uloží do lokálnej queue (SharedPreferences, JSON)
2. Appka sa ju ihneď pokúsi odoslať
3. Ak zlyhá → ostáva v queue
4. Pri obnovení siete (`NetworkChangeReceiver`) sa okamžite skúsi znova
5. Navyše každých 15 minút beží `RetryWorker` (WorkManager) ako poistka
6. Po reštarte telefónu (`BootReceiver`) sa periodická úloha naplánuje znova

### 3. UI — viditeľná queue v appke
Na hlavnej obrazovke je teraz sekcia **"ČAKAJÚCE NOTIFIKÁCIE"**, ktorá
zobrazuje:
- počet čakajúcich notifikácií
- názov, čas prijatia, počet pokusov a poslednú chybu pre každú
- tlačidlo **"Odoslať čakajúce teraz"** na manuálny retry

Ak je queue prázdna, vidíš "Žiadne čakajúce notifikácie — všetko odoslané ✓".

## Build a inštalácia

Postup je rovnaký ako predtým:
1. Nahraj všetky súbory na GitHub (zachovaj štruktúru priečinkov)
2. Po commite sa build spustí automaticky
3. Actions → zelený beh → Artifacts → stiahni `VUB-Notifier-APK`
4. **DÔLEŽITÉ:** stiahni cez Files/Stiahnuté súbory, nie priamo z Google Drive
   prehliadača (Drive appka APK súbory niekedy nesprávne otvára ako dokument)
5. Nainštaluj — prepíše predošlú verziu, dáta/nastavenia URL zostanú

## Odporúčanie pre MIUI (Redmi/Xiaomi)

Aby appka spoľahlivo fungovala na pozadí aj po "vyčistení z pamäte":
1. Nastavenia → Aplikácie → VÚB Notifier → Battery saver → **No restrictions**
2. Nastavenia → Aplikácie → VÚB Notifier → Autostart → **zapnúť**
3. V "Recent apps" zamknúť VÚB Notifier (ikona zámky)
