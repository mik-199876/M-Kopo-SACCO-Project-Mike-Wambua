import java.io.*;
import java.time.LocalDateTime;
import java.util.*;

/* ============================================================
   1. CUSTOM EXCEPTION
   ============================================================ */
class InsufficientFundsException extends Exception {
    public InsufficientFundsException(String message) {
        super(message);
    }
}

/* ============================================================
   2. INTERFACE
   ============================================================ */
interface Auditable {
    void logTransaction(String message);
}

/* ============================================================
   3. ABSTRACT CLASS (PARENT)
   ============================================================ */
abstract class Account implements Auditable {
    private String accountNumber;
    private double balance;
    private ArrayList<String> transactionHistory;

    public Account(String accountNumber, double initialBalance) {
        this.accountNumber = accountNumber;
        this.balance = initialBalance;
        this.transactionHistory = new ArrayList<>();
    }

    public String getAccountNumber() { return accountNumber; }
    public double getBalance() { return balance; }
    public ArrayList<String> getTransactionHistory() { return transactionHistory; }

    public void deposit(double amount) {
        if (amount > 0) {
            balance += amount;
            String log = "Deposited: " + amount + " | New Balance: " + balance;
            transactionHistory.add(log);
            logTransaction(log);
        } else {
            System.out.println("Error: Deposit amount must be positive.");
        }
    }

    public void withdraw(double amount) throws InsufficientFundsException {
        if (amount <= 0) {
            System.out.println("Error: Withdrawal amount must be positive.");
            return;
        }
        if (amount > balance) {
            throw new InsufficientFundsException(
                "Insufficient funds. Balance is: " + balance);
        }
        balance -= amount;
        String log = "Withdrew: " + amount + " | New Balance: " + balance;
        transactionHistory.add(log);
        logTransaction(log);
    }

    public abstract double calculateInterest();

    @Override
    public void logTransaction(String message) {
        try (FileWriter fw = new FileWriter("transactions.txt", true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println(LocalDateTime.now() + " | " + accountNumber + "|" + message);
        } catch (IOException e) {
            System.out.println("Error writing to transaction log: " + e.getMessage());
        }
    }
}

/* ============================================================
   4. CONCRETE SUBCLASSES
   ============================================================ */
class SavingsAccount extends Account {
    public SavingsAccount(String accountNumber, double initialBalance) {
        super(accountNumber, initialBalance);
    }

    @Override
    public double calculateInterest() {
        return getBalance() * 0.05;   // 5% interest
    }
}

class LoanAccount extends Account {
    public LoanAccount(String accountNumber, double initialBalance) {
        super(accountNumber, initialBalance);
    }

    @Override
    public double calculateInterest() {
        return getBalance() * 0.12;   // 12% interest
    }
}

/* ============================================================
   5. DOMAIN CLASS (MEMBER)
   ============================================================ */
class Member {
    private String memberId;
    private String name;
    private String nationalId;
    private Map<String, Account> accounts;

    public Member(String memberId, String name, String nationalId) {
        this.memberId = memberId;
        this.name = name;
        this.nationalId = nationalId;
        this.accounts = new HashMap<>();
    }

    public String getMemberId() { return memberId; }
    public String getName() { return name; }
    public String getNationalId() { return nationalId; }
    public Map<String, Account> getAccounts() { return accounts; }

    public void addAccount(String type, Account account) {
        accounts.put(type, account);
    }
}

/* ============================================================
   6. MAIN APPLICATION CLASS
   ============================================================ */
public class SaccoManager {
    private static HashMap<String, Member> membersDb = new HashMap<>();
    private static HashSet<String> registeredNationalIds = new HashSet<>();
    private static final String MEMBERS_FILE = "members.csv";
    private static final String TRANSACTIONS_FILE = "transactions.txt";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        loadData();

        int choice;
        do {
            System.out.println("\n=== M-KOPO SACCO MANAGEMENT SYSTEM ===");
            System.out.println("1. Register New Member");
            System.out.println("2. Open an Account");
            System.out.println("3. Deposit Money");
            System.out.println("4. Withdraw Money");
            System.out.println("5. View Member Details");
            System.out.println("6. Exit & Save");
            System.out.print("Enter choice: ");

            while (!scanner.hasNextInt()) {
                System.out.println("Invalid input. Please enter a number.");
                scanner.next();
            }
            choice = scanner.nextInt();
            scanner.nextLine();  // consume newline

            switch (choice) {
                case 1: registerMember(scanner); break;
                case 2: openAccount(scanner); break;
                case 3: depositMoney(scanner); break;
                case 4: withdrawMoney(scanner); break;
                case 5: viewMemberDetails(scanner); break;
                case 6:
                    saveData();
                    System.out.println("Exiting system. Data saved successfully. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid choice. Try again.");
            }
        } while (choice != 6);

