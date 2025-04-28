# Construction-Application-DB-final


A Java desktop application built with **IntelliJ IDEA** for managing clients, subcontractors, products, sales, discounts, and revenue for a construction company. The system uses an **SQL database** for data storage and retrieval.

## Features
- User registration and login system
- Dashboard overview
- Manage clients, subcontractors, and products
- Track client and subcontractor sales
- Assign and manage discounts
- Revenue tracking
- Upload and display profile images
- View detailed client profiles
- Data visualization for top-performing clients and products

## Technologies Used
- Java (Swing GUI)
- SQL Database (MySQL recommended)
- IntelliJ IDEA
- JDBC for database connection

## Project Structure
src/
├── dbcon.java                
├── Main.java                 
├── ProfileImageUploader.java 
├── userdatastore.java        
├── welcomepage_image.png     
├── clientdiscountspage/      
├── ClientProfileViewer/
├── clientspage/
├── clientsSalesPage/
├── dashboardpage/
├── discountspage/
├── loginpage/
├── productspage/
├── revenuepage/
├── salespage/
├── signuppage/
├── subconstructorspage/
├── subconstructorsSalesPage/
├── subcontractordiscountspage/
└── welcomepage/


## Getting Started

1. **Clone** the repository or **download** the project ZIP.
2. **Open** the project with **IntelliJ IDEA**.
3. **Set up the database**:
   - Create a new SQL database (e.g., MySQL).
   - Import the provided database schema (if available) or manually create tables for users, clients, products, sales, discounts, etc.
4. **Configure database credentials** in `dbcon.java`.
5. **Run** `Main.java` to start the application.

## Database Setup
- Ensure you have a MySQL server running.
- Create a database and required tables.
- Example connection snippet in `dbcon.java`:
  ```java
  Connection con = DriverManager.getConnection(
      "jdbc:mysql://localhost:3306/your_database_name", "username", "password");

## How to Run
Locate Main.java.

Right-click and select Run 'Main'.

## Author
Lyudmila Petrova
