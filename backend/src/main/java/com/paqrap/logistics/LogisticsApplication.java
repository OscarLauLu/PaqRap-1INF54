package com.paqrap.logistics;

import com.paqrap.logistics.planificacion.algoritmo.BenchmarkMetaheuristicas;
import com.paqrap.logistics.planificacion.algoritmo.Experimento5DRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

/**
 * Clase principal de la aplicación Spring Boot para el sistema de logística de PaqRap.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class LogisticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogisticsApplication.class, args);
    }

    @Bean
    public CommandLineRunner benchmarkCommandLineRunner(ApplicationArguments appArgs) {
        return args -> {
            if (appArgs.containsOption("experimento") || appArgs.containsOption("exp5d") || "true".equalsIgnoreCase(System.getenv("RUN_EXPERIMENTO"))) {
                java.util.List<String> expArgs = new java.util.ArrayList<>();
                for (String opt : appArgs.getOptionNames()) {
                    if ("experimento".equalsIgnoreCase(opt) || "exp5d".equalsIgnoreCase(opt)) continue;
                    java.util.List<String> vals = appArgs.getOptionValues(opt);
                    if (vals.isEmpty()) {
                        expArgs.add("--" + opt);
                    } else {
                        expArgs.add("--" + opt + "=" + vals.get(0));
                    }
                }
                for (String nonOpt : appArgs.getNonOptionArgs()) {
                    expArgs.add(nonOpt);
                }
                Experimento5DRunner.main(expArgs.toArray(new String[0]));
                System.exit(0);
            }

            if (appArgs.containsOption("benchmark") || "true".equalsIgnoreCase(System.getenv("RUN_BENCHMARK"))) {
                String mes = null;
                String archivoVentas = null;
                String limite = null;
                Integer diaInicio = null;
                boolean averia = "true".equalsIgnoreCase(System.getenv("SIMULAR_AVERIA"));

                // 1. Interpretar argumentos posicionales (cualquier orden: mes, límite de días/pedidos, archivo, avería, día inicio)
                for (String arg : appArgs.getNonOptionArgs()) {
                    String clean = arg.trim();
                    if (clean.matches("\\d{6}")) {
                        mes = clean;
                    } else if (clean.endsWith(".txt") || clean.contains("/")) {
                        archivoVentas = clean;
                    } else if (clean.matches("(\\d+)[-\\.\\.]+(\\d+)d?") || clean.matches(".*(\\d+)\\+(\\d+)d?") || clean.matches("(\\d+)d(ias)?") || "todos".equalsIgnoreCase(clean) || "all".equalsIgnoreCase(clean) || "mes".equalsIgnoreCase(clean)) {
                        limite = clean;
                    } else if (clean.matches("dia(\\d+)") || clean.matches("inicio(\\d+)")) {
                        diaInicio = Integer.parseInt(clean.replaceAll("\\D", ""));
                    } else if (clean.matches("\\d+")) {
                        int num = Integer.parseInt(clean);
                        if (num <= 31 && limite != null && diaInicio == null) {
                            diaInicio = num;
                        } else {
                            limite = clean;
                        }
                    } else if ("averia".equalsIgnoreCase(clean)) {
                        averia = true;
                    }
                }

                // 2. Flags con nombre tienen precedencia si se especificaron
                if (appArgs.containsOption("mes")) mes = appArgs.getOptionValues("mes").get(0);
                if (appArgs.containsOption("pedidos")) limite = appArgs.getOptionValues("pedidos").get(0);
                if (appArgs.containsOption("dias")) limite = appArgs.getOptionValues("dias").get(0) + "d";
                if (appArgs.containsOption("inicio")) diaInicio = Integer.parseInt(appArgs.getOptionValues("inicio").get(0).replaceAll("\\D", ""));
                if (appArgs.containsOption("desde")) diaInicio = Integer.parseInt(appArgs.getOptionValues("desde").get(0).replaceAll("\\D", ""));
                if (appArgs.containsOption("dia")) diaInicio = Integer.parseInt(appArgs.getOptionValues("dia").get(0).replaceAll("\\D", ""));
                if (appArgs.containsOption("averia")) averia = true;

                // 3. Defaults razonables únicamente si el usuario no indicó nada
                if (mes == null && archivoVentas == null) mes = "202601";
                if (limite == null) limite = "5d"; // Horizonte de 5 días por defecto

                java.util.List<String> bArgs = new java.util.ArrayList<>();
                bArgs.add(archivoVentas != null ? archivoVentas : mes);
                bArgs.add("");
                bArgs.add(limite);
                if (averia) bArgs.add("averia");
                if (diaInicio != null) bArgs.add("--inicio=" + diaInicio);

                for (String opt : appArgs.getOptionNames()) {
                    if ("flota".equals(opt) && !appArgs.getOptionValues(opt).isEmpty()) {
                        bArgs.add("--flota=" + appArgs.getOptionValues(opt).get(0));
                    }
                }
                BenchmarkMetaheuristicas.main(bArgs.toArray(new String[0]));
                System.exit(0);
            }
        };
    }
}

