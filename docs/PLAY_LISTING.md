# Play Store listing (draft)

Everything here is draft text for the owner to review. Nothing has been submitted.

## Basics
- **App name:** Flipday (Arabic listing: فليب داي) — name clearance not done.
- **Package:** `app.flipday`
- **Category:** Tools (or Productivity). **Pricing:** free, no ads, no in-app purchases.
- **Languages:** English, Arabic (RTL).

## Short description (max 80)
- EN: Flip-clock countdowns, count-ups and periods, with home-screen widgets.
- AR: عدّ تنازلي وتصاعدي وفترات بأسلوب الفلاب، مع ودجات للشاشة الرئيسية.

## Full description
**EN**
Count the days to what matters, and the days since.

Flipday shows time as split-flap tiles you can read at a glance. Add a countdown to a date, a birthday that shows age and the next birthday, a count-up from a start date, or a period with a start and an end date (a trip, a course, a no-sugar month) and see how far in you are.

- Countdowns, birthdays, count-ups and periods
- Home-screen widgets in six styles: flap, paper, ring, age, date and live ticker
- Share a card as a square, story or wide image
- Reminders before the date, at the time you choose
- Archive for finished events, search, sort and filters
- English and Arabic, right-to-left, Western or Arabic-Indic numerals
- Light and dark themes
- Export your events to a file and import them back

Private by design: no account, no ads, no analytics, no internet connection. Everything stays on your phone.

**AR**
احسب الأيام المتبقية لما يهمّك، والأيام التي مرّت.

يعرض فليب داي الوقت بلوحات الفلاب لتقرأه بنظرة. أضف عدًّا تنازليًا لموعد، أو عيد ميلاد يُظهر العمر وموعد العيد القادم، أو عدًّا تصاعديًا منذ تاريخ، أو فترة لها بداية ونهاية (رحلة، دورة، شهر بلا سكر) وشاهد أين وصلت منها.

- عدّ تنازلي وأعياد ميلاد وعدّ تصاعدي وفترات
- ودجات للشاشة الرئيسية بستة أنماط: فلاب وورق وحلقة وعمر وتاريخ وعدّاد حيّ
- شارك بطاقة بصورة مربّعة أو قصة أو عريضة
- تذكيرات قبل الموعد، في الوقت الذي تختاره
- أرشيف للأحداث المنتهية وبحث وفرز وتصفية
- العربية والإنجليزية، من اليمين إلى اليسار، وأرقام غربية أو هندية
- مظهر فاتح وداكن
- صدّر أحداثك إلى ملف واستوردها من جديد

خصوصية بالتصميم: لا حساب ولا إعلانات ولا تحليلات ولا اتصال بالإنترنت. كل شيء يبقى على هاتفك.

## Data safety form (answers)
- Data collected: **none**. Data shared: **none**.
- Network: the app has no `INTERNET` permission.
- Encryption in transit: not applicable. Deletion: user deletes events in the app or uninstalls.
- Device backup: Android Auto Backup may copy events and settings to the user's own Google account (OS feature).

## Permissions and declarations
| Permission | Why | Notes |
|---|---|---|
| `POST_NOTIFICATIONS` | Event reminders | Asked in context, with a rationale (design 06B.6) |
| `RECEIVE_BOOT_COMPLETED` | Re-schedule reminders after reboot | |
| `SCHEDULE_EXACT_ALARM` | Only when the person picks a custom reminder time and allows it | Opt-in (06C.3). `USE_EXACT_ALARM` is not declared. Play may ask for a declaration. |

## Content rating
Everyone. No user-generated content shared with others, no ads, no purchases.

## Assets still needed
- Screenshots (phone, 7-inch and 10-inch tablet): home, add, detail, period, widgets gallery, widget studio, share card, Arabic home, dark home.
- Feature graphic 1024x500, icon 512x512 (launcher icon exists as adaptive icon).
- A hosted privacy policy URL (the text is in the app and in `strings.xml` key `privacy_body`; Play needs a public URL, for example GitHub Pages).
- Signing: upload key and Play App Signing; a release build config. Not set up.
