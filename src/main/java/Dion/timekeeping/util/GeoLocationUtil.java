package Dion.timekeeping.util;

public class GeoLocationUtil {

    private static final double EARTH_RADIUS_METERS = 6371000.0;

    /**
     * Tính khoảng cách giữa 2 tọa độ GPS (kinh độ, vĩ độ) bằng công thức Haversine (đơn vị: mét)
     */
    public static double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }

    /**
     * Kiểm tra xem vị trí hiện tại có nằm trong bán kính cho phép của công ty hay không
     */
    public static boolean isWithinAllowedRadius(double currentLat, double currentLon,
                                                double officeLat, double officeLon,
                                                double allowedRadiusMeters) {
        double distance = calculateDistanceMeters(currentLat, currentLon, officeLat, officeLon);
        return distance <= allowedRadiusMeters;
    }
}