        scanner.close();
    }

    /* ---------- MEMBER REGISTRATION ---------- */
    private static void registerMember(Scanner scanner) {
        System.out.print("Enter Member ID (e.g., M-001): ");
        String id = scanner.nextLine().trim();
        if (id.isEmpty()) {
            System.out.println("Error: Member ID cannot be empty.");
            return;
        }
        if (membersDb.containsKey(id)) {
            System.out.println("Error: Member ID already exists.");
            return;
        }

        System.out.print("Enter Full Name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println("Error: Name cannot be empty.");
            return;
        }

        System.out.print("Enter National ID: ");
        String nationalId = scanner.nextLine().trim();
        if (nationalId.isEmpty()) {
            System.out.println("Error: National ID cannot be empty.");
            return;
        }
        if (registeredNationalIds.contains(nationalId)) {
            System.out.println("Error: A member with this National ID already exists.");
            return;
        }

        Member newMember = new Member(id, name, nationalId);
        membersDb.put(id, newMember);
        registeredNationalIds.add(nationalId);
        System.out.println("Success: Member registered successfully!");
        logEvent("MEMBER REGISTERED: " + id + " - " + name);
    }

    /* ---------- OPEN ACCOUNT ---------- */
    private static void openAccount(Scanner scanner) {
        System.out.print("Enter Member ID: ");
        String id = scanner.nextLine().trim();
        Member member = membersDb.get(id);
        if (member == null) {
            System.out.println("Error: Member not found.");
            return;
        }

        System.out.println("1. Savings Account");
        System.out.println("2. Loan Account");
        System.out.print("Choose account type: ");
        while (!scanner.hasNextInt()) {
            System.out.println("Invalid input. Please enter 1 or 2.");
            scanner.next();
        }
        int type = scanner.nextInt();
        scanner.nextLine();

        String accountNumber = id + "-" + System.currentTimeMillis() % 10000;

        if (type == 1) {
            if (member.getAccounts().containsKey("Savings")) {
                System.out.println("Error: Member already has a Savings account.");
                return;
            }
            member.addAccount("Savings", new SavingsAccount(accountNumber, 0));
            System.out.println("Success: Savings Account (" + accountNumber + ") opened.");
            logEvent("ACCOUNT OPENED: " + id + " | Savings | " + accountNumber);
        } else if (type == 2) {
            if (member.getAccounts().containsKey("Loan")) {
                System.out.println("Error: Member already has a Loan account.");
                return;
            }
            member.addAccount("Loan", new LoanAccount(accountNumber, 0));
            System.out.println("Success: Loan Account (" + accountNumber + ") opened.");
            logEvent("ACCOUNT OPENED: " + id + " | Loan | " + accountNumber);
        } else {
            System.out.println("Invalid account type.");
        }
    }

    /* ---------- DEPOSIT ---------- */
    private static void depositMoney(Scanner scanner) {
        Account account = findAccount(scanner);
        if (account == null) return;

        System.out.print("Enter amount to deposit: ");
        if (!scanner.hasNextDouble()) {
            System.out.println("Invalid amount. Please enter a valid number.");
            scanner.next();
            return;
        }
        double amount = scanner.nextDouble();
        scanner.nextLine();
        account.deposit(amount);
        System.out.println("New Balance: " + account.getBalance());
    }

    /* ---------- WITHDRAW ---------- */
    private static void withdrawMoney(Scanner scanner) {
        Account account = findAccount(scanner);
        if (account == null) return;

        System.out.print("Enter amount to withdraw: ");
        if (!scanner.hasNextDouble()) {
            System.out.println("Invalid amount. Please enter a valid number.");
            scanner.next();
            return;
        }
        double amount = scanner.nextDouble();
        scanner.nextLine();

        try {
            account.withdraw(amount);
            System.out.println("New Balance: " + account.getBalance());
        } catch (InsufficientFundsException e) {
            System.out.println("TRANSACTION FAILED: " + e.getMessage());
        }
    }

