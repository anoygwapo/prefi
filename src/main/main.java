package main;

import config.config;
import java.util.Scanner;
import java.util.List;
import java.util.Map;

public class main {

    public static void viewPatients(config db) {
        String qry = "SELECT * FROM patient_table";
        String[] headers = {"Patient ID", "Full Name", "Date of Birth", "Gender", "Address", "Contact"};
        String[] cols = {"p_id", "p_fullname", "p_dob", "p_gender", "p_address", "p_contact"};
        db.viewRecords(qry, headers, cols);
    }

    public static void viewDoctors(config db) {
        String qry = "SELECT * FROM tbl_user WHERE u_type = 'Doctor'";
        String[] headers = {"Doctor ID", "Name", "Email", "Status"};
        String[] cols = {"u_id", "u_name", "u_email", "u_status"};
        db.viewRecords(qry, headers, cols);
    }

    public static void viewAppointments(config db) {
        String qry = "SELECT * FROM appointment_table";
        String[] headers = {"Appointment ID", "Patient ID", "Doctor ID", "Date", "Reason"};
        String[] cols = {"a_id", "p_id", "d_id", "a_date", "a_reason"};
        db.viewRecords(qry, headers, cols);
    }

    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        config db = new config();
        db.connectDB();

        int choice;
        char cont;

