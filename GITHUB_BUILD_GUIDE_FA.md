# ساخت APK با GitHub Actions

این پروژه طوری آماده شده که بدون Android Studio در GitHub کامپایل شود.

## مراحل

1. وارد GitHub شوید و یک Repository جدید بسازید.
2. Repository را Public یا Private بسازید.
3. تمام محتویات این پوشه را در ریشه Repository آپلود کنید.
   مهم: پوشه `.github` هم باید آپلود شود.
4. بعد از آپلود فایل‌ها، وارد تب **Actions** شوید.
5. Workflow با نام **Build Android APK** را باز کنید.
6. روی **Run workflow** بزنید و اجرا را تأیید کنید.
7. چند دقیقه صبر کنید تا Build سبز شود.
8. وارد همان اجرای Workflow شوید.
9. پایین صفحه در بخش **Artifacts** فایل:
   `JB-Smart-Modern-debug-apk`
   را دانلود کنید.
10. ZIP دانلودشده را باز کنید؛ داخل آن فایل:
   `app-debug.apk`
   قرار دارد.
11. APK را به گوشی Android منتقل و نصب کنید.

## اگر Build خطا داد
از صفحه Actions وارد اجرای ناموفق شوید و متن خطا را برای ChatGPT بفرستید.

## نکته امنیتی
این خروجی Debug است و برای تست شخصی مناسب است. برای انتشار عمومی باید Release APK/AAB امضاشده ساخته شود.
