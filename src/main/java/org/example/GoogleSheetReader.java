package org.example;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class GoogleSheetReader {

    private static final String APPLICATION_NAME = "stock-pet-project";
    private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    public static void main(String[] args) throws Exception {


        String serviceAccountKeyPath = "D:\\Developement\\Sample Source Code\\1_Secreate_keys\\my-stocks-479704-4ae8d32ea2e8.json";
        String spreadsheetId = "1frDKWMorSwYhdfCy18uzlvvz7gnn1vAzWr8GH6A2fhE";
        String tabName = "Top 20";
        String range = "Top 20!A1:H20";

        GoogleCredential credential = GoogleCredential
                .fromStream(new FileInputStream(serviceAccountKeyPath))
                .createScoped(List.of("https://www.googleapis.com/auth/spreadsheets.readonly"));

        Sheets sheetsService = new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

        ValueRange response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();

        List<List<Object>> values = response.getValues();

        // Prepare output directory and file
        String outputDirPath = "src/data";
        Files.createDirectories(Path.of(outputDirPath));
        String outputFilePath = outputDirPath + "/" + tabName + ".txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
            //writer.write(tableName);
            //writer.newLine();
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

        System.out.println("Data written to: " + outputFilePath);
    }
}
