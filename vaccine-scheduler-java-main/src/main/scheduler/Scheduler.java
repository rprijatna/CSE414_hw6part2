package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1)
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1)
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2)
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2)
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        // TODO: Part 1
        // create_patient <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Create patient failed");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the patient
        try {
            Patient patient = new Patient.PatientBuilder(username, salt, hash).build();
            // save to patient information to our database
            patient.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Create patient failed");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            Caregiver caregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build(); 
            // save to caregiver information to our database
            caregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        // TODO: Part 1
        // login_patient <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in, try again");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login patient failed");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login patient failed");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Login patient failed");
        } else {
            System.out.println("Logged in as " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        // TODO: Part 2                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        // check if a user is already logged in
       if(currentCaregiver == null && currentPatient == null) {
           System.out.println("Please login first");
           return;
       }
       if(tokens.length != 2) {
           System.out.println("Please try again");
           return;
       }
       String date = tokens[1];
       try {
           Date d = Date.valueOf(date);

            String getCaregiversQuery = "SELECT Username FROM Availabilities WHERE Time = ? ORDER BY Username";
            PreparedStatement caregiversStmt = con.prepareStatement(getCaregiversQuery);
            caregiversStmt.setDate(1, d);
            ResultSet caregiversRS = caregiversStmt.executeQuery();

            boolean caregiversFound = false;
            while (caregiversRS.next()) {
            caregiversFound = true;
            System.out.println(caregiversRS.getString("Username"));
            }
            if (!caregiversFound) {
                System.out.println("No caregivers available.");
            }
    
            // Retrieve vaccine details
            String getVaccinesQuery = "SELECT Name, Doses FROM Vaccines";
            PreparedStatement vaccinesStmt = con.prepareStatement(getVaccinesQuery);
            ResultSet vaccinesRS = vaccinesStmt.executeQuery();
    
            while (vaccinesRS.next()) {
                String vaccineName = vaccinesRS.getString("Name");
                int dosesAvailable = vaccinesRS.getInt("Doses");
                System.out.println(vaccineName + " " + dosesAvailable);
            }
       } catch (SQLException e) {
           System.out.println("Please try again");
           e.printStackTrace();
       } finally {
           cm.closeConnection();
       }
    }

    private static void reserve(String[] tokens) {
        // TODO: Part 2
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first");
            return;
        }
        if (currentCaregiver != null) {
            System.out.println("Please login as a patient");
            return;
        }
        if (tokens.length != 3) {
            System.out.println("Please try again");
            return;
        }

        String date = tokens[1];
        String vaccine = tokens[2];

        try {
            Date d = Date.valueOf(date);

            // Check vaccine availability
            String vaccineQuery = "SELECT Doses FROM Vaccines WHERE Name = ?";
            PreparedStatement vaccineStmt = con.prepareStatement(vaccineQuery);
            vaccineStmt.setString(1, vaccine);
            ResultSet vaccineRS = vaccineStmt.executeQuery();

            if (!vaccineRS.next() || vaccineRS.getInt("Doses") <= 0) {
                System.out.println("Not enough available doses");
                return;
            }

            // Find an available caregiver
            String caregiverQuery = "SELECT Username FROM Availabilities WHERE Time = ? ORDER BY Username";
            PreparedStatement caregiverStmt = con.prepareStatement(caregiverQuery);
            caregiverStmt.setDate(1, d);
            ResultSet caregiverRS = caregiverStmt.executeQuery();

            if (!caregiverRS.next()) {
                System.out.println("No caregiver is available");
                return;
            }

            String caregiverUsername = caregiverRS.getString("Username");
            int appointmentId = getAppointmentId();

            try {
                // Reserve the appointment
                //get appointment ID and addinto insert query + 1
                String insertAppointmentQuery = "INSERT INTO Appointments (Patient_name, Caregiver_name, Vaccine_name, Time, id) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement appointmentStmt = con.prepareStatement(insertAppointmentQuery, Statement.RETURN_GENERATED_KEYS);
                appointmentStmt.setString(1, currentPatient.getUsername());
                appointmentStmt.setString(2, caregiverUsername);
                appointmentStmt.setString(3, vaccine);
                appointmentStmt.setDate(4, d);
                appointmentStmt.setInt(5, appointmentId);
                appointmentStmt.executeUpdate();

                // Reduce vaccine doses
                String updateVaccineQuery = "UPDATE Vaccines SET Doses = Doses - 1 WHERE Name = ?";
                PreparedStatement updateVaccineStmt = con.prepareStatement(updateVaccineQuery);
                updateVaccineStmt.setString(1, vaccine);
                updateVaccineStmt.executeUpdate();

                // Remove caregiver availability for the date
                String deleteAvailabilityQuery = "DELETE FROM Availabilities WHERE Username = ? AND Time = ?";
                PreparedStatement deleteAvailabilityStmt = con.prepareStatement(deleteAvailabilityQuery);
                deleteAvailabilityStmt.setString(1, caregiverUsername);
                deleteAvailabilityStmt.setDate(2, d);
                deleteAvailabilityStmt.executeUpdate();

                // Commit transaction
                con.commit();

                // Output success message
                System.out.println("Appointment ID " + appointmentId + ", Caregiver username " + caregiverUsername);

            } catch (SQLException e) {
                // Rollback in case of any failure
                con.rollback();
                System.out.println("Please try again");
                e.printStackTrace();
            } finally {
                cm.closeConnection();
            }
        } catch (SQLException e) {
            System.out.println("Please try again");
            e.printStackTrace();
        }
    }

    private static int getAppointmentId() {
        String query = "SELECT MAX(id) FROM Appointments";
        int appointmentId = 1;

        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        try {
            PreparedStatement stmt = con.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();

            if(rs.next()) {
                if(!rs.wasNull()) {
                    appointmentId = rs.getInt(1) + 1;
                }
            }

        } catch (SQLException e) {
            System.out.println("Please try again");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return appointmentId;
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        // TODO: Extra credit
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        // check if a user is already logged in
        if(currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first");
            return;
        }
        if(tokens.length != 2) {
            System.out.println("Please try again");
            return;
        }
        int appointmentId = Integer.parseInt(tokens[1]);

        try {
            String query = "SELECT Time, Caregiver_name, Patient_name, Vaccine_name FROM Appointments WHERE id = ?";
            PreparedStatement stmt = con.prepareStatement(query);
            stmt.setInt(1, appointmentId);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                System.out.println("Appointment ID does not exist");
                return;
            }

            Date appointmentTime = rs.getDate("Time");
            String caregiverName = rs.getString("Caregiver_name");
            String patientName = rs.getString("Patient_name");
            String vaccineName = rs.getString("Vaccine_name");

            if (currentCaregiver != null && !currentCaregiver.getUsername().equals(caregiverName)) {
                System.out.println("You are not authorized to cancel this appointment");
                return;
            }
            if (currentPatient != null && !currentPatient.getUsername().equals(patientName)) {
                System.out.println("You are not authorized to cancel this appointment");
                return;
            }

            //delete the appointment
            String deleteAppointmentQuery = "DELETE FROM Appointments WHERE id = ?";
            PreparedStatement deleteStmt = con.prepareStatement(deleteAppointmentQuery);
            deleteStmt.setInt(1, appointmentId);
            deleteStmt.executeUpdate();

            //restore caregiver availability
            String restoreAvailabilityQuery = "INSERT INTO Availabilities (Time, Username) VALUES (?, ?)";
            PreparedStatement restoreStmt = con.prepareStatement(restoreAvailabilityQuery);
            restoreStmt.setDate(1, appointmentTime);
            restoreStmt.setString(2, caregiverName);
            restoreStmt.executeUpdate();

            //add back vaccine dose
            String incrementVaccineQuery = "UPDATE Vaccines SET Doses = Doses + 1 WHERE Name = ?";
            PreparedStatement incrementStmt = con.prepareStatement(incrementVaccineQuery);
            incrementStmt.setString(1, vaccineName);
            incrementStmt.executeUpdate();

            System.out.println("Appointment " + appointmentId + " has been successfully canceled");
        } catch (SQLException e) {
            System.out.println("Please try again");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        // TODO: Part 2
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        if(currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first");
            return;
        }
        if(tokens.length != 1) {
            System.out.println("Please try again");
            return;
        }
        try {
            String query = "";
            if (currentPatient != null) {
                // For patients: appointment ID, vaccine name, date, and caregiver name
                query = "SELECT id, Vaccine_name, Time, Caregiver_name FROM Appointments WHERE Patient_name = ? ORDER BY id";
            } else {
                // For caregivers: appointment ID, vaccine name, date, and patient name
                query = "SELECT id, Vaccine_name, Time, Patient_name FROM Appointments WHERE Caregiver_name = ? ORDER BY id";
            }
            PreparedStatement stmt = con.prepareStatement(query);
            if(currentPatient != null) {
                stmt.setString(1, currentPatient.getUsername());
            } else {
                stmt.setString(1, currentCaregiver.getUsername());
            }

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int appointmentId = rs.getInt("id");
                String vaccineName = rs.getString("Vaccine_name");
                Date date = rs.getDate("Time");
                String otherUsername = (currentPatient != null) ? rs.getString("Caregiver_name") : rs.getString("Patient_name");

                System.out.println(appointmentId + " " + vaccineName + " " + date + " " + otherUsername);
            }
        } catch (SQLException e) {
            System.out.println("Please try again");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void logout(String[] tokens) {
        // TODO: Part 2
        if(tokens.length != 1) {
            System.out.println("Please try again");
            return;
        }
        if(currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first");
            return;
        }
        try {
            currentCaregiver = null;
            currentPatient = null;
            System.out.println("Successfully logged out");
        } catch (Exception e) {
            System.out.println("Please try again");
            e.printStackTrace();
        }
    }
}
