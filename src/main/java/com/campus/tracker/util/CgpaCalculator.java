package com.campus.tracker.util;

import java.util.*;

public class CgpaCalculator {

    private static final Map<String, Integer> LETTER_GRADE_TO_NUMBER = new HashMap<>() {{
        put("O", 10);
        put("A+", 9);
        put("A", 8);
        put("B+", 7);
        put("B", 6);
        put("C", 5);
    }};

    // Expects a list of course maps, each with keys: semester, grade, credit, etc.
    public static List<Map<String, String>> calculateGpaAndCgpa(List<Map<String, String>> courseList) {
        List<Map<String, String>> result = new ArrayList<>();

        // Organize by semester
        Map<String, List<Map<String, String>>> coursesBySem = new LinkedHashMap<>();
        for (Map<String, String> course : courseList) {
            String semester = course.get("semester").trim();
            coursesBySem.computeIfAbsent(semester, k -> new ArrayList<>()).add(course);
        }

        double overallProduct = 0;
        double overallCredits = 0;
        boolean foundBacklog = false;

        for (String sem : coursesBySem.keySet()) {
            List<Map<String, String>> courses = coursesBySem.get(sem);

            double semProduct = 0;
            double semCredits = 0;
            boolean thisSemBacklog = false;

            for (Map<String, String> course : courses) {
                String gradeStr = course.get("grade").toUpperCase().replaceAll("[^A-Z+]", "");
                String resultStatus = course.getOrDefault("result", "Pass").trim();

                // Ignore non-letter grades (or ungraded entries)
                if (LETTER_GRADE_TO_NUMBER.containsKey(gradeStr)) {
                    int grade = LETTER_GRADE_TO_NUMBER.get(gradeStr);
                    double credit = Double.parseDouble(course.get("credit").trim());
                    semProduct += grade * credit;
                    semCredits += credit;
                }

                if (resultStatus.equalsIgnoreCase("RA") || resultStatus.equalsIgnoreCase("Reappear")) {
                    thisSemBacklog = true;
                }
            }

            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("semester", sem);

            if (!thisSemBacklog && !foundBacklog && semCredits > 0) {
                overallProduct += semProduct;
                overallCredits += semCredits;
                double gpa = semProduct / semCredits;
                double cgpa = overallProduct / overallCredits;
                entry.put("gpa", String.format("%.2f", gpa));
                entry.put("cgpa", String.format("%.2f", cgpa));
            } else {
                entry.put("gpa", "-");
                entry.put("cgpa", "-");
                foundBacklog = true;
            }

            result.add(entry);
        }
        return result;
    }
}
