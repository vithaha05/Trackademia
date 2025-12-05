package com.psgtech.studentportal.services;

import com.psgtech.studentportal.models.Course;
import com.psgtech.studentportal.database.DatabaseManager;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced CGPA Prediction Service
 * Uses Ensemble Model: Damped Linear Regression + Weighted Moving Average
 * More realistic than polynomial regression for academic predictions
 */
public class CGPAPredictionService {

    private DatabaseService databaseService;

    public CGPAPredictionService(DatabaseManager dbManager) {
        this.databaseService = new DatabaseService(dbManager);
    }

    /**
     * Detailed prediction result with breakdown for visualization
     */
    public static class PredictionDetails {
        private double predictedCGPA;
        private double lowerBound;
        private double upperBound;
        private double confidence;
        private String trend;

        private double linearPrediction; // Renamed from polynomial
        private double weightedAvgPrediction;
        private double linearWeight = 0.5;
        private double weightedAvgWeight = 0.5;

        private List<Double> semesterGPAs = new ArrayList<>();
        private List<Double> semesterCGPAs = new ArrayList<>();
        private List<Double> projectedCGPAs = new ArrayList<>();

        private int currentSemester;
        private int totalSemesters;
        private String program;

        // Getters and Setters
        public double getPredictedCGPA() {
            return predictedCGPA;
        }

        public void setPredictedCGPA(double predictedCGPA) {
            this.predictedCGPA = predictedCGPA;
        }

        public double getLowerBound() {
            return lowerBound;
        }

        public void setLowerBound(double lowerBound) {
            this.lowerBound = lowerBound;
        }

        public double getUpperBound() {
            return upperBound;
        }

        public void setUpperBound(double upperBound) {
            this.upperBound = upperBound;
        }

        public double getConfidence() {
            return confidence;
        }

        public void setConfidence(double confidence) {
            this.confidence = confidence;
        }

        public String getTrend() {
            return trend;
        }

        public void setTrend(String trend) {
            this.trend = trend;
        }

        public double getPolynomialPrediction() {
            return linearPrediction;
        }

        public void setPolynomialPrediction(double p) {
            this.linearPrediction = p;
        }

        public double getWeightedAvgPrediction() {
            return weightedAvgPrediction;
        }

        public void setWeightedAvgPrediction(double w) {
            this.weightedAvgPrediction = w;
        }

        public double getPolynomialWeight() {
            return linearWeight;
        }

        public double getWeightedAvgWeight() {
            return weightedAvgWeight;
        }

        public List<Double> getSemesterGPAs() {
            return semesterGPAs;
        }

        public void setSemesterGPAs(List<Double> s) {
            this.semesterGPAs = s;
        }

        public List<Double> getSemesterCGPAs() {
            return semesterCGPAs;
        }

        public void setSemesterCGPAs(List<Double> s) {
            this.semesterCGPAs = s;
        }

        public List<Double> getProjectedCGPAs() {
            return projectedCGPAs;
        }

        public void setProjectedCGPAs(List<Double> p) {
            this.projectedCGPAs = p;
        }

        public int getCurrentSemester() {
            return currentSemester;
        }

        public void setCurrentSemester(int c) {
            this.currentSemester = c;
        }

        public int getTotalSemesters() {
            return totalSemesters;
        }

        public void setTotalSemesters(int t) {
            this.totalSemesters = t;
        }

        public String getProgram() {
            return program;
        }

        public void setProgram(String p) {
            this.program = p;
        }

        public String getFormattedPrediction() {
            return String.format("%.2f - %.2f", lowerBound, upperBound);
        }

        public String getProgressText() {
            return String.format("Semester %d of %d", currentSemester, totalSemesters);
        }

        public int getProgressPercent() {
            return (int) ((double) currentSemester / totalSemesters * 100);
        }
    }

    public static class CGPAPrediction extends PredictionDetails {
    }

