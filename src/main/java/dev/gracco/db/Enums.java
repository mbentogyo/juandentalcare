package dev.gracco.db;

import org.apache.commons.text.WordUtils;

public class Enums {
    public enum ActionType {
        CREATE, UPDATE, DELETE;
    }

    public enum Role {
        CLERK, DENTIST, ADMIN;

        public static Role fromString(String value) {
            if (value == null) return null;

            for (Role role : Role.values()) {
                if (role.name().equalsIgnoreCase(value.trim())) {
                    return role;
                }
            }

            return null;
        }

        @Override
        public String toString() {
            return WordUtils.capitalizeFully(this.name());
        }
    }

    public enum Status {
        CONFIRMED("Confirmed"),
        CANCELLED("Cancelled"),
        BOOKED("Booked"),
        RESCHEDULED("Rescheduled"),
        NO_SHOW("No Show");

        private final String value;

        Status(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    public enum Tables {
        APPOINTMENTS, PATIENTS, USERS
    }
}
