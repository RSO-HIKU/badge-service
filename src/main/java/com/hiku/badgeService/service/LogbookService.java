package com.hiku.badgeService.service;

import com.hiku.badgeService.db.dao.LogbookDao;
import com.hiku.badgeService.db.models.LogbookEntry;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.time.Instant;
import java.util.List;

@ApplicationScoped
public class LogbookService {

    @Inject
    LogbookDao logbookDao;

    public List<LogbookEntry> listForUser(Integer userId) {
        return logbookDao.findByUser(userId);
    }

    @Transactional
    public LogbookEntry addEntry(Integer userId, Integer peakId, Instant addedAt, String notes) {
        LogbookEntry entry = new LogbookEntry();
        entry.setUserId(userId);
        entry.setPeakId(peakId);
        entry.setNotes(notes);
        entry.setAddedAt(addedAt != null ? addedAt : Instant.now());
        return logbookDao.insert(entry);
    }
}
