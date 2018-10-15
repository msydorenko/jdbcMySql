package entity;

public class Group {
    private int id;
    private String name;

    public Group() {
    }

    public Group(String name) {
        this.name = name;
    }

    public static Group createGroup(String name) {
        return new Group(name);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
