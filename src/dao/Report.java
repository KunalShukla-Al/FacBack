package dao;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One misconduct report as the teacher sees it: the subject, the media, an
 * optional note, and when it was filed. There is no reporter field — the
 * teacher never learns who reported. The outcome fields are null until logged.
 */
public record Report(
        int id,
        String courseCode,
        String courseName,
        String mediaPath,
        String note,
        LocalDateTime reportedAt,
        String outcome,
        String outcomeNote,
        LocalDate outcomeOn
) {}
