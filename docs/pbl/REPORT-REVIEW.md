# Review of FacBack PBL3 Report

Reviewed the supplied 17-page FacBack_PBL3_Report.docx against the security-and-misconduct branch and the PBL-II threading update. The DOCX itself has not been changed.

Required corrections before submission:

- Page 7: remove the literal angle-bracket literature-review placeholder. Either provide an actual sourced comparison or call this section Technical Background and check that the required report format permits that.
- Pages 3, 9-10 and 13: replace "server key/time" with "application secret" and "desktop application timestamp". The current app has no trusted backend server issuing these values.
- Pages 3 and 8-10: describe identity exclusion from review rows and teacher-facing queries precisely. Do not promise that administrators, local database users, identifying comments or media metadata cannot reveal identity.
- Pages 3, 8-9: logic.ReviewHasher reads a key file and environment variables, so the entire logic package is not pure. The scoring rules are pure.
- Chapter 5: add a subsection explaining SwingWorker, doInBackground, done, EDT input snapshots, busy controls, error propagation and LoginForm.invokeLater. See PBL-COVERAGE.md.
- Chapter 6: retain the 11 existing logic checks and add the two reproducible background-task scenarios. Separate those automated checks from a manual UI/database demonstration.
- Page 15: the report says integration scripts ran locally but are absent from the repository. Retain those claims only with their actual scripts/logs or rerun and preserve evidence. This review did not rerun those database checks.
- Page 11: the migration does not itself back up submission rows. The backup statement requires separate evidence. Its DELETE runs again on every invocation.
- Page 15: remove or recompute the source-line counts after the threading changes; they are not a useful success metric.
- Pages 12-14: recheck screenshots against the final application, including the taller Import Marks screen and progress indicator.
- Page 16: preserve the declared limitations, add single-machine media storage and client-side access-control limitations, and distinguish compilation/thread tests from a visually checked full demonstration.

The supplied assessment table alone cannot verify all Review 1/3 expectations or the separate scenario-based assessments.
