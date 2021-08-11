package org.orcid.service.assertions.upload;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.orcid.domain.Assertion;

public class AssertionsUpload {

    List<Assertion> assertions = new ArrayList<>();

    Set<String> users = new HashSet<>();

    List<AssertionsUploadError> errors = new ArrayList<>();

    public void addAssertion(Assertion assertion) {
        assertions.add(assertion);
    }

    public void removeAssertion(Assertion assertion) {
        assertions.remove(assertion);
    }

    public void addUser(String user) {
        users.add(user);
    }

    public void addError(long index, String message) {
        errors.add(new AssertionsUploadError(index, message));
    }

    public List<Assertion> getAssertions() {
        return assertions;
    }

    public Set<String> getUsers() {
        return users;
    }

    public List<AssertionsUploadError> getErrors() {
        return errors;
    }

    public static class AssertionsUploadDate {

        String year;
        String month;
        String day;

        public AssertionsUploadDate(String year, String month, String day) {
            this.year = year;
            this.month = month;
            this.day = day;
        }

        public String getYear() {
            return year;
        }

        public String getMonth() {
            return month;
        }

        public String getDay() {
            return day;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder(year);
            if (month != null) {
                builder.append("-").append(month);
                if (day != null) {
                    builder.append("-").append(day);
                }
            }
            return builder.toString();
        }

    }

}