    /* ---------- VIEW MEMBER DETAILS ---------- */
    private static void viewMemberDetails(Scanner scanner) {
        System.out.print("Enter Member ID: ");
        String id = scanner.nextLine().trim();
        Member member = membersDb.get(id);
        if (member == null) {
            System.out.println("Error: Member not found.");
            return;
        }

        System.out.println("\n--- MEMBER DETAILS ---");
        System.out.println("Name: " + member.getName());
        System.out.println("Member ID: " + member.getMemberId());
        System.out.println("National ID: " + member.getNationalId());

        Map<String, Account> accounts = member.getAccounts();
        if (accounts.isEmpty()) {
            System.out.println("No accounts opened yet.");
        } else {
            for (Map.Entry<String, Account> entry : accounts.entrySet()) {
                Account acc = entry.getValue();
                System.out.println("Account: " + entry.getKey() +
                        " | No: " + acc.getAccountNumber() +
                        " | Balance: KSh " + acc.getBalance());
                // Polymorphism: calculateInterest() runs Savings or Loan logic
                System.out.println("  Potential Interest: KSh " + acc.calculateInterest());
            }
        }
    }

    /* ---------- HELPER: FIND ACCOUNT ---------- */
    private static Account findAccount(Scanner scanner) {
        System.out.print("Enter Member ID: ");
        String id = scanner.nextLine().trim();
        Member member = membersDb.get(id);
        if (member == null) {
            System.out.println("Error: Member not found.");
            return null;
        }

        System.out.print("Enter account type (Savings/Loan): ");
        String type = scanner.nextLine().trim();
        if (type.equalsIgnoreCase("savings")) type = "Savings";
        else if (type.equalsIgnoreCase("loan")) type = "Loan";

        Account account = member.getAccounts().get(type);
        if (account == null) {
            System.out.println("Error: Account type not found for this member.");
            return null;
        }
        return account;
    }

    /* ---------- HELPER: LOG SYSTEM EVENT ---------- */
    private static void logEvent(String message) {
        try (FileWriter fw = new FileWriter(TRANSACTIONS_FILE, true);
             PrintWriter pw = new PrintWriter(fw)) {
            pw.println("SYSTEM | " + message);
        } catch (IOException e) {
            System.out.println("Error writing to transaction log: " + e.getMessage());
        }
    }

    /* ============================================================
       FILE I/O METHODS
       ============================================================ */
    private static void loadData() {
        File file = new File(MEMBERS_FILE);
        if (!file.exists()) {
            System.out.println("No previous data found. Starting fresh.");
            return;
        }

        try (Scanner fileScanner = new Scanner(file)) {
            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine().trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(",");
                if (parts.length != 6) continue;

                String memberId = parts[0];
                String name     = parts[1];
                String nationalId = parts[2];
                String accType  = parts[3];
                String accNumber = parts[4];
                double balance  = Double.parseDouble(parts[5]);

                // Reconstruct Member (only once)
                Member member = membersDb.get(memberId);
                if (member == null) {
                    member = new Member(memberId, name, nationalId);
                    membersDb.put(memberId, member);
                    registeredNationalIds.add(nationalId);
                }

                // Reconstruct Account (skip NONE markers for account-less members)
                if (!accType.equalsIgnoreCase("NONE")) {
                    Account account;
                    if (accType.equalsIgnoreCase("Savings")) {
                        account = new SavingsAccount(accNumber, balance);
                    } else {
                        account = new LoanAccount(accNumber, balance);
                    }
                    member.addAccount(accType, account);
                }
            }
            System.out.println("Data loaded successfully from " + MEMBERS_FILE);
        } catch (FileNotFoundException e) {
            System.out.println("Error loading data: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("Error parsing data: " + e.getMessage());
        }
    }

    private static void saveData() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(MEMBERS_FILE))) {
            for (Member member : membersDb.values()) {
                if (member.getAccounts().isEmpty()) {
                    // Persist members who haven't opened any account yet
                    pw.println(member.getMemberId() + "," +
                               member.getName() + "," +
                               member.getNationalId() + ",NONE,0,0");
                } else {
                    for (Map.Entry<String, Account> entry : member.getAccounts().entrySet()) {
                        String accType = entry.getKey();
                        Account acc = entry.getValue();
                        pw.println(member.getMemberId() + "," +
                                   member.getName() + "," +
                                   member.getNationalId() + "," +
                                   accType + "," +
                                   acc.getAccountNumber() + "," +
                                   acc.getBalance());
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error saving data: " + e.getMessage());
        }
    }
}