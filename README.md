# Digital Student Card

Digital student cards with photo, signed QR verification, an approval workflow for the card office and a gate
scanner for campus security.

**Spring Boot 4.1 · Java 21 · PostgreSQL 18 · Flyway · Spring Security · Thymeleaf · Docker Compose**

This is the Spring Boot version of the earlier NetBeans project (Jakarta EE EJB + WAR on GlassFish with Java DB /
Derby). Everything now lives in one Spring Boot application and runs with one command.

## Run it with Docker (recommended)

You only need **Docker Desktop**. No Java, Maven, GlassFish or database install.

```powershell
cd digital-student-card
docker compose up --build
```

The first build downloads Maven dependencies and takes a few minutes. When the log shows
`Started DigitalStudentCardApplication`, open <http://localhost:8080>.

Demo accounts are created on the first start (turn this off with `APP_DEMO_SEED=false` in `.env`):

| Who | Where | Sign in with | Password |
|---|---|---|---|
| Administrator | <http://localhost:8080/admin/login> | `admin` | `Admin#2026` |
| Security officer | <http://localhost:8080/admin/login> | `security.gate1` | `Gate#2026x` |
| Students (6 fictional) | <http://localhost:8080/login> | email, e.g. `thabo.mokoena@example.com` | `Student#2026` |

Two of the demo students (Naledi Khumalo and Ayanda Zulu) self-registered, so they wait in **Awaiting approval**.

Useful commands:

```powershell
docker compose up --build -d     # run in the background
docker compose logs -f app       # follow the application log
docker compose down              # stop (data is kept)
docker compose down -v           # stop and delete the database
```

To change settings, copy `.env.example` to `.env` and edit it, then run `docker compose up -d` again.

### Scanning from a phone

QR codes contain a link to `/verify`. For phones on the same Wi-Fi to open it, set
`APP_PUBLIC_BASE_URL=http://<your-PC-IP>:8080` in `.env` (find the IP with `ipconfig`). Browsers only allow the
camera scanner page on `https://` or on `localhost`, so on phones either put the app behind HTTPS or let security
use the phone's normal camera app, which opens the `/verify` page.

## Run it without Docker (VS Code / IntelliJ)

Needs **JDK 21** and a PostgreSQL database called `studentcard` (user and password `postgres`). The quickest way
to get the database is still Docker:

```powershell
docker compose up -d postgres
.\mvnw.cmd spring-boot:run                    # Windows
./mvnw spring-boot:run                        # macOS / Linux
```

`mvnw` downloads Maven by itself. To use your own database set `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`. Set `APP_DEMO_SEED=true` for the demo accounts, or
open <http://localhost:8080/admin/login> and create the first administrator on the setup page.

## What each person can do

**Students** (`/register`, `/login`, `/student`)
- Register with an SA ID number (date of birth, gender, citizenship and check digit are read as you type), photo,
  campus, faculty, qualification and year.
- See the card with a live ticking clock and moving hologram, so a screenshot is easy to tell apart from the real
  card; flip it to the QR side; full-screen mode keeps the screen on at gates.
- Download a print-ready PDF card (bank-card size, front and back) or email it to themselves.
- See every time their card was checked, update email/phone, change password.

**Card office (administrators)** (`/admin`)
- Overview: valid / pending / expired counts, cards expiring within 30 days, checks today, registrations per month,
  students per campus.
- Student directory with search, status tabs, campus and faculty filters, sorting, paging and CSV export.
- Approve self-registrations (one by one or in bulk), renew, suspend with a reason, revoke, re-activate.
- **Replace card**: issues a new card version; the QR code on the lost card stops working immediately.
- Edit details, replace photo, reset password (one-time temporary password), delete with typed confirmation.
- Card-check log, a full activity log (who did what and when), staff accounts with two roles.

**Campus security** (`/scan`)
- Camera scanner (phone or laptop) that checks cards continuously: green / amber / red result with the photo on
  record, beep and vibration, session history, location label. Student numbers can be typed in.
