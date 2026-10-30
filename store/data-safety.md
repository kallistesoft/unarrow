# Play Console: Data safety, ads and content rating

Answers for the Play Console forms, from what the app and its SDKs do. Check them against the SDK providers' current
guidance when filling the forms.

## App content

- **Ads:** yes, the app contains ads (AdMob).
- **Target audience:** 13 and older (not designed for children, so no Families policy requirements).
- **Content rating (IARC):** puzzle game; no violence, no user-generated content, no chat, no gambling; in-app
  purchases yes. Expected: Everyone / PEGI 3.
- **Government app, news app, health:** no.
- **Account:** none; no sign-in, no account deletion flow needed.

## Data safety

No data is collected by Kallistesoft (no server). The SDKs collect:

| Data | Collected by | Purpose | Shared |
|---|---|---|---|
| Device or other IDs (advertising ID) | Google Mobile Ads | Advertising, analytics, fraud prevention | Yes (ad partners) |
| App interactions, diagnostics | Google Mobile Ads | Advertising, analytics | Yes |
| Crash logs, diagnostics | Firebase Crashlytics (release builds) | App functionality, analytics | No |
| Purchase history | Google Play Billing | App functionality | No |

- **Encrypted in transit:** yes (all SDKs use HTTPS).
- **Users can request deletion:** progress is on the device only (uninstall deletes it); ad data per Google's policy.
- **Consent:** Google's User Messaging Platform asks for consent where required (EEA, UK, Switzerland), and Settings
  offers "Privacy options" there.

## Privacy policy

A privacy policy URL is required (ads). It must cover AdMob, Crashlytics and Play Billing, and give
contact@kallistesoft.ca as the contact.
