package replica3.model;

public enum UserType {
    ADMIN("Admin", "A"),
    PATIENT("Patient", "P");

    private final String _userTypeName;
    private final String _userTypeCode;
    UserType(final String userTypeName, final String userTypeCode) {
        _userTypeName = userTypeName;
        _userTypeCode = userTypeCode;
    }

    public String getUserTypeName() {
        return _userTypeName;
    }

    public String getUserTypeCode() {
        return _userTypeCode;
    }

    public static UserType findUserType(String userTypeCode) {
        for(UserType userType : UserType.values()) {
            if (userType._userTypeCode.equalsIgnoreCase(userTypeCode)) {
                return userType;
            }
        }

        return null;
    }
}
