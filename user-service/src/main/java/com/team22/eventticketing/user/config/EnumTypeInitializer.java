package com.team22.eventticketing.user.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Component
public class EnumTypeInitializer implements ApplicationRunner {

    private final DataSource dataSource;

    public EnumTypeInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            String dbProduct = conn.getMetaData().getDatabaseProductName();
            if (!dbProduct.toLowerCase().contains("postgresql")) {
                return;
            }

            // Create enum types if not present
            stmt.execute(
                "DO $$ BEGIN " +
                "IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_role') THEN " +
                "CREATE TYPE user_role AS ENUM ('ATTENDEE', 'ADMIN'); " +
                "END IF; END $$"
            );
            stmt.execute(
                "DO $$ BEGIN " +
                "IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_status') THEN " +
                "CREATE TYPE user_status AS ENUM ('ACTIVE', 'DEACTIVATED'); " +
                "END IF; END $$"
            );

            // Drop stale check constraints if present (safe: IF EXISTS)
            ResultSet rs = conn.getMetaData().getTables(null, "public", "users", null);
            if (rs.next()) {
                stmt.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check");
                stmt.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_status_check");

                // Alter columns to native enum type if they are still varchar
                stmt.execute(
                    "DO $$ BEGIN " +
                    "IF EXISTS (SELECT 1 FROM information_schema.columns " +
                    "           WHERE table_name='users' AND column_name='role' AND data_type='character varying') THEN " +
                    "ALTER TABLE users ALTER COLUMN role TYPE user_role USING role::user_role; " +
                    "END IF; END $$"
                );
                stmt.execute(
                    "DO $$ BEGIN " +
                    "IF EXISTS (SELECT 1 FROM information_schema.columns " +
                    "           WHERE table_name='users' AND column_name='status' AND data_type='character varying') THEN " +
                    "ALTER TABLE users ALTER COLUMN status TYPE user_status USING status::user_status; " +
                    "END IF; END $$"
                );

                // Create implicit casts so Hibernate varchar params work with enum columns
                stmt.execute(
                    "DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_cast c " +
                    "JOIN pg_type s ON s.oid=c.castsource JOIN pg_type t ON t.oid=c.casttarget " +
                    "WHERE s.typname='varchar' AND t.typname='user_role') " +
                    "THEN CREATE CAST (varchar AS user_role) WITH INOUT AS IMPLICIT; END IF; END $$"
                );
                stmt.execute(
                    "DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_cast c " +
                    "JOIN pg_type s ON s.oid=c.castsource JOIN pg_type t ON t.oid=c.casttarget " +
                    "WHERE s.typname='varchar' AND t.typname='user_status') " +
                    "THEN CREATE CAST (varchar AS user_status) WITH INOUT AS IMPLICIT; END IF; END $$"
                );
            }
        }
    }
}
