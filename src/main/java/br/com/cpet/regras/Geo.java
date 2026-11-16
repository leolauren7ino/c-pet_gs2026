package br.com.cpet.regras;

import java.util.Locale;

public final class Geo {

    private static final double RAIO_TERRA_METROS = 6_371_000;

    private Geo() {
    }

    public static double distanciaMetros(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * RAIO_TERRA_METROS * Math.asin(Math.sqrt(a));
    }

    public static double distanciaKm(double lat1, double lng1, double lat2, double lng2) {
        return distanciaMetros(lat1, lng1, lat2, lng2) / 1000;
    }

    public static double arredondar2(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    public static String celula(double lat, double lng) {
        return String.format(Locale.ROOT, "%.2f,%.2f", arredondar2(lat), arredondar2(lng));
    }

    public static double[] centroDaCelula(String celula) {
        String[] partes = celula.split(",");
        return new double[] {Double.parseDouble(partes[0]), Double.parseDouble(partes[1])};
    }
}
