# סכמת ה-XML של תרגיל 1 (נספח ג' + XSD מקבצי הבדיקה)

## מלכודת קריטית
האלמנט של העמלה נקרא **`comision`** (שגיאת כתיב, c אחת, s אחת) — לא `commission`!
כך זה מופיע ב-XSD הרשמי ובכל קבצי הדוגמה. ה-JAXB חייב למפות לשם הזה בדיוק.

## מבנה (סכמה V1 — תרגיל 1)
```xml
<Guess-Market>                      <!-- שורש. שימו לב למקף! -->
  <GM-events>                       <!-- מכיל את כל האירועים -->
    <GM-event name="Event Name">    <!-- name = attribute, required -->
      <id>1</id>                    <!-- xs:int, מספר שלם חופשי -->
      <description>...</description><!-- מחרוזת חופשית -->
      <comision type="on-purchase">5</comision>
                                    <!-- ערך: xs:int (אחוז). attribute type חובה:
                                         "on-close" או "on-purchase" -->
      <GM-options>
        <GM-option>Hell Yea !</GM-option>  <!-- מחרוזת. maxOccurs=2 בסכמה -->
        <GM-option>No way !</GM-option>
      </GM-options>
      <GM-method>
        <GM-LMSR>                   <!-- השיטה היחידה בתרגיל 1 -->
          <b>100</b>                <!-- xs:int, מדד נזילות, שלם חיובי -->
        </GM-LMSR>
      </GM-method>
    </GM-event>
    <!-- GM-event: maxOccurs=unbounded -->
  </GM-events>
</Guess-Market>
```

סדר האלמנטים בתוך GM-event (xs:sequence): id, description, comision, GM-options, GM-method.
בקבצי הדוגמה יש גם `xmlns:xsi` + `xsi:noNamespaceSchemaLocation="GM-EX1-schema.xsd"` —
אין namespace אמיתי, JAXB בלי namespace.

## בדיקות תקינות (application-wise; מובטח תקין schema-wise)
1. הקובץ קיים בנתיב שנמסר.
2. הקובץ מסתיים ב-`.xml` (די בבדיקת סיומת; case-insensitive).
3. לכל אירוע `id` ייחודי (אין כפילויות).
4. עמלה: `0 <= comision <= 90`.

הערות נוספות מהמסמך:
- מחרוזות עם רווחים בקצוות — לעשות `trim()`.
- b: "מספר שלם חיובי" — הגיוני לוודא b > 0 (לתעד ב-readme כהנחה).
- בדיוק 2 אפשרויות לאירוע (maxOccurs=2 בסכמה; "לכל אירוע יהיו בדיוק שתי תשובות אפשריות").

## קבצי הבדיקה ששוחזרו (GuessMarket/test-files/)
| קובץ | מקור | תקין? | מה בו |
|---|---|---|---|
| `valid-3-events.xml` | בדיקה 5 | תקין | 3 אירועים: Mujtaba (on-purchase 5%, b=100), World Cap (on-close 15%, b=50), Earth Quake (on-purchase 50%, b=400) |
| `valid-1-event.xml` | בדיקה 6 | תקין | אירוע יחיד: Earth Quake (id=3, on-purchase 50%, b=100) |
| `invalid-dup-id.xml` | בדיקה 1 (מידע מפורט) | פסול | שני אירועים עם id=1 (Mujtaba ו-Earth Quake) |
| `invalid-commission.xml` | בדיקה 2 | פסול | World Cap עם עמלה 115 (מעל 90) |
| `not-an-xml.txt` | שלנו | פסול | סיומת לא xml |
| `invalid-negative-commission.xml` | שלנו | פסול | עמלה שלילית |

בדיקה 3 = ה-XSD הרשמי (GM-EX1-schema.xsd). בדיקה 4 = תיאור סימולטור ה-LMSR (לא קובץ מערכת).
