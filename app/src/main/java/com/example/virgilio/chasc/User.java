package com.example.virgilio.chasc;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class User {
    private int id;
    private String name;
    private String surname;
    private String nfc;
    private List<Schedule> schedules;
    private Date created;

    public User() {
        this.id = -1;
        this.name = "";
        this.surname = "";
        this.nfc = "";
        this.schedules = new ArrayList<>();
        this.created = new Date();
    }

    public User(int id, String name, String surname, String nfc, List<Schedule> schedules) {
        this.id = id;
        this.name = name;
        this.surname = surname;
        this.nfc = nfc;
        this.schedules = schedules;
        this.created = new Date();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getNfc() {
        return nfc;
    }

    public void setNfc(String nfc) {
        this.nfc = nfc;
    }

    public List<Schedule> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<Schedule> schedules) {
        this.schedules = schedules;
    }

    public void addSchedule(Schedule schedule) {
        this.schedules.add(schedule);
    }

    public void addSchedule(List<Schedule> schedules) {
        this.schedules.addAll(schedules);
    }

    public void delSchedule(Schedule schedule) {
        this.schedules.remove(schedule);
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Schedule getSchedule(Date date) {
        // Get user schedule by date
        Schedule schedule = null;
        for (Schedule sch : schedules) {
            if (sch.getDate().equals(date)) schedule = sch;
        }
        return schedule;
    }

    @Override
    public String toString() {
        return surname + ", " + name;
    }


    @Override
    public boolean equals(Object obj) {
        // Compare IDs
        User user = (User) obj;

        return this.id == user.getId();
    }
}
