import java.sql.*;

public class Main {
    private static final String URL = "jdbc:oracle:thin:@localhost:1521:xe";
    private static final String USER = "RIBERA";
    private static final String PASS = "ribera";

    public static void main(String[] args) {
        // Abrimos la conexión una sola vez para todo el programa
        try (Connection connection = DriverManager.getConnection(URL, USER, PASS)) {

            // --- SECCIÓN 1: LISTADO INICIAL ---
            System.out.println("\n" + "=".repeat(70));
            System.out.println("           LISTADO INICIAL DE CICLISTAS Y EQUIPOS");
            System.out.println("=".repeat(70));
            System.out.printf("%-25s | %-15s | %-20s%n", "CICLISTA", "NACIONALIDAD", "EQUIPO");
            System.out.println("-".repeat(70));

            String query1 = "SELECT C.NOMBRE AS NOM_CICLISTA, C.NACIONALIDAD, E.NOMBRE AS NOM_EQUIPO " +
                    "FROM CICLISTA C JOIN EQUIPO E ON C.ID_EQUIPO = E.ID_EQUIPO WHERE ROWNUM <= 5";

            try (PreparedStatement st = connection.prepareStatement(query1);
                 ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    System.out.printf("%-25s | %-15s | %-20s%n",
                            rs.getString("NOM_CICLISTA"), rs.getString("NACIONALIDAD"), rs.getString("NOM_EQUIPO"));
                }
            }

            // --- SECCIÓN 2: TOP 5 RENDIMIENTO AVANZADO (Punto 1 del ejercicio) ---
            System.out.println("\n" + "=".repeat(95));
            System.out.println("                TOP 5 CICLISTAS - ESTADÍSTICAS DE RENDIMIENTO");
            System.out.println("=".repeat(95));
            System.out.printf("%-20s | %-20s | %-10s | %-8s | %-8s | %-6s%n",
                    "NOMBRE", "EQUIPO", "NACIONAL.", "PUNTOS", "PROM.", "ETAPAS");
            System.out.println("-".repeat(95));

            String sqlTop5 = "SELECT C.NOMBRE, E.NOMBRE AS EQUIPO, C.NACIONALIDAD, " +
                    "SUM(P.PUNTOS) AS TOTAL, AVG(P.PUNTOS) AS PROM, COUNT(P.NUMERO_ETAPA) AS ETAPAS " +
                    "FROM CICLISTA C JOIN EQUIPO E ON C.ID_EQUIPO = E.ID_EQUIPO " +
                    "JOIN PARTICIPACION P ON C.ID_CICLISTA = P.ID_CICLISTA " +
                    "GROUP BY C.NOMBRE, E.NOMBRE, C.NACIONALIDAD " +
                    "ORDER BY TOTAL DESC, PROM DESC " +
                    "FETCH FIRST 5 ROWS ONLY";

            try (PreparedStatement st = connection.prepareStatement(sqlTop5);
                 ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    System.out.printf("%-20s | %-20s | %-10s | %8d | %8.2f | %6d%n",
                            rs.getString("NOMBRE"), rs.getString("EQUIPO"), rs.getString("NACIONALIDAD"),
                            rs.getInt("TOTAL"), rs.getDouble("PROM"), rs.getInt("ETAPAS"));
                }
            }

            // --- SECCIÓN 3: COMPARATIVA DE EQUIPOS (Punto 2 del ejercicio) ---
            System.out.println("\n" + "=".repeat(95));
            System.out.println("                      COMPARATIVA PROFESIONAL DE EQUIPOS");
            System.out.println("=".repeat(95));
            System.out.printf("%-25s | %-10s | %-5s | %-8s | %-6s | %-15s%n",
                    "EQUIPO", "PAÍS", "CICL.", "PUNTOS", "EDAD", "LÍDER");
            System.out.println("-".repeat(95));

            String sqlEquipos = "SELECT E.NOMBRE, E.PAIS, COUNT(DISTINCT C.ID_CICLISTA) AS NUM_C, " +
                    "SUM(P.PUNTOS) AS TOTAL_P, AVG(C.EDAD) AS MEDIA_E, " +
                    "(SELECT MAX(C2.NOMBRE) FROM CICLISTA C2 WHERE C2.ID_EQUIPO = E.ID_EQUIPO) AS LIDER " +
                    "FROM EQUIPO E JOIN CICLISTA C ON E.ID_EQUIPO = C.ID_EQUIPO " +
                    "JOIN PARTICIPACION P ON C.ID_CICLISTA = P.ID_CICLISTA " +
                    "GROUP BY E.NOMBRE, E.PAIS, E.ID_EQUIPO ORDER BY TOTAL_P DESC";

            try (PreparedStatement st = connection.prepareStatement(sqlEquipos);
                 ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    System.out.printf("%-25s | %-10s | %5d | %8d | %6.1f | %-15s%n",
                            rs.getString("NOMBRE"), rs.getString("PAIS"), rs.getInt("NUM_C"),
                            rs.getInt("TOTAL_P"), rs.getDouble("MEDIA_E"), rs.getString("LIDER"));
                }
            }

            // --- SECCIÓN 4: ETAPAS ESPECIALES (Punto 3 del ejercicio) ---
            System.out.println("\n" + "=".repeat(95));
            System.out.println("                         INFORME DE ETAPAS ESPECIALES");
            System.out.println("=".repeat(95));

            String sqlEtapas = "SELECT NUMERO, ORIGEN, DESTINO, DISTANCIA_KM, FECHA " +
                    "FROM ETAPA WHERE DISTANCIA_KM > (SELECT AVG(DISTANCIA_KM) FROM ETAPA) " +
                    "OR DISTANCIA_KM = (SELECT MAX(DISTANCIA_KM) FROM ETAPA) " +
                    "OR DISTANCIA_KM = (SELECT MIN(DISTANCIA_KM) FROM ETAPA)";

            try (PreparedStatement st = connection.prepareStatement(sqlEtapas);
                 ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    int numEtapa = rs.getInt("NUMERO");
                    System.out.printf("ETAPA %d: %s -> %s (%s km) [%s]%n",
                            numEtapa, rs.getString("ORIGEN"), rs.getString("DESTINO"),
                            rs.getString("DISTANCIA_KM"), rs.getDate("FECHA"));

                    String sqlPodio = "SELECT C.NOMBRE, P.POSICION FROM PARTICIPACION P " +
                            "JOIN CICLISTA C ON P.ID_CICLISTA = C.ID_CICLISTA " +
                            "WHERE P.NUMERO_ETAPA = ? AND P.POSICION <= 3 ORDER BY P.POSICION";

                    try (PreparedStatement stPodio = connection.prepareStatement(sqlPodio)) {
                        stPodio.setInt(1, numEtapa);
                        try (ResultSet rsPodio = stPodio.executeQuery()) {
                            System.out.print("   Podio: ");
                            while (rsPodio.next()) {
                                System.out.print(rsPodio.getInt("POSICION") + "º " + rsPodio.getString("NOMBRE") + " | ");
                            }
                            System.out.println("\n" + "-".repeat(95));
                        }
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Error de base de datos: " + e.getMessage());
        }
    }
}