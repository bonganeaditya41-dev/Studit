# Studit — Student Knowledge Community

Native Android app (Kotlin + Jetpack Compose + Material 3) built on Firebase.
Reddit-style academic Q&A: communities, questions, answers, votes, reputation,
bookmarks, search, leaderboards and in-app notifications.

- Package name: `com.studyit`
- Firebase project: `studdyit` (config already placed at `app/google-services.json`)
- Stack: Kotlin 2.3.21 · AGP 8.13.2 · Gradle 8.13 · Compose BOM 2026.03.01 · Firebase BoM 35.1.0
- Architecture: MVVM + repository pattern, Hilt DI, Coroutines + StateFlow, Navigation Compose
- `minSdk` 24 · `targetSdk` 36

## Build scope (per your instruction)

Included: full source tree, `firestore.rules`, `storage.rules`.
Deliberately excluded: Cloud Functions and `firestore.indexes.json` (Spark / free plan).

Because Cloud Functions are unavailable on Spark, two things work differently from the
original brief — both are handled in code, not faked:

1. **Reputation points** are written by the app (`PointsRepository`) instead of a
   callable function. `firestore.rules` bounds every write: a profile update may raise
   `totalPoints` by at most 30, can never make it negative, and cannot change `uid` or
   `usernameLower`. Votes are single-document-per-user, so repeat-voting the same post
   is impossible. This is solid for a real community, but it is *not* cryptographically
   tamper-proof — a modified client could still add up to 30 points per write. Move
   `PointsRepository.awardPoints()` into a callable Cloud Function when you upgrade to
   Blaze.
2. **Push notifications**: the app writes an in-app notification document for every
   answer, reply, upvote and accepted answer, and it receives FCM messages
   (`StuditMessagingService`). Actually *sending* a push to another device needs a
   trusted server, which Spark does not provide — so on the free plan, notifications
   appear in the Notifications tab instantly, and push delivery works for messages you
   send from the Firebase Console. Add a function (or your own server) to push those
   same documents as FCM messages later.

No composite indexes are required: every query is single-field, and ordering that would
have needed a composite index (community feed, answers, comments) is done client-side.

## First run — Firebase console checklist

1. **Authentication → Sign-in method**: enable *Email/Password* and *Google*.
2. **Google Sign-In SHA fingerprints**: add your debug SHA-1 (and SHA-256) under
   *Project settings → Your apps → com.studyit*. Without this, Google sign-in returns
   `ApiException: 10 (DEVELOPER_ERROR)`.
   ```
   ./gradlew signingReport        # prints SHA-1 / SHA-256
   ```
3. **Firestore Database**: create the database (production mode), then paste the
   contents of `firestore.rules` into *Firestore → Rules* and publish.
4. **Storage**: create the bucket, then paste `storage.rules` into *Storage → Rules*.
5. **Cloud Messaging** works out of the box; no server key is stored in the app.
6. Optional but recommended: create the starter communities (`r/Physics`,
   `r/Chemistry`, `r/Biology`, `r/Mathematics`, `r/ComputerScience`, `r/NEET`,
   `r/Class11`, `r/Class12`, `r/StudyTips`) from inside the app with *Communities →
   New space* — the app has no seed script because it must not ship admin credentials.

## Open and run

```bash
# Android Studio: File → Open → this folder, then Run 'app'
./gradlew :app:assembleDebug
```

If the Gradle wrapper jar is missing in your copy, run `gradle wrapper` once (or let
Android Studio regenerate it) — the wrapper scripts and properties are included.

## Project layout

```
app/src/main/java/com/studyit/
  core/            constants, UiState, extensions, Firestore snapshot helpers
  data/model/      Firestore data classes (User, Post, Answer, Comment, Vote, ...)
  data/repository/ Auth, User, Post, Answer, Vote, Community, Bookmark,
                   Notification, Storage, Moderation, Points repositories
  data/local/      DraftStore (question drafts), SearchHistoryStore
  di/              FirebaseModule (Auth, Firestore, Storage)
  service/         StuditMessagingService (FCM)
  ui/              Compose screens grouped by feature, each with its own ViewModel
  ui/components/   GlassCard, PostCard, Avatar, Tag, dropdowns, empty/error states
  ui/theme/        Material 3 theme, blue/white palette, light + dark
firestore.rules    Firestore security rules
storage.rules      Cloud Storage rules
```

## Firestore collections

