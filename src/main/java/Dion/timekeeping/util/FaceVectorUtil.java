package Dion.timekeeping.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class FaceVectorUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Chuyển chuỗi JSON float array thành List<Double>
     */
    public static List<Double> parseVector(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Double>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Lỗi phân giải Face Embedding Vector JSON: " + e.getMessage());
        }
    }

    /**
     * Chuyển List<Double> thành chuỗi JSON
     */
    public static String toJson(List<Double> vector) {
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi serialize Face Vector: " + e.getMessage());
        }
    }

    /**
     * Tính Cosine Similarity giữa 2 vector (giá trị từ -1 đến 1, càng gần 1 càng giống nhau)
     * Ngưỡng thực tế thường là >= 0.85
     */
    public static double cosineSimilarity(List<Double> v1, List<Double> v2) {
        if (v1 == null || v2 == null || v1.size() != v2.size() || v1.isEmpty()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < v1.size(); i++) {
            double a = v1.get(i);
            double b = v2.get(i);
            dotProduct += a * b;
            normA += a * a;
            normB += b * b;
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Tính khoảng cách Euclidean Distance (càng nhỏ càng giống, thường < 0.6 là cùng 1 người)
     */
    public static double euclideanDistance(List<Double> v1, List<Double> v2) {
        if (v1 == null || v2 == null || v1.size() != v2.size()) {
            return Double.MAX_VALUE;
        }
        double sum = 0.0;
        for (int i = 0; i < v1.size(); i++) {
            double diff = v1.get(i) - v2.get(i);
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }
}
