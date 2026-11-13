-- Creates the schema and an application user. Tables are created by Hibernate (ddl-auto=update).
CREATE DATABASE IF NOT EXISTS bankdb
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'bank'@'%' IDENTIFIED BY 'bankpw';
GRANT ALL PRIVILEGES ON bankdb.* TO 'bank'@'%';
FLUSH PRIVILEGES;
