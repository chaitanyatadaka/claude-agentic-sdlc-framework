Task: T2 (category and grocery item persistence), attempt 1

Backend
Command: configured verification[0] (cmd /c "set MAVEN_BASEDIR=backend&& backend\mvnw.cmd -B -f backend\pom.xml verify"), run from project/
Exit code: 0
Counts: 9 passed, 0 failed, 0 errors, 0 skipped (ItemPersistenceTest 8, SmartPantryApplicationTests 1)
Result: BUILD SUCCESS
Raw log: evidence/raw/T2-1.log (not committed)

Commit: 14c2a3d (sdlc/SP-2), four T2 files, parent is the T1 commit 7cf0d4d.

Notes
- Earlier in this attempt, 7 test errors came from test-side property precedence. Fixed in the test before the passing run. Recorded in the implementation log as a failure signature.
- The default profile has no schema generation setting, so the grocery_item table is not created by a normal application start. The test sets it. This is an open issue for the application-shell task (T14), not a T2 defect.
- Two leftover application processes from an earlier run hold the local H2 file. They were not stopped in this step.
