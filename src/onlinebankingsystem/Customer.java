package onlinebankingsystem;

public class Customer {
    private Long id;
    private String firstName;
    private String lastName;
    private int phone;
    private String uid;
    private String password;


    // Constructor
    public Customer(Long id, String firstName, String lastName, int phone, String uid, String password) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.uid = uid;
        this.password = password;
    }


    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public int getPhone() { return phone; }
    public void setPhone(int phone) { this.phone = phone; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }


    public String getFullName() {
        return firstName + " " + lastName;
    }
}