    public PredictionDetails getDetailedPrediction(String rollNo, int totalSemesters, String program)
            throws SQLException {
        PredictionDetails details = new PredictionDetails();
        details.setTotalSemesters(totalSemesters);
        details.setProgram(program);

        List<Course> courses = databaseService.getCourses(rollNo);

        if (courses.isEmpty()) {
            details.setPredictedCGPA(0);
            details.setLowerBound(0);
            details.setUpperBound(0);
            details.setConfidence(0);
            details.setTrend("Unknown");
            return details;
        }

        Map<Integer, List<Course>> coursesBySemester = courses.stream()
                .collect(Collectors.groupingBy(Course::getSemester));

        List<Double> semesterGPAs = new ArrayList<>();
        List<Double> semesterCGPAs = new ArrayList<>();
        double cumulativeGradePoints = 0;
        int cumulativeCredits = 0;

        int maxSemester = Collections.max(coursesBySemester.keySet());
        details.setCurrentSemester(maxSemester);

        for (int sem = 1; sem <= maxSemester; sem++) {
            List<Course> semCourses = coursesBySemester.get(sem);
            if (semCourses != null && !semCourses.isEmpty()) {
                double semGradePoints = 0;
                int semCredits = 0;

                for (Course course : semCourses) {
                    semGradePoints += course.getGradePoints() * course.getCredits();
                    semCredits += course.getCredits();
                }

                double gpa = semCredits > 0 ? semGradePoints / semCredits : 0;
                semesterGPAs.add(gpa);

                cumulativeGradePoints += semGradePoints;
                cumulativeCredits += semCredits;
                double cgpa = cumulativeCredits > 0 ? cumulativeGradePoints / cumulativeCredits : 0;
                semesterCGPAs.add(cgpa);
            }
        }

        details.setSemesterGPAs(semesterGPAs);
        details.setSemesterCGPAs(semesterCGPAs);
        details.setTrend(analyzeTrend(semesterGPAs));

        if (semesterCGPAs.size() < 2) {
            double currentCGPA = semesterCGPAs.isEmpty() ? 0 : semesterCGPAs.get(semesterCGPAs.size() - 1);
            details.setPredictedCGPA(currentCGPA);
            details.setPolynomialPrediction(currentCGPA);
            details.setWeightedAvgPrediction(currentCGPA);
            details.setLowerBound(Math.max(0, currentCGPA - 0.3));
            details.setUpperBound(Math.min(10, currentCGPA + 0.3));
            details.setConfidence(0.5);
            return details;
        }

        double currentCGPA = semesterCGPAs.get(semesterCGPAs.size() - 1);

        // Method 1: Damped Linear Regression
        // Uses linear trend but dampens it as we project further
        double linearPrediction = dampedLinearPrediction(semesterGPAs, currentCGPA, maxSemester, totalSemesters);
        linearPrediction = Math.max(0, Math.min(10, linearPrediction));
        details.setPolynomialPrediction(linearPrediction);

        // Method 2: Weighted Moving Average with momentum
        double weightedPrediction = weightedMovingAverageWithMomentum(semesterGPAs, currentCGPA, maxSemester,
                totalSemesters);
        weightedPrediction = Math.max(0, Math.min(10, weightedPrediction));
        details.setWeightedAvgPrediction(weightedPrediction);

        // Ensemble: 50% linear + 50% weighted (more balanced)
        double ensemblePrediction = 0.5 * linearPrediction + 0.5 * weightedPrediction;
        ensemblePrediction = Math.max(0, Math.min(10, ensemblePrediction));
        details.setPredictedCGPA(ensemblePrediction);

        // Generate projected CGPAs for chart
        List<Double> projectedCGPAs = new ArrayList<>(semesterCGPAs);
        double lastCGPA = currentCGPA;
        double avgGPA = semesterGPAs.stream().mapToDouble(Double::doubleValue).average().orElse(currentCGPA);

        for (int sem = maxSemester + 1; sem <= totalSemesters; sem++) {
            // Project future GPA with dampening toward average
            double projectedGPA = predictFutureGPA(semesterGPAs, avgGPA, sem, maxSemester);

            // Calculate projected CGPA (weighted by credits assumed equal)
            int projectedCredits = cumulativeCredits + (sem - maxSemester) * 20; // ~20 credits per sem
            double projectedCGPA = (lastCGPA * cumulativeCredits + projectedGPA * 20) / (cumulativeCredits + 20);
            projectedCGPA = Math.max(0, Math.min(10, projectedCGPA));
            projectedCGPAs.add(projectedCGPA);
            lastCGPA = projectedCGPA;
            cumulativeCredits += 20;
        }
        details.setProjectedCGPAs(projectedCGPAs);

        double confidence = calculateConfidence(semesterCGPAs, maxSemester, totalSemesters);
        double variance = calculateVariance(semesterGPAs);
        double remainingRatio = (double) (totalSemesters - maxSemester) / totalSemesters;
        double rangeWidth = Math.max(0.15, Math.sqrt(variance) * (1 + remainingRatio * 0.5));

        details.setConfidence(confidence);
        details.setLowerBound(Math.max(0, ensemblePrediction - rangeWidth));
        details.setUpperBound(Math.min(10, ensemblePrediction + rangeWidth));

        System.out.println("📊 Ensemble Prediction: " + details.getFormattedPrediction());
        System.out.println("   Damped Linear: " + String.format("%.2f", linearPrediction) + " (50%)");
        System.out.println("   Weighted Momentum: " + String.format("%.2f", weightedPrediction) + " (50%)");
        System.out.println("   Confidence: " + String.format("%.0f%%", confidence * 100));

        return details;
    }

