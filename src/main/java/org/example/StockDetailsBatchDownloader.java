// File: src/main/java/org/example/StockDetailsBatchDownloader.java
package org.example;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class StockDetailsBatchDownloader {
    private static final String APPLICATION_NAME = "stock-pet-project";
    private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    public static void main(String[] args) throws Exception {
        String serviceAccountKeyPath = "D:\\Developement\\Sample Source Code\\1_Secreate_keys\\my-stocks-479704-4ae8d32ea2e8.json";
        String spreadsheetId = "1frDKWMorSwYhdfCy18uzlvvz7gnn1vAzWr8GH6A2fhE";
        String sheetName = "STOCK DETAILS";
        String range = "'" + sheetName + "'!A6:F254"; // Data range


        String capitalSize = "largecap";

        // Read symbols from midcap.csv (comma-separated, no header)
        List<String> symbols = readSymbolsFromCsv("src/data/reference-data/"+capitalSize+".csv");

        // Setup Google Sheets API
        GoogleCredential credential = GoogleCredential
                .fromStream(new FileInputStream(serviceAccountKeyPath))
                .createScoped(List.of("https://www.googleapis.com/auth/spreadsheets"));

        Sheets sheetsService = new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

        // Prepare output directory
        String outputDirPath = "src/data/"+capitalSize+"-stock-details";
        Files.createDirectories(Path.of(outputDirPath));

        for (String symbol : symbols) {
            // 1. Update cell B1 to current symbol
            List<List<Object>> value = List.of(List.of(symbol));
            ValueRange body = new ValueRange().setValues(value);
            sheetsService.spreadsheets().values()
                    .update(spreadsheetId, "'" + sheetName + "'!B1", body)
                    .setValueInputOption("RAW")
                    .execute();

            // 2. Wait for sheet to update (optional, 1s)
            Thread.sleep(5000);

            // 3. Read the data range
            ValueRange response = sheetsService.spreadsheets().values()
                    .get(spreadsheetId, range)
                    .execute();

            List<List<Object>> values = response.getValues();
            String outputFilePath = outputDirPath + "/" + symbol + ".csv";

            // 4. Write to CSV
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
                if (values == null || values.isEmpty()) {
                    writer.write("No data found.");
                } else {
                    for (List<Object> row : values) {
                        String line = row.stream()
                                .map(Object::toString)
                                .reduce((a, b) -> a + "," + b)
                                .orElse("");
                        writer.write(line);
                        writer.newLine();
                    }
                }
            }
            System.out.println("Data for " + symbol + " written to: " + outputFilePath);
        }
    }

    // Reads symbols from a CSV file (comma-separated, no header, all symbols in one line)
    private static List<String> readSymbolsFromCsv(String csvPath) throws IOException {
        List<String> symbols = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                for (String symbol : parts) {
                    String trimmed = symbol.trim();
                    if (!trimmed.isEmpty()) {
                        symbols.add(trimmed);
                    }
                }
            }
        }
        return symbols;
    }
}