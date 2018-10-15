package entity;

public class User {
    private int id;
    private String login;

    public User() {
        super();
    }

    public User(String login) {
        super();
        this.login = login;
    }

    public static User createUser(String login) {
        return new User(login);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    @Override
    public String toString() {
        return login;
    }

}
