# المحاسب — إعادة البناء

هذا المستودع مخصص لإعادة بناء تطبيق Android انطلاقًا من ملفات التحليل في [almahaseb-pro-apk-analysis](https://github.com/almasawa91-hub/almahaseb-pro-apk-analysis).

## حالة العمل
- الملفات المرجعية المفككة تحتوي على `sources/` و`resources/` (JADX-style)، وليست مشروع Gradle جاهزًا ولا تحتوي على ملفات `smali/` أو `apktool.yml`.
- أُنشئ هنا أساس Android قابلًا للبناء وCI لإنتاج APK تجريبي غير موقّع.
- هذا الأساس ليس بعد إعادة بناء كاملة للتطبيق الأصلي؛ يلزم استعادة السلوك والواجهات من الشيفرات المفككة والموارد والتحقق منها تدريجيًا.

## البناء
يعمل GitHub Actions على بناء Debug APK عند الدفع إلى `main`، ويمكن تنزيله من Artifacts باسم `approved-debug-apk`.