        do {
            System.out.println("\n===== HOSPITAL INFORMATION SYSTEM =====");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("3. Exit");
            System.out.print("Enter choice: ");
            choice = scan.nextInt();
            scan.nextLine(); // Clear buffer

            switch (choice) {
                case 1:
                    System.out.print("Enter Email: ");
                    String em = scan.nextLine().trim();
                    System.out.print("Enter Password: ");
                    String pw = scan.nextLine().trim();
                    String Hashpasseds = db.hashPassword(pw);
                    String qry = "SELECT * FROM tbl_user WHERE u_email = ? AND u_pass = ?";
                    List<Map<String, Object>> res = db.fetchRecords(qry, em, Hashpasseds);

                    if (res.isEmpty()) {
                        System.out.println("❌ Invalid Credentials");
                    } else {
                        Map<String, Object> user = res.get(0);
                        String status = user.get("u_status").toString();
                        String type = user.get("u_type").toString();

                        if (status.equals("Pending")) {
                            System.out.println("⚠️ Account is still pending for Admin approval.");
                        } else {
                            System.out.println("✅ Login Successful!");
                            if (type.equals("Admin")) {
                                adminMenu(scan, db);
                            } else if (type.equals("Doctor")) {
                                doctorMenu(scan, db, user);
                            } else if (type.equals("Patient")) {
                                patientMenu(scan, db, user);
                            }
                        }
                    }
                    break;

                case 2:
                    System.out.print("Enter Name: ");
                    String name = scan.nextLine().trim();
                    
                    if (name.isEmpty()) {
                        System.out.println("❌ Name cannot be empty.");
                        break;
                    }
                    
                    System.out.print("Enter Email: ");
                    String email = scan.nextLine().trim();

                    String checkQry = "SELECT * FROM tbl_user WHERE u_email = ?";
                    List<Map<String, Object>> chk = db.fetchRecords(checkQry, email);
                    if (!chk.isEmpty()) {
                        System.out.println("❌ Email already exists.");
                        break;
                    }

                    System.out.println("Select User Type: ");
                    System.out.println("1. Admin");
                    System.out.println("2. Doctor");
                    System.out.println("3. Patient");
                    System.out.print("Choice: ");
                    int t = scan.nextInt();
                    scan.nextLine(); // Clear buffer
                    String type = (t == 1) ? "Admin" : (t == 2) ? "Doctor" : "Patient";
                    String status = (type.equals("Admin")) ? "Approved" : "Pending";

                    System.out.print("Enter Password: ");
                    String pass = scan.nextLine().trim();
                    String Hashpassed = db.hashPassword(pass);

                    String sql = "INSERT INTO tbl_user (u_name, u_email, u_type, u_status, u_pass) VALUES (?, ?, ?, ?, ?)";
                    db.addRecord(sql, name, email, type, status, Hashpassed);

                    if (type.equals("Patient")) {
                        System.out.print("Enter Date of Birth (YYYY-MM-DD): ");
                        String dob = scan.nextLine().trim();
                        System.out.print("Gender: ");
                        String gender = scan.nextLine().trim();
                        System.out.print("Address: ");
                        String address = scan.nextLine().trim();
                        System.out.print("Contact: ");
                        String contact = scan.nextLine().trim();

                        // Get the u_id of the just-inserted user by email
                        String getLastUserId = "SELECT u_id FROM tbl_user WHERE u_email = ?";
                        List<Map<String, Object>> lastUser = db.fetchRecords(getLastUserId, email);
                        
                        if (!lastUser.isEmpty()) {
                            Object u_id_obj = lastUser.get(0).get("u_id");
                            int u_id = (u_id_obj instanceof Long) ? ((Long) u_id_obj).intValue() : (Integer) u_id_obj;
                            
                            String patSql = "INSERT INTO patient_table (u_id, p_fullname, p_dob, p_gender, p_address, p_contact) VALUES (?, ?, ?, ?, ?, ?)";
                            db.addRecord(patSql, u_id, name, dob, gender, address, contact);
                            System.out.println("✅ Patient profile created successfully!");
                        } else {
                            System.out.println("❌ Error: Could not retrieve user ID.");
                        }
                    }

                    System.out.println("✅ Registration Successful. " + (status.equals("Pending") ? "Wait for Admin Approval." : "You may now log in."));
                    break;

                case 3:
                    System.out.println("Exiting System...");
                    System.exit(0);
                    break;

                default:
                    System.out.println("Invalid choice!");
            }

            System.out.print("Do you want to continue? (Y/N): ");
            cont = scan.next().charAt(0);
            scan.nextLine(); // Clear buffer

        } while (cont == 'Y' || cont == 'y');
    }

    // ================= ADMIN DASHBOARD =================
    public static void adminMenu(Scanner scan, config db) {
        int choice;
        do {
            System.out.println("\n===== ADMIN DASHBOARD =====");
            System.out.println("1. Approve User Accounts");
            System.out.println("2. View Doctors");
            System.out.println("3. View Patients");
            System.out.println("4. Manage Patients (Add/Update/Delete)");
            System.out.println("5. Logout");
            System.out.print("Enter choice: ");
            choice = scan.nextInt();
            scan.nextLine(); // Clear buffer

            switch (choice) {
                case 1:
                    System.out.println("\n=== APPROVE ACCOUNTS ===");
                    String qry = "SELECT * FROM tbl_user WHERE u_status = 'Pending'";
                    String[] hdr = {"User ID", "Name", "Email", "Type", "Status"};
                    String[] col = {"u_id", "u_name", "u_email", "u_type", "u_status"};
                    db.viewRecords(qry, hdr, col);

                    System.out.print("Enter User ID to Approve: ");
                    int id = scan.nextInt();
                    scan.nextLine(); // Clear buffer
                    String upd = "UPDATE tbl_user SET u_status = 'Approved' WHERE u_id = ?";
                    db.updateRecord(upd, id);
                    System.out.println("✅ Account Approved Successfully!");
                    break;

                case 2:
                    viewDoctors(db);
                    break;

                case 3:
                    viewPatients(db);
                    break;

                case 4:
                    managePatients(scan, db);
                    break;

                case 5:
                    System.out.println("Logging out...");
                    return;
            }
        } while (choice != 5);
    }

    // =============== MANAGE PATIENTS ===============
    public static void managePatients(Scanner scan, config db) {
        System.out.println("\n1. Add Patient");
        System.out.println("2. Update Patient");
        System.out.println("3. Delete Patient");
        int sub = scan.nextInt();
        scan.nextLine(); // Clear buffer

        if (sub == 1) {
            System.out.print("Full Name: ");
            String name = scan.nextLine().trim();
            System.out.print("DOB (YYYY-MM-DD): ");
            String dob = scan.nextLine().trim();
            System.out.print("Gender: ");
            String gender = scan.nextLine().trim();
            System.out.print("Address: ");
            String addr = scan.nextLine().trim();
            System.out.print("Contact: ");
            String contact = scan.nextLine().trim();

            // Create user account first
            String userSql = "INSERT INTO tbl_user (u_name, u_email, u_type, u_status, u_pass) VALUES (?, ?, ?, ?, ?)";
            String generatedEmail = name.replaceAll("\\s+", "").toLowerCase() + "@patient.com";
            String defaultPassHash = db.hashPassword("123");
            db.addRecord(userSql, name, generatedEmail, "Patient", "Approved", defaultPassHash);

            // Get the last inserted user ID
            String getLastUserId = "SELECT u_id FROM tbl_user WHERE u_email = ?";
            List<Map<String, Object>> lastUser = db.fetchRecords(getLastUserId, generatedEmail);
            
            if (!lastUser.isEmpty()) {
                Object u_id_obj = lastUser.get(0).get("u_id");
                int u_id = (u_id_obj instanceof Long) ? ((Long) u_id_obj).intValue() : (Integer) u_id_obj;

                // Now insert into patient_table with u_id
                String sql = "INSERT INTO patient_table (u_id, p_fullname, p_dob, p_gender, p_address, p_contact) VALUES (?, ?, ?, ?, ?, ?)";
                db.addRecord(sql, u_id, name, dob, gender, addr, contact);
                System.out.println("✅ Patient added! (Login: " + generatedEmail + " / Password: 123)");
            } else {
                System.out.println("❌ Error: Could not retrieve user ID.");
            }
        } else if (sub == 2) {
            viewPatients(db);
            System.out.print("Enter Patient ID to Update: ");
            int pid = scan.nextInt();
            scan.nextLine(); // Clear buffer
            System.out.print("New Address: ");
            String addr = scan.nextLine().trim();
            System.out.print("New Contact: ");
            String contact = scan.nextLine().trim();

            String upd = "UPDATE patient_table SET p_address = ?, p_contact = ? WHERE p_id = ?";
            db.updateRecord(upd, addr, contact, pid);
            System.out.println("✅ Patient Updated!");
        } else if (sub == 3) {
            viewPatients(db);
            System.out.print("Enter Patient ID to Delete: ");
            int pid = scan.nextInt();
            scan.nextLine(); // Clear buffer
            
            // Get u_id before deleting patient
            String getUid = "SELECT u_id FROM patient_table WHERE p_id = ?";
            List<Map<String, Object>> result = db.fetchRecords(getUid, pid);
            
            if (!result.isEmpty()) {
                Object u_id_obj = result.get(0).get("u_id");
                int u_id = (u_id_obj instanceof Long) ? ((Long) u_id_obj).intValue() : (Integer) u_id_obj;
                
                // Delete patient record
                String del = "DELETE FROM patient_table WHERE p_id = ?";
                db.deleteRecord(del, pid);
                
                // Delete user account
                String delUser = "DELETE FROM tbl_user WHERE u_id = ?";
                db.deleteRecord(delUser, u_id);
                
                System.out.println("✅ Patient and associated user account deleted!");
            } else {
                System.out.println("❌ Patient not found!");
            }
        }
    }

    public static void doctorMenu(Scanner scan, config db, Map<String, Object> user) {
        int choice;
        do {
            System.out.println("\n===== DOCTOR DASHBOARD =====");
            System.out.println("1. View All Patients");
            System.out.println("2. Create Appointment");
            System.out.println("3. View Appointments");
            System.out.println("4. Update Medical Record");
            System.out.println("5. View Medical Records");
            System.out.println("6. Logout");
            System.out.print("Enter choice: ");
            choice = scan.nextInt();
            scan.nextLine(); // Clear buffer

            switch (choice) {
                case 1:
                    viewPatients(db);
                    break;

                case 2:
                    System.out.println("=== CREATE APPOINTMENT ===");
                    viewPatients(db);
                    System.out.print("Enter Patient ID: ");
                    int pid = scan.nextInt();
                    scan.nextLine(); // Clear buffer
                    System.out.print("Enter Date (YYYY-MM-DD): ");
                    String date = scan.nextLine().trim();
                    System.out.print("Enter Reason: ");
                    String reason = scan.nextLine().trim();

                    // Create appointment
                    String sql = "INSERT INTO appointment_table (p_id, d_id, a_date, a_reason) VALUES (?, ?, ?, ?)";
                    db.addRecord(sql, pid, user.get("u_id"), date, reason);

                    // Fetch appointment ID
                    String fetchAID = "SELECT a_id FROM appointment_table WHERE p_id = ? AND d_id = ? AND a_date = ? AND a_reason = ? ORDER BY a_id DESC LIMIT 1";
                    List<Map<String, Object>> last = db.fetchRecords(fetchAID, pid, user.get("u_id"), date, reason);
                    
                    if (!last.isEmpty()) {
                        Object a_id_obj = last.get(0).get("a_id");
                        int a_id = (a_id_obj instanceof Long) ? ((Long) a_id_obj).intValue() : (Integer) a_id_obj;

                        // Create blank medical record
                        String createMed = "INSERT INTO medical_record_table (p_id, d_id, a_id, diagnosis, prescription, treatment, record_date) VALUES (?, ?, ?, ?, ?, ?, ?)";
                        db.addRecord(createMed, pid, user.get("u_id"), a_id, "", "", "", date);

                        System.out.println("✅ Appointment Created and Medical Record Initialized!");
                    }
                    break;

                case 3:
                    viewAppointments(db);
                    break;

                case 4:
                    System.out.println("=== UPDATE MEDICAL RECORD ===");
                    viewAppointments(db);
                    System.out.print("Enter Appointment ID to Add Diagnosis: ");
                    int aid = scan.nextInt();
                    scan.nextLine(); // Clear buffer
                    System.out.print("Diagnosis: ");
                    String diag = scan.nextLine().trim();
                    System.out.print("Prescription: ");
                    String pres = scan.nextLine().trim();
                    System.out.print("Treatment: ");
                    String treat = scan.nextLine().trim();

                    // Verify if appointment has a medical record
                    String check = "SELECT * FROM medical_record_table WHERE a_id = ?";
                    List<Map<String, Object>> result = db.fetchRecords(check, aid);
                    if (result.isEmpty()) {
                        System.out.println("❌ No medical record found for this Appointment ID!");
                    } else {
                        String upd = "UPDATE medical_record_table SET diagnosis = ?, prescription = ?, treatment = ? WHERE a_id = ?";
                        db.updateRecord(upd, diag, pres, treat, aid);
                        System.out.println("✅ Medical Record Updated!");
                    }
                    break;

                case 5:
                    System.out.println("=== VIEW MEDICAL RECORDS ===");
                    String q3 = "SELECT record_id, p_id, a_id, diagnosis, prescription, treatment, record_date "
                            + "FROM medical_record_table WHERE d_id = " + user.get("u_id");
                    String[] h3 = {"Record ID", "Patient ID", "Appointment ID", "Diagnosis", "Prescription", "Treatment", "Date"};
                    String[] c3 = {"record_id", "p_id", "a_id", "diagnosis", "prescription", "treatment", "record_date"};
                    db.viewRecords(q3, h3, c3);
                    break;

                case 6:
                    System.out.println("Logging out...");
                    return;
            }
        } while (choice != 6);
    }

    public static void patientMenu(Scanner scan, config db, Map<String, Object> user) {
        // Get the p_id for the logged-in patient
        String getPidQuery = "SELECT p_id FROM patient_table WHERE u_id = ?";
        List<Map<String, Object>> patientList = db.fetchRecords(getPidQuery, user.get("u_id"));
        
        if (patientList.isEmpty()) {
            System.out.println("❌ You do not have a patient profile. Please contact admin.");
            return;
        }
        
        Object p_id_obj = patientList.get(0).get("p_id");
        int p_id = (p_id_obj instanceof Long) ? ((Long) p_id_obj).intValue() : (Integer) p_id_obj;

        int choice;
        do {
            System.out.println("\n===== PATIENT DASHBOARD =====");
            System.out.println("1. View My Information");
            System.out.println("2. View My Appointments");
            System.out.println("3. View My Medical Records");
            System.out.println("4. Logout");
            System.out.print("Enter choice: ");
            choice = scan.nextInt();
            scan.nextLine(); // Clear buffer

            switch (choice) {
                case 1:
                    System.out.println("=== MY INFORMATION ===");
                    String qry = "SELECT * FROM patient_table WHERE u_id = " + user.get("u_id");
                    String[] hdr = {"ID", "User ID", "Full Name", "DOB", "Gender", "Address", "Contact"};
                    String[] col = {"p_id", "u_id", "p_fullname", "p_dob", "p_gender", "p_address", "p_contact"};
                    db.viewRecords(qry, hdr, col);
                    break;

                case 2:
                    System.out.println("=== MY APPOINTMENTS ===");
                    String q2 = "SELECT * FROM appointment_table WHERE p_id = " + p_id;
                    String[] h2 = {"Appointment ID", "Patient ID", "Doctor ID", "Date", "Reason"};
                    String[] c2 = {"a_id", "p_id", "d_id", "a_date", "a_reason"};
                    db.viewRecords(q2, h2, c2);
                    break;

                case 3:
                    System.out.println("=== MY MEDICAL RECORDS ===");
                    String q3 = "SELECT * FROM medical_record_table WHERE p_id = " + p_id;
                    String[] h3 = {"Record ID", "Appointment ID", "Diagnosis", "Prescription", "Treatment", "Date"};
                    String[] c3 = {"record_id", "a_id", "diagnosis", "prescription", "treatment", "record_date"};
                    db.viewRecords(q3, h3, c3);
                    break;

                case 4:
                    System.out.println("Logging out...");
                    return;
            }
        } while (choice != 4);
    }
}