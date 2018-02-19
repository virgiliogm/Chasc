package com.example.virgilio.chasc;

import java.util.Calendar;
import java.util.Date;

public class Schedule {
    public enum Event {ATTENDED, NO_SHOW}

    private int id;
    private int userId;
    private Date date;
    private String ref;
    private String refText;
    private Date created;
    private Event event;

    public Schedule() {
        this.userId = -1;
        this.id = -1;
        this.date = new Date(0);
        this.created = new Date();
    }

    public Schedule(int id, User user, Date date) {
        this.id = id;
        this.userId = user.getId();
        this.date = date;
        this.created = new Date();
    }

    public Schedule(int id, User user, Date date, String ref, String refText) {
        this.id = id;
        this.userId = user.getId();
        this.date = date;
        this.ref = ref;
        this.refText = refText;
        this.created = new Date();
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getRefText() {
        return refText;
    }

    public void setRefText(String refText) {
        this.refText = refText;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    @Override
    public boolean equals(Object obj) {
        // Return true if belongs to the same user and dates are at the same year, month and day, but ignoring time
        Schedule sch = (Schedule) obj;

        Calendar target = Calendar.getInstance();
        target.setTime(sch.getDate());

        Calendar source = Calendar.getInstance();
        source.setTime(this.date);
        return (this.userId == sch.getUserId() &&
                source.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                source.get(Calendar.MONTH) == target.get(Calendar.MONTH) &&
                source.get(Calendar.DAY_OF_MONTH) == target.get(Calendar.DAY_OF_MONTH));
    }
}
