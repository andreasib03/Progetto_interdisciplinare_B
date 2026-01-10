package it.uninsubria.server.dao.impl;
import it.uninsubria.server.dao.UserDAO;
import it.uninsubria.server.util.PasswordHashUtil;
import it.uninsubria.server.util.InputValidator;
import it.uninsubria.server.service.PasswordService;
import it.uninsubria.shared.exception.ValidationException;
import it.uninsubria.shared.exception.DatabaseException;
import it.uninsubria.server.ServerMessageManager;
import it.uninsubria.shared.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.sql.DataSource;

public class UserDAOImpl implements UserDAO{
    private static final Logger logger = Logger.getLogger(UserDAOImpl.class.getName());
    private final Connection conn;
    private final DataSource ds;
    private final PasswordService passwordService;

    public UserDAOImpl(Connection conn) {
        this.conn = conn;
        this.ds = null;
        this.passwordService = new PasswordService();
    }

    public UserDAOImpl(DataSource ds) {
        this.conn = null;
        this.ds = ds;
        this.passwordService = new PasswordService();
    }

    private Connection getConnection() throws SQLException {
        if (conn != null) return conn;
        if (ds != null) return ds.getConnection();
        throw new SQLException("No DataSource or Connection configured for UserDAOImpl");
    }

    public boolean updateProfileImage(User user, byte[] imageBytes, String mimeType) {
        String sql = "UPDATE Users SET profile_image = ?, profile_image_type = ? WHERE userid = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBytes(1, imageBytes);
            ps.setString(2, mimeType);
            ps.setString(3, user.getID());
            return ps.executeUpdate() > 0;
        } catch(SQLException e){
            logger.log(Level.SEVERE, "Error updating profile image for user: " + user.getID(), e);
            throw new DatabaseException("Failed to update profile image for user: " + user.getID(), e);
        }
    }

    @Override
    public boolean updateProfileInfo(User user) throws Exception {
        String sql = "UPDATE Users SET names = ?, surnames = ? WHERE userid = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getSurname());
            ps.setString(3, user.getID());
            return ps.executeUpdate() > 0;
        } catch(SQLException e){
            logger.log(Level.SEVERE, "Database error updating profile info for user: " + user.getID(), e);
            throw new SQLException(ServerMessageManager.getString("server.error.profile.update"), e);
        }
    }

    @Override
    public User getUserByUsernameOrEmail(String usernameOrEmail){
        User user = null;
        String sql = "SELECT * FROM users WHERE userid = ? OR email = ?";

        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, usernameOrEmail);
            ps.setString(2, usernameOrEmail);

            try (ResultSet rs = ps.executeQuery()) {
                if(rs.next()){
                    user = new User();
                    user.setUser_id(rs.getInt("user_id"));
                    user.setID(rs.getString("userid"));
                    user.setName(rs.getString("names"));
                    user.setSurname(rs.getString("surnames"));
                    user.setEmail(rs.getString("email"));
                    user.setCF(rs.getString("cf"));
                    user.setRecoveryPhrase(rs.getString("passwordRecovery"));
                    user.setProfileImage(rs.getBytes("profile_image"));
                    user.setProfileImageType(rs.getString("profile_image_type"));
                }
            }
        }catch(SQLException e){
            logger.log(Level.SEVERE, "SQL error in getUserByUsernameOrEmail for: " + usernameOrEmail, e);
            throw new DatabaseException("Failed to retrieve user: " + usernameOrEmail, e);
        }
        return user;

    }

    @Override
    public boolean registerUser(User u){
        String sql = "INSERT INTO users (names, surnames, cf, email, userid, passwords, passwordRecovery) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            // Validate input using centralized validator
            try {
                InputValidator.validateUserRegistration(u);
            } catch (ValidationException e) {
                logger.warning("Validation failed for user registration: " + e.getMessage());
                throw new DatabaseException("User registration validation failed: " + e.getMessage(), e);
            }
            String hashedPassword = passwordService.hashPassword(u.getPassword());
            ps.setString(1, u.getName());
            ps.setString(2, u.getSurname());
            ps.setString(3, u.getCF());
            ps.setString(4, u.getEmail());
            ps.setString(5, u.getID());
            ps.setString(6, hashedPassword);
            ps.setString(7, u.getRecoveryPhrase()); // Frase di recupero già hashata dal client

            logger.info("Attempting to register user: " + u.getID());
            int rows = ps.executeUpdate();
            logger.info("User registration completed, rows affected: " + rows);
            return rows == 1;

        }catch(SQLException e){
            logger.log(Level.SEVERE, "Error during user registration for: " + u.getID(), e);
            throw new DatabaseException("Failed to register user: " + u.getID(), e);
        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "Invalid password format during registration", e);
            throw new DatabaseException("Invalid password format for user: " + u.getID(), e);
        }
    }

    @Override
    public boolean login(String usernameOrEmail, String password){
        try {
            InputValidator.validateUsername(usernameOrEmail);
            InputValidator.validatePassword(password);
        } catch (ValidationException e) {
            logger.warning("Login validation failed: " + e.getMessage());
            return false;
        }

        String getUserSql = "SELECT passwords FROM users WHERE userid = ? OR email = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(getUserSql)) {
            ps.setString(1, usernameOrEmail);
            ps.setString(2, usernameOrEmail);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("passwords");
                    boolean isValid = passwordService.verifyPassword(password, storedPassword);
                    if (isValid) {
                        logger.info("Successful login attempt for: " + usernameOrEmail);
                    } else {
                        logger.warning("Failed login attempt for: " + usernameOrEmail);
                    }
                    return isValid;
                } else {
                    logger.warning("Login attempt for non-existent user: " + usernameOrEmail);
                    return false;
                }
            }
        }catch(SQLException e){
            logger.log(Level.SEVERE, "Error during login attempt for: " + usernameOrEmail, e);
            return false;
        }
    }

    @Override
    public boolean phrasePassword(String email, String phrase){
        String sql = "SELECT * FROM Users WHERE email = ? AND phrase = ?";
        try(Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)){
            try {
                InputValidator.validateEmail(email);
                if (phrase == null || phrase.trim().isEmpty()) {
                    throw new ValidationException("Recovery phrase cannot be empty");
                }
            } catch (ValidationException e) {
                logger.warning("Phrase password recovery validation failed: " + e.getMessage());
                return false;
            }
            ps.setString(1, email.trim());
            ps.setString(2, phrase.trim());
            ResultSet rs = ps.executeQuery();
            boolean result = rs.next();
            if (result) {
                logger.info("Phrase password recovery successful for email: " + email);
            } else {
                logger.warning("Phrase password recovery failed for email: " + email);
            }
            return result;
        }catch(SQLException e){
            logger.log(Level.SEVERE, "Error during phrase password recovery", e);
            return false;
        }
    }

    public boolean getPasswordByUserID(String userID, String inputPassword){
        String sql = "SELECT passwords FROM Users WHERE userid = ?";

        try(Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)){
            ps.setString(1, userID);
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                String storedPassword = rs.getString("passwords");
                return passwordService.verifyPassword(inputPassword, storedPassword);
            }
            
            return false;
            
        }catch(SQLException e){
            logger.log(Level.SEVERE, "Error retrieving password for userID: " + userID, e);
            return false;
        }
    }
    public boolean updatePassword(String userID, String newPassword) {
        String sql = "UPDATE Users SET passwords = ? WHERE userid = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            String hashed = PasswordHashUtil.hashPassword(newPassword);
            ps.setString(1, hashed);
            ps.setString(2, userID);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating password for userID: " + userID, e);
            return false;
        }
    }

    private boolean updatePasswordByUsernameOrEmail(String usernameOrEmail, String hashedPassword) {
        String sql = "UPDATE Users SET passwords = ? WHERE userid = ? OR email = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, hashedPassword);
            ps.setString(2, usernameOrEmail);
            ps.setString(3, usernameOrEmail);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating password for: " + usernameOrEmail, e);
            return false;
        }
    }

    public boolean deleteUser(String userID) {
        String sql = "DELETE FROM Users WHERE userid = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userID);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error deleting userID: " + userID, e);
            return false;
        }
    }

    @Override
    public boolean verifyRecoveryPhrase(String email, String hashedPhrase) {
        String sql = "SELECT passwordRecovery FROM Users WHERE email = ?";
        try (Connection c = getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedPhrase = rs.getString("passwordRecovery");
                    return storedPhrase != null && storedPhrase.equals(hashedPhrase);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error verifying recovery phrase for email: " + email, e);
        }
        return false;
    }

    @Override
    public boolean resetPassword(String email, String newPassword) {
        String hashedPassword = passwordService.hashPassword(newPassword);
        return updatePasswordByUsernameOrEmail(email, hashedPassword);
    }
}