    /**
     * Damped Linear Prediction
     * Uses average GPA as baseline for future predictions
     * Accounts for trend but doesn't over-extrapolate
     */
    private double dampedLinearPrediction(List<Double> gpas, double currentCGPA, int currentSem, int totalSem) {
        if (gpas.size() < 2)
            return currentCGPA;

        // Calculate average GPA - this is the expected baseline
        double avgGPA = gpas.stream().mapToDouble(Double::doubleValue).average().orElse(currentCGPA);

        // Calculate linear regression on GPAs to detect trend
        double[] lr = linearRegression(gpas);
        double slope = lr[1];

        // If improving (positive slope), project with optimism
        // If declining (negative slope), project conservatively
        double trendFactor = slope * 0.3; // Reduced impact of trend

        // Project future CGPA
        double projectedCGPA = currentCGPA;
        int remainingSems = totalSem - currentSem;
        int currentCredits = currentSem * 20; // Approximate

        for (int i = 1; i <= remainingSems; i++) {
            // Future GPA = average + small trend adjustment
            // Trend effect diminishes as we project further
            double dampingFactor = Math.pow(0.8, i - 1);
            double projectedGPA = avgGPA + trendFactor * dampingFactor;
            projectedGPA = Math.max(0, Math.min(10, projectedGPA));

            // Update CGPA with projected semester
            int newTotalCredits = currentCredits + i * 20;
            int prevCredits = currentCredits + (i - 1) * 20;
            projectedCGPA = (projectedCGPA * prevCredits + projectedGPA * 20) / newTotalCredits;
        }

        return projectedCGPA;
    }

    /**
     * Simple linear regression
     * Returns [intercept, slope]
     */
    private double[] linearRegression(List<Double> y) {
        int n = y.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            double x = i + 1;
            sumX += x;
            sumY += y.get(i);
            sumXY += x * y.get(i);
            sumX2 += x * x;
        }

        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double intercept = (sumY - slope * sumX) / n;

