package app.util;

import app.mainwindow.MainWindowController;
import cpp.JavaInterface;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Pair;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class for executing SQL queries in the database and handling the results.
 */
public class SQLExecutor {

    /**
     * The {@link JavaInterface} instance for executing SQL queries in the database.
     */
    private final JavaInterface databaseManager;

    /**
     * The {@link TextFlow} component for displaying messages in the console.
     */
    private final TextFlow consoleTextFlow;

    /**
     * The {@link MainWindowController} instance for updating the tables list.
     */
    private final MainWindowController mainWindowController;

    /**
     * Creates a new instance of {@link SQLExecutor} with the specified database manager, console text flow,
     * and main window controller.
     *
     * @param databaseManager        the {@link JavaInterface} instance for executing SQL queries
     * @param consoleTextFlow        the {@link TextFlow} component for displaying messages
     * @param mainWindowController   the {@link MainWindowController} instance for updating the tables list
     */
    public SQLExecutor(JavaInterface databaseManager, TextFlow consoleTextFlow, MainWindowController mainWindowController) {
        this.databaseManager = databaseManager;
        this.consoleTextFlow = consoleTextFlow;
        this.mainWindowController = mainWindowController;
    }

    /**
     * Executes the SQL queries from the given code in the database and displays the results in the application.
     * Results include the execution time, success message, and error message if any.
     *
     * @param code         the SQL code to execute
     * @param isFromEditor true if the code is executed from the editor, false if executed from the other sources
     */
    public void executeQueries(String code, boolean isFromEditor) {
        System.out.println("[RUN] Executing queries");
        String[] splitCode = SQLFormatter.trimCode(code).split(";");

        long startTime = System.nanoTime();
        boolean hasError = false;
        List<String> executedQueries = new ArrayList<>();
        boolean tabsCreated = false;

        for (String query : splitCode) {
            if (query.isEmpty()) continue;

            String formattedQuery = SQLFormatter.formatSQLQuery(query.trim());
            System.out.println("[RUN] Executing formatted query: " + formattedQuery);

            List<String> distinctColumns = new ArrayList<>();
            if (formattedQuery.toLowerCase().startsWith("select distinct")) {
                Pair<String, List<String>> processedQuery = processDistinctQuery(formattedQuery);
                formattedQuery = processedQuery.getKey();
                distinctColumns = processedQuery.getValue();
            }

            if(isModifyingQuery(formattedQuery)) {
                mainWindowController.setHasUnsavedChanges(true);
            }

            databaseManager.executeQuery(formattedQuery);

            File outputFile = new File("output.txt");
            if (FileHelper.checkErrors(outputFile, consoleTextFlow)) {
                hasError = true;
                break;
            }

            boolean isSelectQuery = formattedQuery.toLowerCase().startsWith("select");
            if(isSelectQuery && !distinctColumns.isEmpty()) {
                applyDistinct(outputFile, distinctColumns);
            }

            tabsCreated |= FileHelper.loadTablesFromFile("output.txt", isSelectQuery);

            executedQueries.add(formattedQuery);

            if (formattedQuery.startsWith("DROP TABLE")) {
                String tableName = extractTableName(formattedQuery);
                mainWindowController.removeTableFromList(tableName);
            } else if (formattedQuery.startsWith("CREATE TABLE")) {
                mainWindowController.updateTablesList();
            }
        }

        long endTime = System.nanoTime();
        long executionTime = endTime - startTime;

        if (!hasError && isFromEditor) {
            displaySuccessMessage(executionTime);
            checkForEmptyTables(executedQueries);
        }

        // Set focus based on the execution result
        if (hasError) {
            mainWindowController.resultTabPane.getSelectionModel().select(0); // Select console tab
        } else if (tabsCreated) {
            mainWindowController.resultTabPane.getSelectionModel().select(mainWindowController.resultTabPane.getTabs().size() - 1); // Select last created tab
        } else if (executedQueries.stream().anyMatch(query -> query.equals("SHOW TABLES"))) {
            mainWindowController.resultTabPane.getSelectionModel().select(1); // Select first tab after console
        }
    }

    /**
     * Checks if the given SQL query is a modifying query (INSERT, UPDATE, DELETE, CREATE, DROP).
     *
     * @param query the SQL query to check
     * @return {@code true} if the query is a modifying query, {@code false} otherwise
     */
    public boolean isModifyingQuery(String query) {
        String upperCaseQuery = query.toUpperCase().trim();
        return upperCaseQuery.startsWith("INSERT") ||
                upperCaseQuery.startsWith("UPDATE") ||
                upperCaseQuery.startsWith("DELETE") ||
                upperCaseQuery.startsWith("CREATE") ||
                upperCaseQuery.startsWith("DROP");
    }

