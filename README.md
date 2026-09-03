# JB Smart Modern (v0.1)

یک پروژه Android Studio برای کنترل لامپ‌اسپیکر JB Smart / iBluz با Android جدید.

## چیزی که از APK اصلی استخراج شده
- SDK: Actions Semiconductor iBluz
- Android original app uses the iBluz SPP transport by default.
- Light command: `BluzManager.buildKey(4, 131)`
- Packed color: `(R << 16) | (B << 8) | G`
- Two light states used by the original app: `0x50` and `0x51`
- BLE services in iBluz SDK:
  - `00006666-0000-1000-8000-00805f9b34fb`
  - `00007777-0000-1000-8000-00805f9b34fb`

## امکانات نسخه 0.1
- مجوزهای Bluetooth سازگار با Android 12+
- اسکن دستگاه‌ها
- انتخاب لامپ
- اتصال از طریق iBluz
- روشن/خاموش
- چند رنگ ثابت

## Build
1. پروژه را با Android Studio باز کنید.
2. اجازه دهید Gradle Sync انجام شود.
3. گوشی Android را وصل کنید.
4. Run را بزنید.

Dependency:
`com.actions:ibluz:1.3.6`

این artifact قدیمی در JCenter منتشر شده بود. پروژه از mirror جCenter در Aliyun استفاده می‌کند.

## مهم
این نسخه برای تست واقعی روی لامپ تهیه شده است. چون SDK اصلی قدیمی است، ممکن است در بعضی نسخه‌های Android لازم باشد روش اتصال یا mapping حالت 0x50/0x51 را با نتیجه تست اصلاح کنیم.

اگر دکمه روشن و خاموش برعکس بود، فقط این خط را در `sendRgbPower` عوض کنید:
`int state = on ? 0x51 : 0x50;`
به:
`int state = on ? 0x50 : 0x51;`
