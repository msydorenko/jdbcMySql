package db;

import entity.Group;
import entity.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DBManager {
    private static DBManager instance;
    private static final String URL = "jdbc:mysql://localhost:3306/practice8"
            + "?serverTimezone=UTC&useSSL=false&useUnicode=true&characterEncoding=utf8";
    private static final String USER = "root";
    private static final String PASSWORD = "postgres";

    private static final String SQL_CREATE_NEW_USER = "INSERT INTO users VALUES (DEFAULT, ?)";
    private static final String SQL_READ_ALL_USERS = "SELECT id, login FROM users";
    private static final String SQL_READ_USER_BY_LOGIN = "SELECT id, login FROM users WHERE login=?";

    private static final String SQL_CREATE_NEW_GROUP = "INSERT INTO groups VALUES (DEFAULT, ?)";
    private static final String SQL_READ_ALL_GROUPS = "SELECT id, name FROM groups";
    private static final String SQL_READ_GROUP_BY_NAME = "SELECT id, name FROM groups WHERE name=?";
    private static final String SQL_DELETE_GROUP = "DELETE FROM groups WHERE id=?";
    private static final String SQL_UPDATE_GROUPS = "UPDATE groups SET name = ? WHERE id = ?";

    private static final String SQL_CREATE_USER_GROUP_IN_USERS_GROUP_TABLE = "INSERT INTO users_groups VALUES (?,?)";
    private static final String SQL_READ_GROUP_BY_USER = "SELECT id, name FROM groups WHERE id IN " +
            "(SELECT group_id FROM users_groups WHERE user_id=?)";

    private DBManager() {
    }

    public static synchronized DBManager getInstance() {
        if (instance == null) {
            instance = new DBManager();
        }
        return instance;

    }

    public Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
        return connection;
    }

    public boolean insertUser(User user) {
        boolean result = false;
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(SQL_CREATE_NEW_USER,
                     Statement.RETURN_GENERATED_KEYS)) {
            ResultSet resultSet = null;
            int k = 1;
            preparedStatement.setString(k++, user.getLogin());
            if (preparedStatement.executeUpdate() > 0) {
                resultSet = preparedStatement.getGeneratedKeys();
                while (resultSet.next()) {
                    user.setId(resultSet.getInt(1));
                    result = true;
                }
            }
            close(resultSet);
        } catch (Exception e) {
            throw new RuntimeException("Cannot insert user ", e);
        }
        return result;
    }

    public List<User> findAllUsers() {
        List<User> users = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(SQL_READ_ALL_USERS);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                users.add(extractUser(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Cannot obtain list of users", e);
        }
        return users;
    }


    private User extractUser(ResultSet resultSet) {
        User user = new User();
        try {
            user.setId(resultSet.getInt("id"));
            user.setLogin(resultSet.getString("login"));
        } catch (SQLException e) {
            throw new RuntimeException("Cannot extract user ", e);
        }
        return user;
    }

    public boolean insertGroup(Group group) {
        boolean flag = false;
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(SQL_CREATE_NEW_GROUP)) {
            int k = 1;
            preparedStatement.setString(k++, group.getName());
            preparedStatement.addBatch();
            int[] countUpdateData = preparedStatement.executeBatch();
            if (countUpdateData.length > 0) {
                flag = true;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Cannot insert new group ", e);
        }

        return flag;
    }

    public List<Group> findAllGroups() {
        List<Group> groups = new ArrayList<>();
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(SQL_READ_ALL_GROUPS)) {
            while (resultSet.next()) {
                groups.add(extractGroup(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Cannot obtain list of groups ", e);
        }

        return groups;
    }

    private Group extractGroup(ResultSet rs) {
        Group group = new Group();
        try {
            group.setId(rs.getInt("id"));
            group.setName(rs.getString("name"));
        } catch (SQLException e) {
            throw new RuntimeException("Cannot obtain data from table groups", e);
        }
        return group;
    }

    public User getUser(String login) {
        User user = new User();
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(SQL_READ_USER_BY_LOGIN)
        ) {
            int k = 1;
            preparedStatement.setString(k++, login);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                user = extractUser(resultSet);
            }
            close(resultSet);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot obtain user from table by login ", e);
        }
        return user;
    }

    public Group getGroup(String name) {
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(SQL_READ_GROUP_BY_NAME)
        ) {
            int k = 1;
            preparedStatement.setString(k++, name);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                return extractGroup(resultSet);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Cannot obtain group by name ", e);
        }
        return null;
    }

    public void close(AutoCloseable ac) {
        if (ac != null) {
            try {
                ac.close();
            } catch (Exception e) {
                throw new IllegalStateException("Cannot close " + ac);
            }
        }
    }

    public boolean setGroupsForUser(User user, Group group) {
        List<Group> groups = new ArrayList<>();
        groups.add(group);
        return setUserInGroups(user, groups);
    }

    public boolean setGroupsForUser(User user, Group group1, Group group2) {
        List<Group> groups = new ArrayList<>();
        groups.add(group1);
        groups.add(group2);
        return setUserInGroups(user, groups);
    }

    public boolean setGroupsForUser(User user, Group group1, Group group2, Group group3) {
        List<Group> groups = new ArrayList<>();
        groups.add(group1);
        groups.add(group2);
        groups.add(group3);
        return setUserInGroups(user, groups);
    }

    private boolean setUserInGroups(User user, List<Group> groups) {
        boolean flag = false;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {
            connection = getConnection();
            connection.setAutoCommit(false);
            preparedStatement = connection.prepareStatement(SQL_CREATE_USER_GROUP_IN_USERS_GROUP_TABLE);
            for (Group group : groups) {
                preparedStatement.setInt(1, user.getId());
                preparedStatement.setInt(2, group.getId());
                preparedStatement.addBatch();
            }
            int[] countChangedRow = preparedStatement.executeBatch();
            if (countChangedRow[0] > 0) {
                flag = true;
                connection.commit();
            }
            close(preparedStatement);
            close(connection);
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
                throw new RuntimeException("Cannot rollback transaction dependency one user - one group ", e1);
            }
            throw new RuntimeException("Cannot set groups for user ", e);
        }
        return flag;
    }

    public List<Group> getUserGroups(User user) {
        List<Group> groups = new ArrayList<>();
        int id = user.getId();
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(SQL_READ_GROUP_BY_USER)) {
            int k = 1;
            preparedStatement.setInt(k, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                groups.add(extractGroup(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Cannot obtain groups for user ", e);
        }
        return groups;
    }

    public boolean deleteGroup(Group group) {
        boolean flag = false;
        int id = group.getId();
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(SQL_DELETE_GROUP)) {
            int k = 1;
            preparedStatement.setInt(k, id);
            if (preparedStatement.executeUpdate() > 0) {
                flag = true;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Cannot delete group " + group.getName(), e);
        }
        return flag;
    }

    public boolean updateGroup(Group group) {
        boolean flag = false;
        int id = group.getId();
        String name = group.getName();
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(SQL_UPDATE_GROUPS)) {
            int k = 1;
            preparedStatement.setString(k++, name);
            preparedStatement.setInt(k++, id);
            if (preparedStatement.executeUpdate() > 0) {
                flag = true;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Cannot update table groups ", e);
        }
        return flag;
    }
}
