package tk.rktdev.chasc;

public class User {
    private String id;
    private String account;
    private String name;
    private String surname;
    private String nfc;

    public User(String account, String name, String surname, String nfc) {
        this.account = account;
        this.name = name;
        this.surname = surname;
        this.nfc = nfc;
    }

    public User(String id, String account, String name, String surname, String nfc) {
        this.id = id;
        this.account = account;
        this.name = name;
        this.surname = surname;
        this.nfc = nfc;
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

    @Override
    public String toString() {
        return surname + ", " + name;
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof User) {
            User user = (User) obj;
            return this.id.equals(user.getId());
        }
        else {
            return false;
        }
    }
}