    /**
     * Displays a success message in the console with the execution time of the query.
     * @param executionTime the execution time of the query in nanoseconds
     */
    private void displaySuccessMessage(long executionTime) {
        Text spacer = new Text("\n");
        spacer.setStyle("-fx-font-size: 10px;");
        consoleTextFlow.getChildren().add(spacer);

        AnsiTextParser.parseAnsiText("\nQuery has been \033[1;32m\033[1msuccessfully\033[0m executed!\n", consoleTextFlow);

        TextFlow executionTimeLine = new TextFlow();
        executionTimeLine.setLineSpacing(0);

        double iconSize = 16;
        String timeIconSVG = "M8,16C3.589,16,0,12.411,0,8S3.589,0,8,0s8,3.589,8,8-3.589,8-8,8Zm0-14.667" +
                "C4.324,1.333,1.333,4.324,1.333,8s2.991,6.667,6.667,6.667S14.667,11.676,14.667,8S11.676,1.333,8,1.333Z" +
                "M11.333,8c0-.368-.298-.667-.667-.667H9.333V4c0-.368-.298-.667-.667-.667S8,3.632,8,4v4" +
                "c0,.368,.298,.667,.667,.667h2.667c.368,0,.667-.298,.667-.667Z";
        Node timeIcon = SVGHelper.loadSVG(timeIconSVG, iconSize);

        Text executionTimeText = new Text("Execution time");
        executionTimeText.setStyle("-fx-font-weight: bold; -fx-underline: true;");

        Text colonText = new Text(":");
        colonText.setStyle("-fx-font-weight: bold;");

        Text executionTimeValue = new Text(String.format(" %.2f ms", (double) executionTime / 1000000));

        executionTimeLine.getChildren().addAll(timeIcon, new Text(" "), executionTimeText, colonText, executionTimeValue);

        timeIcon.setTranslateY(3); // for aligning the icon with the text

        consoleTextFlow.getChildren().add(executionTimeLine);
    }

    /**
     * Checks if the executed queries contain the "SHOW TABLES" query and if the output file is empty.
     * Displays a message in the console if no tables are found in the database.
     *
     * @param executedQueries the {@link List} of executed queries
     */
    private void checkForEmptyTables(List<String> executedQueries) {
        boolean showTablesExecuted = executedQueries.stream()
                .anyMatch(query -> query.equals("SHOW TABLES"));

        if (showTablesExecuted && !FileHelper.hasTablesInOutput("output.txt")) {
            TextFlowHelper.updateResultTextFlow(consoleTextFlow,
                    "\nNo tables found in the database.", Color.RED, true);
        }
    }

    /**
     * Extracts the table name from the given SQL query.
     * @param query the SQL query to extract the table name from
     *              <p>(e.g., "DROP TABLE `table_name`" -> "table_name")</p>
     * @return the table name extracted from the query
     */
    private String extractTableName(String query) {
        String[] parts = query.split("\\s+");
        if (parts.length >= 3) {
            return parts[2].replace("`", "").replace("'", "").replace("\"", "");
        }
        return "";
    }

    private Pair<String, List<String>> processDistinctQuery(String query) {
        Pattern pattern = Pattern.compile("SELECT\\s+DISTINCT\\s*(?:\\((.*?)\\))?\\s*(.*)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(query);

        if (matcher.find()) {
            String distinctColumnsStr = matcher.group(1);
            String restOfQuery = matcher.group(2);
            List<String> distinctColumns;

            if (distinctColumnsStr != null) {
                if (distinctColumnsStr.trim().equals("*")) {
                    distinctColumns = Collections.singletonList("*");
                } else {
                    distinctColumns = Arrays.stream(distinctColumnsStr.split(","))
                            .map(String::trim)
                            .collect(Collectors.toList());
                }
            } else {
                // If DISTINCT is not in parentheses, assume it's for all columns
                distinctColumns = Collections.singletonList("*");
            }

            String processedQuery = "SELECT " + (distinctColumnsStr != null ? distinctColumnsStr : "") + " " + restOfQuery;
            return new Pair<>(processedQuery, distinctColumns);
        } else {
            // This case should not happen if the query starts with "SELECT DISTINCT"
            return new Pair<>(query, Collections.emptyList());
        }
    }

    private void applyDistinct(File outputFile, List<String> distinctColumns) {
        try {
            List<String> lines = Files.readAllLines(outputFile.toPath());
            if (lines.size() < 2) return; // No data or only headers

            String headers = lines.get(0);
            List<String> headerList = Arrays.asList(headers.split("~"));

            Set<String> uniqueRows = new LinkedHashSet<>();
            boolean isAllColumns = distinctColumns.contains("*") || distinctColumns.size() == headerList.size();

            for (int i = 1; i < lines.size(); i++) {
                String[] values = lines.get(i).split("~");
                StringBuilder distinctValues = new StringBuilder();

                if (isAllColumns) {
                    distinctValues.append(lines.get(i));
                } else {
                    for (String column : distinctColumns) {
                        int index = headerList.indexOf(column);
                        if (index != -1 && index < values.length) {
                            distinctValues.append(values[index]).append("~");
                        }
                    }
                    distinctValues.setLength(distinctValues.length() - 1); // Remove last ~
                }

                uniqueRows.add(distinctValues.toString());
            }

            try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
                writer.println(headers);
                for (String uniqueRow : uniqueRows) {
                    writer.println(uniqueRow);
                }
            }
        } catch (IOException e) {
            System.err.println("Error applying DISTINCT: " + e.getMessage());
        }
    }
}