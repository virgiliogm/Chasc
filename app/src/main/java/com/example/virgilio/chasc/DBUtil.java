package com.example.virgilio.chasc;

import org.neodatis.odb.ODB;
import org.neodatis.odb.ODBFactory;
import org.neodatis.odb.ODBRuntimeException;
import org.neodatis.odb.Objects;
import org.neodatis.odb.core.query.IQuery;
import org.neodatis.odb.core.query.criteria.And;
import org.neodatis.odb.core.query.criteria.Or;
import org.neodatis.odb.core.query.criteria.Where;
import org.neodatis.odb.impl.core.query.criteria.CriteriaQuery;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// Class that communicates with the database
public class DBUtil {
    private static File directory;
    private ODB odb;

    public DBUtil() { }

    public static void setDirectory(File directory) {
        DBUtil.directory = directory;
    }

    public void open() {
        if (odb == null || odb.isClosed()) odb = ODBFactory.open(directory+"/chasc-dev.db");
    }

    public void close() {
        if (!odb.isClosed()) odb.close();
    }



    ///////////
    // Users //
    ///////////

    public List<User> usersGet() {
        //Get all users

        open();
        IQuery query = new CriteriaQuery(User.class);
        query.orderByAsc("surname1,surname2,name");
        Objects<User> users = odb.getObjects(query);
        close();
        return new ArrayList<>(users);
    }

    public List<User> usersGet(String search) {
        //Get all users whose name or surnames matches a string

        open();
        IQuery query = new CriteriaQuery(User.class, new Or().add(Where.ilike("name", search))
                                                             .add(Where.ilike("surname", search)));
        query.orderByAsc("surname,name");
        Objects<User> users = odb.getObjects(query);
        close();
        return new ArrayList<>(users);
    }

    public List<User> usersByScheduleDate(Date date) {
        //Get users with schedules for an specific date

        List <Schedule> schedules = schedulesGet(date);

        // Create Criteria to match users whose ID was in any of the schedules programmed for the requested date
        Or idsCriteria = new Or();
        for (Schedule sch : schedules) {
            idsCriteria.add(Where.equal("id", sch.getUserId()));
        }

        open();
        IQuery query = new CriteriaQuery(User.class, idsCriteria);
        query.orderByAsc("surname,name");
        Objects<User> users = odb.getObjects(query);
        close();
        return new ArrayList<>(users);
    }

    public List<User> usersByScheduleDate(Date date, String search) {
        //Get users with schedules for an specific date and whose name or surnames matches a string

        List <Schedule> schedules = schedulesGet(date);

        // Create Criteria to match users whose ID was in any of the schedules programmed for the requested date
        Or idsCriteria = new Or();
        for (Schedule sch : schedules) {
            idsCriteria.add(Where.equal("id", sch.getUserId()));
        }

        open();
        IQuery query = new CriteriaQuery(User.class, new And().add(idsCriteria)
                                                              .add(new Or().add(Where.ilike("name", search))
                                                                           .add(Where.ilike("surname", search))));
        query.orderByAsc("surname,name");
        Objects<User> users = odb.getObjects(query);
        close();
        return new ArrayList<>(users);
    }

    public List<User> usersByScheduleDates(Date from, Date to) {
        //Get users with schedules between two dates

        List <Schedule> schedules = schedulesGet(from, to);

        // Create Criteria to match users whose ID was in any of the schedules requested
        Or idsCriteria = new Or();
        for (Schedule sch : schedules) {
            idsCriteria.add(Where.equal("id", sch.getUserId()));
        }

        open();
        IQuery query = new CriteriaQuery(User.class, idsCriteria);
        query.orderByAsc("surname,name");
        Objects<User> users = odb.getObjects(query);
        close();
        return new ArrayList<>(users);
    }

    public User userById(int uid) {
        //Get one user by ID

        open();
        IQuery query = new CriteriaQuery(User.class, Where.equal("id", uid));
        Objects<User> users = odb.getObjects(query);
        close();
        if (users.isEmpty()) return null;
        else return users.getFirst();
    }

    public User userByNfc(String nfc) {
        //Get one user by NFC ID

        open();
        IQuery query = new CriteriaQuery(User.class, Where.equal("nfc", nfc));
        Objects<User> users = odb.getObjects(query);
        close();
        if (users.isEmpty()) return null;
        else return users.getFirst();
    }

    public boolean userAdd(User user){
        //Add one user

        try {
            open();

            //Get users ordered by ID (greatest first)
            IQuery query = new CriteriaQuery(User.class);
            query.orderByDesc("id");
            Objects<User> users = odb.getObjects(query);

            //If there is no user, the new user ID is 1; else, is the last inserted user ID + 1
            int id = (users.isEmpty())? 1 : users.getFirst().getId() + 1;
            user.setId(id);

            odb.store(user);
            odb.commit();
            return true;
        } catch (ODBRuntimeException e){
            return false;
        } finally {
            close();
        }
    }

    public boolean userMod(User user){
        //Modify user

        try {
            open();

            //Load user, apply changes and save
            IQuery query = new CriteriaQuery(User.class, Where.equal("id", user.getId()));
            User nuser = (User) odb.getObjects(query).getFirst();

            //Check that user exists
            if (nuser == null) return false;

            nuser.setName(user.getName());
            nuser.setSurname(user.getSurname());
            nuser.setNfc(user.getNfc());

            odb.store(nuser);
            odb.commit();
            return true;
        } catch (ODBRuntimeException e){
            return false;
        } finally {
            close();
        }
    }

