CREATE DATABASE IF NOT EXISTS tripsplit CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'tripsplit'@'localhost' IDENTIFIED BY 'tripsplit';
GRANT ALL PRIVILEGES ON tripsplit.* TO 'tripsplit'@'localhost';
FLUSH PRIVILEGES;