        return new double[] { intercept, slope };
    }

    /**
     * Weighted Moving Average with Momentum
     * Projects based on recent trend with dampening
     */
    private double weightedMovingAverageWithMomentum(List<Double> gpas, double currentCGPA, int currentSem,
            int totalSem) {
        if (gpas.isEmpty())
            return currentCGPA;

        int n = gpas.size();

        // Calculate simple average GPA - baseline expectation
        double avgGPA = gpas.stream().mapToDouble(Double::doubleValue).average().orElse(currentCGPA);

        // Calculate trend from recent 2 semesters
        double trend = 0;
        if (n >= 2) {
            double recentGPA = gpas.get(n - 1);
            double prevGPA = gpas.get(n - 2);
            trend = (recentGPA - prevGPA) * 0.2; // Small trend impact
        }

        // Project future CGPA
        double projectedCGPA = currentCGPA;
        int remainingSems = totalSem - currentSem;
        int currentCredits = currentSem * 20;

        for (int i = 1; i <= remainingSems; i++) {
            // Project GPA: use average as baseline with small trend adjustment
            double trendAdjustment = trend * Math.pow(0.7, i - 1);
            double projectedGPA = avgGPA + trendAdjustment;
            projectedGPA = Math.max(0, Math.min(10, projectedGPA));

            // Update CGPA
            int newTotalCredits = currentCredits + i * 20;
            int prevCredits = currentCredits + (i - 1) * 20;
            projectedCGPA = (projectedCGPA * prevCredits + projectedGPA * 20) / newTotalCredits;
        }

        return projectedCGPA;
    }

    /**
     * Predict future GPA for a specific semester
     */
    private double predictFutureGPA(List<Double> gpas, double avgGPA, int targetSem, int currentSem) {
        if (gpas.isEmpty())
            return avgGPA;

        int n = gpas.size();
        double recentGPA = gpas.get(n - 1);

        // Calculate trend
        double trend = 0;
        if (n >= 2) {
            trend = recentGPA - gpas.get(n - 2);
        }

        // Dampen trend as we project further
        int semAway = targetSem - currentSem;
        double dampedTrend = trend * Math.pow(0.6, semAway);

        // Project: recent GPA + dampened trend, but pull toward average
        double projected = recentGPA + dampedTrend;

        // Blend toward average (more as we project further)
        double blendFactor = Math.min(1.0, semAway * 0.15);
        projected = projected * (1 - blendFactor) + avgGPA * blendFactor;

        return Math.max(0, Math.min(10, projected));
    }

    public CGPAPrediction predictFinalCGPA(String rollNo, int totalSemesters) throws SQLException {
        PredictionDetails details = getDetailedPrediction(rollNo, totalSemesters, "");
        CGPAPrediction prediction = new CGPAPrediction();
        prediction.setPredictedCGPA(details.getPredictedCGPA());
        prediction.setLowerBound(details.getLowerBound());
        prediction.setUpperBound(details.getUpperBound());
        prediction.setConfidence(details.getConfidence());
        prediction.setTrend(details.getTrend());
        prediction.setCurrentSemester(details.getCurrentSemester());
        prediction.setTotalSemesters(details.getTotalSemesters());
        prediction.setSemesterGPAs(details.getSemesterGPAs());
        prediction.setPolynomialPrediction(details.getPolynomialPrediction());
        prediction.setWeightedAvgPrediction(details.getWeightedAvgPrediction());
        return prediction;
    }

    private String analyzeTrend(List<Double> gpas) {
        if (gpas.size() < 2)
            return "➡️ Stable";

        int mid = gpas.size() / 2;
        double earlierAvg = gpas.subList(0, mid).stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double laterAvg = gpas.subList(mid, gpas.size()).stream().mapToDouble(Double::doubleValue).average().orElse(0);

        double diff = laterAvg - earlierAvg;

        if (diff > 0.3)
            return "📈 Improving";
        if (diff < -0.3)
            return "📉 Declining";
        return "➡️ Stable";
    }

    private double calculateConfidence(List<Double> cgpas, int current, int total) {
        double dataConfidence = Math.min(1.0, current / (double) total * 1.5);
        double variance = calculateVariance(cgpas);
        double varianceMultiplier = Math.max(0.5, 1.0 - variance / 2.0);
        return dataConfidence * varianceMultiplier;
    }

    private double calculateVariance(List<Double> values) {
        if (values.size() < 2)
            return 0;
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double sumSquaredDiff = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .sum();
        return sumSquaredDiff / (values.size() - 1);
    }
}
