-- Run once on an existing database. Safe to re-run.

-- Salted password hashes need more room than the old plain-text VARCHAR(30).
ALTER TABLE Users MODIFY password VARCHAR(255) COLLATE utf8mb4_bin NOT NULL;

-- Review hashes were unsalted SHA-256 of "RA:course", which RA numbers make guessable.
-- They are replaced by keyed HMACs, so the old rows can't match and are removed.
-- The 14-day review cooldown restarts for everyone once.
DELETE FROM Submissions;

-- Each logged misconduct outcome is kept, so changing an outcome leaves a trail.
CREATE TABLE IF NOT EXISTS MisconductOutcomes (
  outcome_id   INT AUTO_INCREMENT PRIMARY KEY,
  report_id    INT NOT NULL,
  outcome      VARCHAR(40) NOT NULL,
  outcome_note VARCHAR(300),
  logged_on    DATE NOT NULL,
  FOREIGN KEY (report_id) REFERENCES MisconductReports(report_id) ON DELETE CASCADE
);