- Anyone can scan a card with a normal phone camera: it opens `/verify`, which shows the live status and photo only.

## Settings

All settings are environment variables (in `.env` for Docker) with defaults in
`src/main/resources/application.properties`.

| Variable | What it does | Default |
|---|---|---|
| `APP_INSTITUTION_NAME`, `APP_INSTITUTION_SHORT_NAME` | Name on cards, pages and emails | Tshwane University of Technology, TUT (Docker) |
| `APP_CARD_OFFICE` | Where students go for help | the Student Card Office |
| `APP_PUBLIC_BASE_URL` | Address put inside QR codes and emails | the address the page was opened with |
| `APP_CARD_VALIDITY_MONTHS` | How long new and renewed cards are valid | 12 |
| `APP_REGISTRATION_APPROVAL` | `false` makes self-registered cards valid immediately | true |
| `APP_SETUP_KEY` | If set, the first-administrator page asks for this key | not set |
| `APP_DEMO_SEED` | Demo staff and students on an empty database | true in Docker, false otherwise |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `APP_MAIL_FROM` | Email for sending cards. Gmail: `smtp.gmail.com`, `587`, an app password | email off |
| `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB` | Database account (Docker) | postgres / postgres / studentcard |

Campuses and faculties are listed in `application.properties` (`app.campuses`, `app.departments`).

## What changed from the GlassFish / Derby version

| Before | Now |
|---|---|
| Two NetBeans projects (EJB jar + WAR), deployed to GlassFish | One Spring Boot app (`java -jar` or Docker) |
| Java DB (Derby) through GlassFish's `jdbc/__default`, tables made by EclipseLink | PostgreSQL 18; tables made by a Flyway migration (`db/migration/V1__create_schema.sql`), Hibernate only validates |
| EJBs (`@Stateless`) and servlets | Spring services (`@Service`, `@Transactional`) and Spring MVC controllers |
| JSP + JSTL pages | Thymeleaf templates (`src/main/resources/templates`), same design and CSS |
| Hand-written login filter, PBKDF2 hashes, CSRF tokens | Spring Security: two sign-in areas (staff and students), bcrypt, built-in CSRF protection, login throttling, security headers |
| Photo stored in the student row | Separate `student_photo` table, so lists do not load images |
| Gmail address and app password typed by the student | SMTP set once on the server (optional) |
| `run.ps1` + GlassFish install | `docker compose up --build` |

Old Derby data is **not** copied over: the PostgreSQL database starts empty (or with the demo data). Students
register again, or an administrator adds them under **Register a student**.

## Code map

```
src/main/java/tut/ac/za/dsc/
├── DigitalStudentCardApplication.java
├── config/       AppProperties (app.* settings), SecurityConfig (staff + student sign-in, headers)
├── domain/       Student, StudentPhoto, StaffUser, VerificationLog, AuditEvent, AppSetting, CardStatus, VerificationResult
├── repository/   Spring Data JPA repositories
├── security/     AppUser, AccountLookup, LoginAttempts (lock after repeated failures), SessionLogin
├── service/      StudentService, CardService (approve, renew, suspend, replace, QR signing and checking),
│                 DirectoryService (search, dashboard figures), StaffService, AuditService, MailService,
│                 CardPdf, QrCodes, PhotoProcessor, SAIdNumber, Validators, CsvExport
├── startup/      StartupTasks (QR signing key, demo data)
└── web/          PublicController, RegisterController, StudentController, StaffLoginController,
                  AdminController, ScanController, MediaController
src/main/resources/
├── application.properties
├── db/migration/ Flyway SQL
├── templates/    Thymeleaf pages and fragments
└── static/assets CSS, JS, fonts (SIL OFL), html5-qrcode 2.3.8 (Apache 2.0)
```

## Screenshots

`docs/screenshots` shows the landing page, a student's card, the card office overview, the student directory and
record, the public check page and the phone layout (rendered with sample data).
