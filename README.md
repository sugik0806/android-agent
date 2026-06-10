# Legal Remote Android Agent

Starter Android Agent Kotlin untuk dashboard Laravel `file-share`.

Mode keamanan:
- Service berjalan sebagai foreground service dengan notifikasi permanen.
- Command sensitif tidak dieksekusi diam-diam.
- Android menampilkan notifikasi dan user harus membuka aplikasi lalu menekan persetujuan.
- CameraX snapshot berjalan dari Activity yang terlihat.
- Screenshot tetap harus memakai prompt MediaProjection resmi Android.

Build debug:
```bash
cd android-agent
gradlew.bat assembleDebug
```

Build release:
1. Buka folder `android-agent` di Android Studio.
2. Ubah `serverUrl` default di `AgentPrefs.kt` atau isi dari UI aplikasi.
3. Build > Generate Signed Bundle / APK.
4. Pilih APK, buat/import keystore.
5. Build variant `release`.

Endpoint Laravel yang dipakai:
- `POST /api/register`
- `POST /api/heartbeat`
- `GET /api/commands?device_id=...`
- `POST /api/commands/{id}/executed`
- `POST /api/upload/photo`
- `POST /api/location`
