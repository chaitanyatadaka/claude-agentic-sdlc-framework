Task: T1 (project scaffold), attempt 2
Date: 2026-11-25 (local run)

Backend
Command: cmd /c "set MAVEN_BASEDIR=backend&& backend\mvnw.cmd -B -f backend\pom.xml verify" (run from project/)
Exit code: 0
Counts: 1 passed, 0 failed, 0 skipped (SmartPantryApplicationTests)
Result: BUILD SUCCESS
Raw log: evidence/raw/T1-2.log (not committed)

Frontend
Command: npm --prefix frontend run build (run from project/)
Exit code: 0
Counts: no tests; Vite transformed 29 modules
Raw log: evidence/raw/T1-2-frontend.log (not committed)

Notes
- The first backend attempt (attempt 2, unset MAVEN_BASEDIR, run from project/) failed with exit 1 because the wrapper looks for .mvn in the current directory. Fixed by configuring MAVEN_BASEDIR in the verification command. The configuration change is in project/sdlc.config.json.
- The frontend build writes to project/backend/src/main/resources/static/, which is now git-ignored.

Commit: 7cf0d4d (sdlc/SP-2, parent d0fb962 on main). Contains the 13 approved T1 files. No model-specific trailer: the model used could not be verified, so no attribution trailer is recorded.