`users`, `communities`, `communityMembers`, `posts`, `answers`, `comments`, `votes`,
`bookmarks`, `notifications`, `reports`, `pointsEvents`.

`pointsEvents` is the ledger behind the weekly/monthly leaderboards — it is read with a
single `createdAt >= window start` range query and aggregated on the client, so no
scheduled reset job is needed.

## Feature map

| Area | Where |
| --- | --- |
| Email/password + Google sign-in, reset, verification, persistent session | `ui/auth`, `SessionViewModel` |
| Home feed (Latest / Popular / Trending / Unanswered) with pagination | `ui/home` |
| Ask question (subject, class, community, tags, image, draft) | `ui/post/AskQuestionScreen` |
| Question detail, answers, replies, accept, votes, bookmark, share, report | `ui/post/PostDetailScreen` |
| Communities: browse, search, join/leave, rules, create | `ui/communities` |
| Points, ranks, weekly/monthly/all-time leaderboards | `PointsRepository`, `ui/leaderboard` |
| Profile, edit profile, photo upload, privacy toggle | `ui/profile`, `ui/settings` |
| Global search: posts, communities, users, subjects + recent history | `ui/search` |
| Bookmarks | `ui/bookmarks` |
| Notifications inbox | `ui/notifications` |

## Getting an installable APK

The project compiles cleanly (66 Kotlin files, Kotlin 2.3.21 + AGP 8.13.2, Hilt components
generated, zero errors). It was **not** packaged inside the assistant's sandbox: every
command there is capped at 300 s on 2 throttled vCPUs, and `D8` dexing alone measured
~60 s per library jar across ~170 jars, so packaging cannot finish. Nothing is wrong with
the code — only with that machine.

**GitHub Actions (configured).** `.github/workflows/android.yml` builds a signed release
APK, prints the signing certificate fingerprints, and uploads both as the
`studit-release-apk` artifact:

```bash
git init && git add -A && git commit -m "Studit"
gh repo create Studyt --private --source=. --push
gh run watch                       # ~5 minutes
gh run download --name studit-release-apk
```

For a release-signed APK (rather than one signed with the debug key), add three repository
secrets first — `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` — from
your own keystore:

```bash
base64 -w0 studit-release.keystore | gh secret set KEYSTORE_BASE64
gh secret set KEYSTORE_PASSWORD
gh secret set KEY_ALIAS
gh secret set KEY_PASSWORD
```

Without those secrets the workflow still produces an installable APK, signed with the
runner's debug key.

**Android Studio.** File → Open → this folder → Run 'app'; or Build → Generate Signed
Bundle / APK → APK for a release build. `release.keystore` in the project root is picked up
automatically when present (`KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` env vars).

## Version pins that matter (do not bump blindly)

These combinations are the ones that actually compile together on AGP 8.13 / Kotlin 2.3:

| Component | Pinned | Why |
| --- | --- | --- |
| AGP | 8.13.2 | newest AGP that still runs the classic DSL |
| Kotlin | 2.3.21 | matches the Compose compiler plugin |
| Hilt | 2.57.2 | 2.58+ requires AGP 9; 2.57.2 + the `kotlin-metadata-jvm` force below |
| AndroidX core / lifecycle / navigation / activity | 1.16.0 / 2.9.4 / 2.9.6 / 1.10.1 | 2026 releases require AGP 9.1+ and compileSdk 37 |
| Compose BOM | 2025.12.01 | last BOM that targets compileSdk 36 |
| play-services-auth | 21.6.0 | 22.0.0 removed the `GoogleSignIn` client used for Google login |

`app/build.gradle.kts` forces `org.jetbrains.kotlin:kotlin-metadata-jvm:2.3.21` because
Hilt 2.57.2 ships an older copy that cannot read Kotlin 2.3 metadata.

If you upgrade to AGP 9.x later, you can move all of these back to their newest versions.

## Known limitations (honest list)

- Reputation is client-written (see above); rules bound it but cannot fully verify it.
- Push delivery to other devices needs a server; in-app notifications are complete.
- Search is prefix-based (`titleLower`, `usernameLower`, `nameLower`) — Firestore has no
  full-text search on Spark. Swap in Algolia/Typesense later if needed.
- Usernames are immutable after registration (enforced in rules), matching the "unique
  username" requirement.
- Answer/comment counters are maintained client-side; they can drift if a write fails
  halfway, so a periodic reconciliation (or a Blaze function) is advisable at scale.
