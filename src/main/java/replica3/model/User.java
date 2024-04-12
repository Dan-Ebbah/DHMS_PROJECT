package replica3.model;

public class User {
    private HospitalType _hospitalType;

    private UserType _userType;


    private String _userId;

    public User(HospitalType hospitalType, UserType userType) {
        _hospitalType = hospitalType;
        _userType = userType;
    }

    public User(String userId) {
        _userId = userId;
        _hospitalType = HospitalType.findHospital(userId.substring(0,3));
        _userType = UserType.findUserType(userId.substring(3, 4));
    }

    public HospitalType getHospitalType() {
        return _hospitalType;
    }

    public boolean isAdmin() {
        return "A".equalsIgnoreCase(_userType.getUserTypeCode());
    }

    public String getUserId() {
        return _userId;
    }
}
