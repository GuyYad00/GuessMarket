# עיצוב המערכת — לפי הנחיות המרצה (פוסטים בפורום, אוגוסט 2026)

## עקרונות מחייבים מהמרצה
1. **Interface בין UI למנוע** — ה-UI מכיר את המנוע רק דרך interface שמגדיר את כל
   פעולות המנוע (מתודה לפחות לכל פקודת תפריט). המופע הקונקרטי נוצר במקום אחד
   בראשית המע'.
2. **DTO ולא אובייקטי ליבה** — המנוע לעולם לא מחזיר את אובייקטי הליבה שלו ל-UI
   (encapsulation!). מחזירים DTO: immutable, ללא לוגיקה, constructor + getters בלבד.
   DTO נוצר מחדש בכל קריאה. חובה DTO **לעומק** — גם אובייקטים פנימיים (למשל
   היסטוריית מסחר) מומרים ל-DTO.
3. **Exceptions לשגיאות קובץ** — בדיקת ה-XML זורקת exceptions מפורטים (מה קרה,
   אילו אלמנטים מעורבים, איך לתקן). המרצה ממליץ **unchecked**. ה-UI תופס ומציג.
   מיקום: package בשם `exception` (ביחיד!).
4. **כל הלוגיקה במנוע** — טעינת קובץ, בדיקות קלט, המרות — הכול במנוע. ה-UI רק
   אוסף קלט ומציג פלט. (ה-UI כן רשאי לבדוק בחירת תפריט לא קיימת וכד'.)
   המנוע לא סומך על ה-UI — מוודא הכול בעצמו.
5. **jar לכל מודול** — לא fat jar! ההגשה: jar נפרד ל-engine, jar נפרד ל-ui,
   + jars של JAXB. להתנסות בבנייה מוקדם, לא ברבע שעה האחרונה.

## מבנה הפרויקט שלנו
```
GuessMarket/
├── docs/               <- קבצי הקונטקסט האלה
├── lib/                <- JAXB RI jars (jakarta.xml.bind-api, jaxb-impl/runtime, ...)
├── test-files/         <- קבצי XML לבדיקה
├── engine/src/
│   └── engine/
│       ├── api/        <- Engine (interface), EngineImpl
│       ├── core/       <- Event, EventOption, Trade, לוגיקת LMSR
│       ├── dto/        <- EventDTO, TradeStateDTO, OptionStateDTO, TradeDTO,
│       │                  BuyResultDTO, CloseResultDTO
│       ├── exception/  <- InvalidFileException + נגזרות
│       └── jaxb/       <- GuessMarketJaxb, GmEventJaxb, ... (מיפוי 1:1 לסכמה)
├── ui/src/
│   └── ui/             <- Main, ConsoleUI (תפריט, קלט, פורמט פלט)
├── build.bat           <- קומפילציה + יצירת jars ל-out/
├── run.bat             <- הרצה
└── out/                <- תוצרי בנייה (engine.jar, ui.jar)
```

## החלטות מימוש (לתעד ב-readme)
- חשבון אירוע מתחיל ב-`-b*ln(2)` (הסבסוד). ראו LMSR.md.
- עמלת on-close: משולם לזוכים `$1*(1-C%)` למניה; העמלה נזקפת לחשבון האירוע.
- כמות מניות בקנייה: מספר שלם חיובי (> 0).
- b חייב להיות > 0 (ולידציה בטעינה, מעבר לנדרש).
- בדיוק 2 אפשרויות לאירוע (לפי הסכמה והמסמך).
- Java 25. JAXB RI 4.0.5 (jakarta.xml.bind).

## סטטוס עבודה (לעדכן תוך כדי!)
- [x] קבצי קונטקסט
- [x] JDK 25 מותקן (C:\Program Files\Java\jdk-25.0.4.1)
- [x] JAXB ב-lib/
- [x] קבצי בדיקה משוחזרים
- [x] Engine
- [x] UI
- [x] build + בדיקות (3 תרחישים עברו, מספרי LMSR תואמים למסמך)
- [x] בונוס Save/Load (פקודות 7-8)
- [x] GitHub: https://github.com/GuyYad00/GuessMarket (ציבורי)
- [x] ZIP: submission/GuessMarket-EX1.zip — נבדק על "מערכת נקייה"
- [x] readme.docx כולל את פרטי שני המגישים (שמות, ת.ז., אימיילים)

## תהליך בנייה ואריזה
1. `set JAVA_HOME=C:\Program Files\Java\jdk-25.0.4.1` ואז `build.bat` — מקמפל
   ומייצר את `dist\` (engine.jar, ui.jar, lib\, run.bat).
2. `package.bat` — בונה מחדש את `dist\readme.docx` מתוך `readme-src\`
   ואורז את `submission\GuessMarket-EX1.zip`.

לעריכת ה-readme: `readme-src/word/document.xml` ואז `package.bat`.
(שים לב: `build.bat` מוחק את `out\` בכל ריצה, ולכן מקור ה-readme יושב ב-`readme-src\`.)
