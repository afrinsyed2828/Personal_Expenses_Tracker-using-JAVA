
CREATE DATABASE IF NOT EXISTS expensedb;

USE expensedb;

CREATE TABLE IF NOT EXISTS expenses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100),
    category VARCHAR(50),
    amount DOUBLE,
    expense_date DATE
);
