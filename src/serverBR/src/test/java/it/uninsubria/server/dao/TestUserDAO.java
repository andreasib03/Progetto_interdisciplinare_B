package it.uninsubria.server.dao;

import it.uninsubria.shared.model.User;

public class TestUserDAO implements UserDAO {
    private int registerUserCalls = 0;
    private int loginCalls = 0;
    private int phrasePasswordCalls = 0;
    private int getUserByUsernameOrEmailCalls = 0;
    private int updateProfileImageCalls = 0;
    private int updateProfileInfoCalls = 0;
    private int verifyRecoveryPhraseCalls = 0;
    private int resetPasswordCalls = 0;
    @Override
    public boolean registerUser(User utente) {
        registerUserCalls++;
        return true;
    }

    @Override
    public boolean login(String usernameOrEmail, String password) {
        loginCalls++;
        return true;
    }

    @Override
    public boolean phrasePassword(String email, String phrase) {
        phrasePasswordCalls++;
        return true;
    }

    @Override
    public User getUserByUsernameOrEmail(String usernameOrEmail) {
        getUserByUsernameOrEmailCalls++;
        // Use a valid User constructor (id, name, surname, email, CF)
        return User.builder().id(usernameOrEmail).name("TestName").surname("TestSurname").email(usernameOrEmail).CF("TESTCF").build(); // simplistic
    }

    @Override
    public boolean updateProfileImage(User user, byte[] imageBytes, String mimeType) {
        updateProfileImageCalls++;
        return true;
    }

    @Override
    public boolean updateProfileInfo(User user) throws Exception {
        updateProfileInfoCalls++;
        return true;
    }

    public boolean getPasswordByUserID(String userID, String outPassword) {
        // In tests this can be ignored; simulate always successful verification
        // Note: outPassword is a parameter; in tests it's not mutable here.
        return true;
    }

    @Override
    public boolean updatePassword(String userID, String newPassword) {
        // Track usage if needed in tests; here just simulate success
        return true;
    }

    @Override
    public boolean deleteUser(String userID) {
        // Simulate successful deletion
        return true;
    }

    // Getters for tests
    public int getRegisterUserCalls() { return registerUserCalls; }
    public int getLoginCalls() { return loginCalls; }
    public int getPhrasePasswordCalls() { return phrasePasswordCalls; }
    public int getGetUserByUsernameOrEmailCalls() { return getUserByUsernameOrEmailCalls; }
    public int getUpdateProfileImageCalls() { return updateProfileImageCalls; }
    public int getUpdateProfileInfoCalls() { return updateProfileInfoCalls; }
    public int getVerifyRecoveryPhraseCalls() { return verifyRecoveryPhraseCalls; }
    public int getResetPasswordCalls() { return resetPasswordCalls; }

    @Override
    public boolean verifyRecoveryPhrase(String email, String hashedPhrase) {
        // Simple test implementation - verify phrase matches stored value
        verifyRecoveryPhraseCalls++;
        if ("test@example.com".equals(email) && "testHashedPhrase".equals(hashedPhrase)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean resetPassword(String email, String newPassword) {
        // Simple test implementation - simulate password reset
        resetPasswordCalls++;
        if ("test@example.com".equals(email) && newPassword != null && !newPassword.isEmpty()) {
            return true;
        }
        return false;
    }
}