    public boolean userDel(User user){
        //Delete user

        try {
            open();

            //Delete user schedules
            IQuery schQuery = new CriteriaQuery(Schedule.class, Where.equal("userId", user.getId()));
            Objects<Schedule> schedules = odb.getObjects(schQuery);
            for (Schedule sch : new ArrayList<>(schedules)) {
                odb.delete(sch);
            }

            //Delete user
            IQuery userQuery = new CriteriaQuery(User.class, Where.equal("id", user.getId()));
            odb.delete(odb.getObjects(userQuery).getFirst());
            odb.commit();
            return true;
        } catch (ODBRuntimeException e){
            odb.rollback();
            return false;
        } finally {
            close();
        }
    }




    ///////////////
    // Schedules //
    ///////////////

    public List<Schedule> schedulesGet(Date date) {
        //Get schedules for a specific date

        open();
        IQuery query = new CriteriaQuery(Schedule.class, new And().add(Where.equal("date", date)));
        Objects<Schedule> schedules = odb.getObjects(query);
        close();
        return new ArrayList<>(schedules);
    }

    public List<Schedule> schedulesGet(Date from, Date to) {
        //Get schedules between two dates

        open();
        IQuery query = new CriteriaQuery(Schedule.class, new And().add(Where.ge("date", from))
                                                                  .add(Where.le("date", to)));
        query.orderByAsc("date");
        Objects<Schedule> schedules = odb.getObjects(query);
        close();
        return new ArrayList<>(schedules);
    }

    public List<Schedule> schedulesGet(String ref) {
        //Get user schedules by ref token

        open();
        IQuery query = new CriteriaQuery(Schedule.class, Where.equal("ref", ref));
        query.orderByAsc("date");
        Objects<Schedule> schedules = odb.getObjects(query);
        close();
        return new ArrayList<>(schedules);
    }

    public boolean scheduleAdd(User user, Schedule schedule){
        //Add one schedule

        try {
            open();

            //Load the user to whom the schedule belongs
            IQuery query = new CriteriaQuery(User.class, Where.equal("id", user.getId()));
            User nuser = (User) odb.getObjects(query).getFirst();

            //Check that user exists and does't contain this schedule date
            if (nuser == null || nuser.getSchedules().contains(schedule)) return false;

            //Get schedules ordered by ID (greatest first)
            query = new CriteriaQuery(Schedule.class);
            query.orderByDesc("id");
            Objects<Schedule> schs = odb.getObjects(query);

            //If there is no schedule, the new schedule ID is 1; else, is the last inserted schedule ID + 1
            int id = (schs.isEmpty())? 1 : schs.getFirst().getId() + 1;
            schedule.setId(id);
            nuser.addSchedule(schedule);

            odb.store(nuser);
            odb.commit();
            return true;
        } catch (Exception e){
            return false;
        } finally {
            close();
        }
    }

    public boolean schedulesAdd(User user, List<Schedule> schedules){
        //Add a list of schedules

        try {
            open();

            //Load the user to whom the schedule belongs
            IQuery query = new CriteriaQuery(User.class, Where.equal("id", user.getId()));
            User nuser = (User) odb.getObjects(query).getFirst();

            //Check that user exists
            if (nuser == null) return false;

            //Get schedules ordered by ID (greatest first)
            query = new CriteriaQuery(Schedule.class);
            query.orderByDesc("id");
            Objects<Schedule> schs = odb.getObjects(query);

            //If there is no schedule, the new schedule ID is 1; else, is the last inserted schedule ID + 1
            int id = (schs.isEmpty())? 1 : schs.getFirst().getId() + 1;

            boolean any_inserted = false;

            //Apply the new ID to each schedule and insert it into the user schedules list
            for (Schedule schedule : schedules) {

                //Check that the schedule date doesn't exist
                if (!nuser.getSchedules().contains(schedule)) {
                    any_inserted = true;
                    schedule.setId(id);
                    nuser.addSchedule(schedule);
                    id++;
                }
            }

            if (any_inserted) {
                odb.store(nuser);
                odb.commit();
                return true;
            } else return false;
        } catch (Exception  e){
            return false;
        } finally {
            close();
        }
    }

    public boolean scheduleTick(Schedule schedule, Schedule.Event event){
        //Set/unset schedule event

        try {
            open();

            //Load schedule, apply changes and save
            IQuery query = new CriteriaQuery(Schedule.class, Where.equal("id", schedule.getId()));
            Schedule nschedule = (Schedule) odb.getObjects(query).getFirst();

            //Check that schedule exists
            if (nschedule == null) return false;

            nschedule.setEvent(event);

            odb.store(nschedule);
            odb.commit();
            return true;
        } catch (ODBRuntimeException e){
            return false;
        } finally {
            close();
        }
    }

    public boolean scheduleDel(Schedule schedule){
        //Delete schedule by ID

        try {
            open();
            IQuery query = new CriteriaQuery(Schedule.class, Where.equal("id", schedule.getId()));
            odb.delete(odb.getObjects(query).getFirst());
            odb.commit();
            return true;
        } catch (ODBRuntimeException e){
            return false;
        } finally {
            close();
        }
    }

    public boolean schedulesDel(String ref){
        //Delete schedules by ref token

        try {
            open();
            IQuery query = new CriteriaQuery(Schedule.class, Where.equal("ref", ref));
            Objects<Schedule> schedules = odb.getObjects(query);
            for (Schedule sch : schedules){
                odb.delete(sch);
            }
            odb.commit();
            return true;
        } catch (ODBRuntimeException e){
            odb.rollback();
            return false;
        } finally {
            close();
        }
    }
}
