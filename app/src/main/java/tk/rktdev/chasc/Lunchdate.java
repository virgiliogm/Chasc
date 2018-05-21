package tk.rktdev.chasc;

import java.util.Calendar;
import java.util.Date;

public class Lunchdate {
    public enum Event {ATTENDED, NO_SHOW}

    private String id;
    private String account;
    private String userId;
    private Date date;
    private String ref;
    private String refText;
    private String status;

    public Lunchdate(String account, String userId, Date date) {
        this.account = account;
        this.userId = userId;
        this.date = date;
    }

    public Lunchdate(String account, String userId, Date date, String ref, String refText) {
        this.account = account;
        this.userId = userId;
        this.date = date;
        this.ref = ref;
        this.refText = refText;
    }

    public Lunchdate(String id, String account, String userId, Date date) {
        this.id = id;
        this.account = account;
        this.userId = userId;
        this.date = date;
    }

    public Lunchdate(String id, String account, String userId, Date date, String ref, String refText) {
        this.id = id;
        this.account = account;
        this.userId = userId;
        this.date = date;
        this.ref = ref;
        this.refText = refText;
    }

    public Lunchdate(String id, String account, String userId, Date date, String ref, String refText, String status) {
        this.id = id;
        this.account = account;
        this.userId = userId;
        this.date = date;
        this.ref = ref;
        this.refText = refText;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object obj) {
        // Return true if belongs to the same user and dates are at the same year, month and day, but ignoring time
        Lunchdate sch = (Lunchdate) obj;

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
