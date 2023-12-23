package de.uniwue.jpp.mensabot.gui;

import de.uniwue.jpp.errorhandling.OptionalWithMessage;
import de.uniwue.jpp.mensabot.dataclasses.Meal;
import de.uniwue.jpp.mensabot.dataclasses.Menu;
import de.uniwue.jpp.mensabot.retrieval.Fetcher;
import de.uniwue.jpp.mensabot.retrieval.Parser;
import de.uniwue.jpp.mensabot.retrieval.Saver;
import de.uniwue.jpp.mensabot.sending.Importer;
import de.uniwue.jpp.mensabot.sending.Sender;
import de.uniwue.jpp.mensabot.sending.formatting.Formatter;
import de.uniwue.jpp.mensabot.sending.formatting.analyze.Analyzer;
import javafx.application.Application;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;


public class MensabotGui extends Application {
    /*-----------------------------------------------------------------------------------------
    General Structure of this file:
        1. declaration of variables and FXML Tags
        2. start & initialize methods to set up the GUI for use
        3. Methods to fetch data and update pages/data display accordingly
        4. Various other methods used by the GUI, mostly independent
     -----------------------------------------------------------------------------------------*/
    /*-----------------------------------------------------------------------------------------
    first two letters in the names of fx:ids and methods declare tab, also methods are sorted according to this order
    pp - pipelining
    fc - formatter creation
    dg - diagrams
    st - stats
    lv - log viewer
    -----------------------------------------------------------------------------------------*/
    @FXML
    TableView<Menu> lvTable;
    @FXML
    Label ppMealToday, ppStatsTodayHeadline, ppStatsTodayContent, lvNumberOfResults, ppInfo, fcInfo, stAveragePriceAll, stAveragePriceToday, stMostExpensiveToday,
            stMostExpensiveAll, stCheapestToday, stCheapestAll, stSumToday, stSumAll, stMostPopularToday, stMostPopularAll, stMedianPriceToday, stMedianPriceAll,
            stStandardDeviationToday, stStandardDeviationAll, stSumOfMealsToday, stSumOfMealsAll, fcFormatterHeadlineLable, fcFormatterFormatLable, fcFormatterNameLable;
    @FXML
    TitledPane ppMealTodayTitle,ppStatsTodayTitle, stAveragePriceTitle, stMostExpensiveTitle, stCheapestTitle, stSumTitle, stMostPopularTitle, stMedianPrice, stStandardDeviation, stSumOfMeals;
    @FXML
    TextField lvSearchBar, fcFormatterName, fcFormatterHeadline, ppNameOfLogfile;
    @FXML
    TextArea fcFormatterFormat;
    @FXML
    Spinner<Integer> lvResultSelector;
    @FXML
    ListView<Formatter> ppFormatterListView, fcFormatterListView;
    @FXML
    ListView<Analyzer<?>> fcAvailableAnalyzers, fcSelectedAnalyzers;
    @FXML
    Button fcSimpleSelection, fcComplexSelection,fcAddAnalyzerButton ;
    @FXML
    LineChart<String, Double> dgLineChart;
    @FXML
    PieChart dgPieChart;
    @FXML
    BarChart<String, Integer> dgBarChart;
    @FXML
    VBox fcSelectedAnalyzersBox, fcAvailableAnalyzersBox;
    @FXML
    DatePicker stDatePicker, ppDatePicker;


    /*-----------------------------------------------------------------------------------------
    Global variables for GUI
    -----------------------------------------------------------------------------------------*/
    private final ObservableList<Formatter> formatterList = FXCollections.observableArrayList();
    private final ObservableList<Analyzer<?>> selectedAnalyzerList = FXCollections.observableArrayList();
    private static final ObservableList<Menu> logData = FXCollections.observableArrayList();
    private static final SortedList<Menu> logDataSorted = logData.sorted((m1, m2) -> (m2.getDate().compareTo(m1.getDate())));
    private final ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
    private final XYChart.Series<String,Double> seriesAverage = new XYChart.Series<>();
    private final XYChart.Series<String,Double> seriesTotal = new XYChart.Series<>();
    private final XYChart.Series<String,Integer> seriesMealCount = new XYChart.Series<>();
    private boolean fcGenerateComplex = false;
    private Menu menuToday;
    private Path logfile = Path.of("log.csv");


    /*------------------------------------------------------------------------------------------------------------------------------------------
     Start and initialize methods for setup
    ------------------------------------------------------------------------------------------------------------------------------------------*/
    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("MensabotGui.fxml")));
        primaryStage.setTitle("Mensabot");
        primaryStage.setScene(new Scene(root, 1920, 1080));
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    @FXML
    public void initialize() {
        stDatePicker.setValue(LocalDate.now());
        ppDatePicker.setValue(LocalDate.now());

        //setup diagrams
        seriesAverage.setName("Durchschnittspreis");
        seriesTotal.setName("Gesamtpreis");

        dgLineChart.getData().add(seriesAverage);
        dgLineChart.getData().add(seriesTotal);

        dgBarChart.getData().add(seriesMealCount);
        dgBarChart.setLegendVisible(false);

        dgPieChart.setData(pieChartData);

        //listener, to add fetched data to diagrams
        logData.addListener((ListChangeListener<Menu>) c -> {
            while(c.next()){
                if(c.wasAdded()){
                    for(Menu menu : c.getAddedSubList()){
                        List<Menu> menuAsList = new ArrayList<>();
                        menuAsList.add(menu);
                        seriesAverage.getData().add(new XYChart.Data<>(menu.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), Analyzer.createAveragePriceAnalyzer().analyze(menuAsList).get().doubleValue()/100));
                        seriesTotal.getData().add(new XYChart.Data<>(menu.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), Analyzer.createTotalPriceAnalyzer().analyze(menuAsList).get().doubleValue()/100));
                        seriesMealCount.getData().add(new XYChart.Data<>(menu.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), menu.getMeals().size()));
                        sortDiagrams();

                        pieChartData.clear();
                        OptionalWithMessage<List<Integer>> priceRange = Analyzer.createPriceRangeAnalyzer(5, 100).analyze(logDataSorted);
                        pieChartData.addAll(
                                new PieChart.Data("0\u20ac-1\u20ac", priceRange.get().get(0)),
                                new PieChart.Data("1\u20ac-2\u20ac", priceRange.get().get(1)),
                                new PieChart.Data("2\u20ac-3\u20ac", priceRange.get().get(2)),
                                new PieChart.Data("3\u20ac-4\u20ac", priceRange.get().get(3)),
                                new PieChart.Data("4\u20ac-5\u20ac", priceRange.get().get(4)),
                                new PieChart.Data(">5\u20ac", priceRange.get().get(5))
                        );
                        pieChartData.removeIf(p -> p.getPieValue() == 0);
                    }
                }
            }
        });

        formatterList.addAll(
                Formatter.createSimpleFormatter(),
                Formatter.createSimpleMealFormatter(),
                Formatter.createHiddenFormatter(),
                Formatter.createFirstWordFormatter(),
                Formatter.createShortFormatter(),
                Formatter.createPricelessFormatter(),
                Formatter.createSimpleTotalFormatter()
        );
        ppFormatterListView.setItems(formatterList);
        fcFormatterListView.setItems(formatterList);

        //setup display of analyzers on formatter creation tab
        ObservableList<Analyzer<?>> analyzerList = FXCollections.observableArrayList();
        analyzerList.addAll(
                Analyzer.createAveragePriceAnalyzer(),
                Analyzer.createMedianPriceAnalyzer(),
                Analyzer.createMinPriceMealAnalyzer(),
                Analyzer.createMaxPriceMealAnalyzer(),
                Analyzer.createTotalPriceAnalyzer()
        );
        fcAvailableAnalyzers.setItems(analyzerList);
        fcSelectedAnalyzers.setItems(selectedAnalyzerList);

        lvNumberOfResults.setVisible(false);

        importLogfile();
        getMenuByDate(LocalDate.now());
        updateStatDisplay();
        setupLogViewerTable();
        ppHelp();
    }

   /*------------------------------------------------------------------------------------------------------------------------------------------
    Methods to fetch data and update pages
    ------------------------------------------------------------------------------------------------------------------------------------------*/
    //fetches Data and saves to logfile, update stats accordingly
    public void ppFetchSave(){
        Fetcher fetcher = Fetcher.createDummyCsvFetcher();
        Parser parser = Parser.createCsvParser();
        Saver saver = Saver.createCsvSaver();

        OptionalWithMessage<Menu> fetchedMenu = fetcher.fetchCurrentData().flatMap(parser::parse);
        Optional<String> trySave = fetchedMenu.tryToConsume(s -> saver.log(logfile, s));
        if(trySave.isEmpty()){
            ppInfo.setText(importLineFromLogfile().orElse("Daten erfolgreich in " + logfile.toString() + " gespeichert"));
            fetchedMenu.map(Menu::getDate).consume(ppDatePicker::setValue);
            fetchedMenu.map(Menu::getDate).consume(stDatePicker::setValue);
            menuToday = fetchedMenu.get();
            updateStatDisplay();
        }else{
            ppInfo.setText(trySave.get());
        }
    }
    //imports the whole logfile
    private Optional<String> importLogfile(){
        Importer importer = Importer.createCsvImporter();
        logData.clear();
        try{
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(logfile.toFile()), StandardCharsets.UTF_8));
            OptionalWithMessage<List<Menu>> data = importer.getAll(reader);

            if(data.isEmpty()){
                return Optional.of("File is empty");
            }

            return  data.consume(logData::addAll);

        }catch (FileNotFoundException e){
            return Optional.of("FileNotFound");
        } catch (Exception e){
            return Optional.of("UnexpectedException");
        }
    }
    //imports the first line of logfile
    private Optional<String> importLineFromLogfile(){

        Importer importer = Importer.createCsvImporter();

        try{
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(logfile.toFile()), StandardCharsets.UTF_8));
            OptionalWithMessage<Menu> data = importer.getLatest(reader);

            return data.consume(logData::add);

        } catch (Exception e){
            return Optional.of("UnexpectedException");
        }
    }

    //sorts the diagram data ascending by date
    private void sortDiagrams(){
        seriesMealCount.getData().sort(Comparator.comparing(d -> LocalDate.parse(d.getXValue(), DateTimeFormatter.ofPattern("dd.MM.yyyy"))));
        seriesTotal.getData().sort(Comparator.comparing(d -> LocalDate.parse(d.getXValue(), DateTimeFormatter.ofPattern("dd.MM.yyyy"))));
        seriesAverage.getData().sort(Comparator.comparing(d -> LocalDate.parse(d.getXValue(), DateTimeFormatter.ofPattern("dd.MM.yyyy"))));
    }

    //delets all data from diagrams
    private void refreshDiagrams(){
        seriesMealCount.getData().clear();
        seriesTotal.getData().clear();
        seriesAverage.getData().clear();
        pieChartData.clear();
        dgLineChart.getData().clear(); //needed to remove remaining lines in view (don´t ask me why)
        dgLineChart.getData().add(seriesAverage);
        dgLineChart.getData().add(seriesTotal);
    }

    private void setupLogViewerTable(){
        lvTable.getColumns().clear();
        TableColumn<Menu, String> dateColumn = new TableColumn<>("Datum");
        dateColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))));

        lvTable.getColumns().add(dateColumn);
        lvTable.setItems(logDataSorted);
        int colsNeeded = 1;
        for(Menu m : logDataSorted){
            String[] meals = m.toCsvLine().split(";");
            if((meals.length-1) > colsNeeded){
                colsNeeded = (meals.length-1);
            }
        }
        for(int newColPairNr = 1; newColPairNr <= colsNeeded; newColPairNr++){
            lvTable.getColumns().addAll(createColumnPair(newColPairNr));
        }

        logDataSorted.comparatorProperty().bind(lvTable.comparatorProperty());
    }
    private void updateStatDisplay(){
        Formatter simpleMealFormatter = Formatter.createSimpleMealFormatter();
        LocalDate dateForStats = stDatePicker.getValue();

        List<String> headlinesList = new ArrayList<>();
        headlinesList.add("Mittelwert der Preise");
        headlinesList.add("Median der Preise");
        headlinesList.add("Billigstes Gericht");
        headlinesList.add("Teuerstes Gericht");
        headlinesList.add("Summe aller Preise");

        ppMealTodayTitle.setText("Essen am " + dateForStats.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        ppStatsTodayTitle.setText("Statistiken f\u00fcr den " + dateForStats.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));

        ppMealToday.setText("Leider gibt es an diesem Tag kein Essen in der Mensa");
        ppStatsTodayHeadline.setText("Leider gibt es an diesem Tag kein Essen in der Mensa");
        ppStatsTodayContent.setText("");
        stAveragePriceToday.setText("Leider gibt es an diesem Tag kein Essen in der Mensa");
        stCheapestToday.setText("Leider gibt es an diesem Tag kein Essen in der Mensa");
        stSumToday.setText("Leider gibt es an diesem Tag kein Essen in der Mensa");
        stMostPopularToday.setText("Leider gibt es an diesem Tag kein Essen in der Mensa");
        stMostExpensiveToday.setText("Leider gibt es an diesem Tag kein Essen in der Mensa");
        stAveragePriceAll.setText("Keine Daten vorhanden");
        stMedianPriceToday.setText("Leider gibt es an diesem Tag kein Essen in der Mensa");
        stMedianPriceAll.setText("Keine Daten vorhanden");
        stStandardDeviationToday.setText("Leider gibt es an diesem Tag kein Essen in der Mensa");
        stStandardDeviationAll.setText("Keine Daten vorhanden");
        stSumOfMealsToday.setText("Leider gibt es an diesem Tag kein Essen in der Mensa");
        stSumOfMealsAll.setText("Keine Daten vorhanden");

        ArrayList<Menu> singleMenuList = new ArrayList<>();
        if(menuToday != null){
            singleMenuList.add(menuToday);
            simpleMealFormatter.format(menuToday, () -> OptionalWithMessage.of(logDataSorted)).consume(ppMealToday::setText);

            OptionalWithMessage<String> averagePriceToday = Analyzer.createAveragePriceAnalyzer().analyze(singleMenuList, Formatter::centToEuro);
            OptionalWithMessage<String> medianToday = Analyzer.createMedianPriceAnalyzer().analyze(singleMenuList, Formatter::centToEuro);
            OptionalWithMessage<String> mostExpensiveToday = Analyzer.createMaxPriceMealAnalyzer().analyze(singleMenuList, Meal::toString);
            OptionalWithMessage<String> cheapestToday = Analyzer.createMinPriceMealAnalyzer().analyze(singleMenuList, Meal::toString);
            OptionalWithMessage<String> sumToday = Analyzer.createTotalPriceAnalyzer().analyze(singleMenuList, Formatter::centToEuro);
            OptionalWithMessage<String> mostPopularToday = Analyzer.createPopularityAnalyzer().analyze(singleMenuList, s -> s.get(0).toString());
            OptionalWithMessage<String> sumOfMealsToday = Analyzer.createAmountOfDishesAnalyzer().analyze(singleMenuList, Object::toString);
            OptionalWithMessage<String> standardDeviationToday = Analyzer.createStandardDeviationAnalyzer().analyze(singleMenuList, Formatter::centToEuro);


            StringBuilder headlineBuilder = new StringBuilder();
            for(String headline : headlinesList){
                headlineBuilder.append("\u2022 ").append(headline).append(":").append(System.lineSeparator());
            }
            ppStatsTodayHeadline.setText(headlineBuilder.toString());

            StringBuilder contentBuilder = new StringBuilder();
            averagePriceToday.consume(contentBuilder::append);
            contentBuilder.append(System.lineSeparator());
            medianToday.consume(contentBuilder::append);
            contentBuilder.append(System.lineSeparator());
            mostExpensiveToday.consume(contentBuilder::append);
            contentBuilder.append(System.lineSeparator());
            cheapestToday.consume(contentBuilder::append);
            contentBuilder.append(System.lineSeparator());
            sumToday.consume(contentBuilder::append);
            contentBuilder.append(System.lineSeparator());
            ppStatsTodayContent.setText(contentBuilder.toString());

            averagePriceToday.consume(stAveragePriceToday::setText);
            mostExpensiveToday.consume(stMostExpensiveToday::setText);
            cheapestToday.consume(stCheapestToday::setText);
            sumToday.consume(stSumToday::setText);
            mostPopularToday.consume(stMostPopularToday::setText);
            medianToday.consume(stMedianPriceToday::setText);
            standardDeviationToday.consume(stStandardDeviationToday::setText);
            sumOfMealsToday.consume(stSumOfMealsToday::setText);
        }

        stAveragePriceAll.setText(Analyzer.createAveragePriceAnalyzer().analyze(logDataSorted, Formatter::centToEuro).orElse("Keine Daten vorhanden"));
        stMostExpensiveAll.setText(Analyzer.createMaxPriceMealAnalyzer().analyze(logDataSorted, Meal::toString).orElse("Keine Daten vorhanden"));
        stCheapestAll.setText(Analyzer.createMinPriceMealAnalyzer().analyze(logDataSorted, Meal::toString).orElse("Keine Daten vorhanden"));
        stSumAll.setText(Analyzer.createTotalPriceAnalyzer().analyze(logDataSorted, Formatter::centToEuro).orElse("Keine Daten vorhanden"));
        stMostPopularAll.setText(Analyzer.createPopularityAnalyzer().analyze(logDataSorted, s -> s.get(0).toString()).orElse("Keine Daten vorhanden"));
        stMedianPriceAll.setText(Analyzer.createMedianPriceAnalyzer().analyze(logDataSorted, Formatter::centToEuro).orElse("Keine Daten vorhanden"));
        stSumOfMealsAll.setText(Analyzer.createAmountOfDishesAnalyzer().analyze(logDataSorted, Object::toString).orElse("Keine Daten vorhanden"));
        stStandardDeviationAll.setText(Analyzer.createStandardDeviationAnalyzer().analyze(logDataSorted, Formatter::centToEuro).orElse("Keine Daten vorhanden"));
    }

    private void getMenuByDate(LocalDate date) {
        menuToday = null;
        for (Menu menu : logDataSorted) {
            if (menu.getDate().equals(date)) {
                menuToday = menu;
            }
        }
    }

    //recursive use to dynamically alter the size of lvTableView
    private List<TableColumn<Menu, String>> createColumnPair(int index){
        List<TableColumn<Menu, String>> result = new ArrayList<>();

        TableColumn<Menu, String> mealColumn = new TableColumn<>("Gericht " + index);
        TableColumn<Menu, String> priceColumn = new TableColumn<>("Preis " + index);

        //formats the menu name cell
        mealColumn.setCellValueFactory(param -> {
            String[] meals = param.getValue().toCsvLine().split(";");

            for (int newColNr = (lvTable.getColumns().size() - 1) / 2; newColNr < meals.length - 1; newColNr++) {
                lvTable.getColumns().addAll(createColumnPair(newColNr + 1));
            }

            if (index >= meals.length) {
                return new SimpleStringProperty("");
            } else {
                String[] singleMeal = meals[index].split("_");
                return new SimpleStringProperty(singleMeal[0]);
            }
        });
        result.add(mealColumn);

        //formats the price cell
        priceColumn.setCellValueFactory(param -> {
            String[] meals = param.getValue().toCsvLine().split(";");
            if(index >= meals.length){
                return new SimpleStringProperty("");
            }else{
                String[] singleMeal = meals[index].split("_");
                int mealPriceCent = Integer.parseInt(singleMeal[1]);

                if(mealPriceCent%100 < 10){
                    return  new SimpleStringProperty(   mealPriceCent/100 + ",0" + mealPriceCent%100  + "\u20ac");
                }
                return  new SimpleStringProperty(   mealPriceCent/100 + "," + mealPriceCent%100  + "\u20ac");
            }
        });

        result.add(priceColumn);
        return result;
    }

    /*------------------------------------------------------------------------------------------------------------------------------------------
    Various other methods
    ------------------------------------------------------------------------------------------------------------------------------------------*/
    public void ppHelp(){
        ppInfo.setText("""
                 Willkommen, in diesem Tab k\u00f6nnen Sie neue Daten anfordern, diese nach Wunsch formatieren und versenden.
                 Au\u00dferdem k\u00f6nnen sie \u00fcber dieser Schaltfl\u00e4che die Statistiken und die Speisekarte f\u00fcr einen Tag sehen.
                 Versendet werden immer die Daten des Tages, der gerade ausgew\u00e4hlt ist.
                 
                 Zus\u00e4tzlich k\u00f6nnen sie hier den Namen der Datei eingeben, in die die angeforderten Daten gespeichert werden.
                 Aber Achtung, wenn sie den Namen \u00e4ndern und auf "Daten aus Datei laden" dr\u00fccken wird eine neue Datei geladen oder angelegt.
                 Die alte Datei bleibt erhalten, muss aber erneut geladen werden um auf die Daten zugreifen zu k\u00f6nnen.
                """);
    }
    public OptionalWithMessage<String> ppFormatInput(){
        Formatter formatter = ppFormatterListView.getSelectionModel().getSelectedItem();
        OptionalWithMessage<String> result;

        if(formatter == null){
            ppInfo.setText("Bitte w\u00e4hle einen Formatter aus");
            return OptionalWithMessage.ofMsg("Kein Formatter ausgew\u00e4hlt");
        }
        if(logDataSorted.isEmpty()){
            ppInfo.setText("Keine Daten vorhanden");
            return OptionalWithMessage.ofMsg("Keine Daten vorhanden");
        }
        result = formatter.format(menuToday, () -> OptionalWithMessage.of(logDataSorted));
        result.consume(ppInfo::setText);
        return result;
    }
    public void ppFormatAndSend(){
        ppFormatInput().consume(Sender.createDummySender()::send);
    }
    public void ppDatePickerChange(){
        stDatePicker.setValue(ppDatePicker.getValue());
        getMenuByDate(ppDatePicker.getValue());
        updateStatDisplay();
    }
    public void ppImportNewLogfile(){
        logfile = Path.of(ppNameOfLogfile.getText() + ".csv");
        refreshDiagrams();
        importLogfile();
        ppInfo.setText("Logdatei auf " + logfile.toString() + " ge\u00e4ndert.\nSollte die Datei bereits existieren und Daten in einem kompatiblen Format enthalten wurden diese geladen");
        updateStatDisplay();
        setupLogViewerTable();

    }
    public void fcOnChangeTo(){
        fcInfo.setText("Willkommen!\nIn diesem Tab k\u00f6nnen sie Formatter erstellen.\nW\u00e4hlen sie dazu zuerst die Art des Formatters direkt \u00fcber diesem Textfeld ");
        fcComplexSelection.setStyle(new Button().getStyle());
        fcSimpleSelection.setStyle(new Button().getStyle());
        fcFormatterFormat.setVisible(false);
        fcFormatterFormatLable.setVisible(false);
        fcFormatterHeadline.setVisible(false);
        fcFormatterHeadlineLable.setVisible(false);
        fcFormatterName.setVisible(false);
        fcSelectedAnalyzers.setVisible(false);
        fcAddAnalyzerButton.setVisible(false);
        fcSelectedAnalyzersBox.setVisible(false);
        fcAvailableAnalyzersBox.setVisible(false);
        fcFormatterNameLable.setVisible(false);
    }
    public void fcRemoveAnalyzer(){
        selectedAnalyzerList.remove(fcSelectedAnalyzers.getSelectionModel().getSelectedItem());
    }
    public void fcAddAnalyzer(){
        if(fcAvailableAnalyzers.getSelectionModel().getSelectedItem() != null){
            selectedAnalyzerList.add(fcAvailableAnalyzers.getSelectionModel().getSelectedItem());
        }
    }
    public void fcChangeToSimple(){
        fcGenerateComplex = false;
        fcComplexSelection.setStyle(new Button().getStyle());
        fcSimpleSelection.setStyle("-fx-background-color: #949191;" );
        fcInfo.setText("""
                Sie haben sich f\u00fcr den einfachen Formatter entschieden.
                
                Dieser formattiert die Ausgabe eines einzelnen Analyzers mit einer Headline.
                
                Geben sie nun die gew\u00fcnschten Parameter an, anschlie\u00dfend k\u00f6nnen sie den Vorgang mit dem Button "Erstellen" abschlie\u00dfen""");
        fcFormatterFormat.setVisible(false);
        fcFormatterFormatLable.setVisible(false);
        fcFormatterHeadline.setVisible(true);
        fcFormatterHeadlineLable.setVisible(true);
        fcFormatterName.setVisible(true);
        fcSelectedAnalyzers.setVisible(true);
        fcAddAnalyzerButton.setVisible(false);
        fcSelectedAnalyzersBox.setVisible(false);
        fcAvailableAnalyzersBox.setVisible(true);
        fcFormatterNameLable.setVisible(true);
    }
    public void fcChangeToComplex(){
        fcGenerateComplex = true;
        fcSimpleSelection.setStyle(new Button().getStyle());
        fcComplexSelection.setStyle("-fx-background-color: #949191;" );
        fcInfo.setText("""
                Sie haben sich f\u00fcr den komplexen Formatter entschieden.
                
                Dieser formattiert die Ausgabe mehrerer Analyzer in einem vorgegebenen Format.
                
                Geben sie nun die gew\u00fcnschten Parameter an, anschlie\u00dfend k\u00f6nnen sie den Vorgang mit dem Button "Erstellen" abschlie\u00dfen""");
        fcFormatterFormat.setVisible(true);
        fcFormatterFormatLable.setVisible(true);
        fcFormatterHeadline.setVisible(false);
        fcFormatterHeadlineLable.setVisible(false);
        fcFormatterName.setVisible(true);
        fcSelectedAnalyzers.setVisible(true);
        fcAddAnalyzerButton.setVisible(true);
        fcSelectedAnalyzersBox.setVisible(true);
        fcAvailableAnalyzersBox.setVisible(true);
        fcFormatterNameLable.setVisible(true);
    }
    public void fcCreateFormatter(){
        if(fcGenerateComplex){
            if(fcFormatterName.getText().isBlank()){
                fcInfo.setText("Bitte geben Sie einen Namen ein");
            } else if(fcFormatterFormat.getText().isBlank()) {
                fcInfo.setText("Bitte geben Sie das gew\u00fcnschte Format ein");
            } else if(!fcFormatterFormat.getText().contains("$")){
                fcInfo.setText("Das Format muss '$' Zeichen enthalten");
            } else if(selectedAnalyzerList.stream().toList().size() != fcFormatterFormat.getText().chars().filter(c -> c == '$').count()){
                fcInfo.setText("Die Zahl der '$' Zeichen muss mit der Zahl der gew\u00e4hlten Analyzer \u00fcbereinstimmen");
            } else {
                formatterList.add(Formatter.createFormatterFromFormat(fcFormatterFormat.getText(), selectedAnalyzerList.stream().toList(), fcFormatterName.getText()));
                fcInfo.setText("Ihr Formatter wurde erfolgreich erstellt!");
            }
        } else {
            if(fcFormatterName.getText().isBlank()){
                fcInfo.setText("Bitte geben Sie einen Namen ein");
            } else if(fcFormatterHeadline.getText().isBlank()){
                fcInfo.setText("Bitte geben Sie eine Headline ein");
            } else if(fcAvailableAnalyzers.getSelectionModel().getSelectedItem() == null){
                fcInfo.setText("Bitte w\u00e4hlen sie einen Analyzer an");
            } else{
                formatterList.add(Formatter.createFormatterFromAnalyzer(fcFormatterHeadline.getText(),fcAvailableAnalyzers.getSelectionModel().getSelectedItem(), fcFormatterName.getText()));
                fcInfo.setText("Ihr Formatter wurde erfolgreich erstellt!");
            }
        }
    }
    public void stDatePickerChange(){
        ppDatePicker.setValue(stDatePicker.getValue());
        getMenuByDate(stDatePicker.getValue());
        updateStatDisplay();

    }
    public void stOpenAll(){
        stAveragePriceTitle.setExpanded(true);
        stMostExpensiveTitle.setExpanded(true);
        stCheapestTitle.setExpanded(true);
        stSumTitle.setExpanded(true);
        stMostPopularTitle.setExpanded(true);
        stStandardDeviation.setExpanded(true);
        stSumOfMeals.setExpanded(true);
        stMedianPrice.setExpanded(true);
    }
    public void stCloseAll(){
        stAveragePriceTitle.setExpanded(false);
        stMostExpensiveTitle.setExpanded(false);
        stCheapestTitle.setExpanded(false);
        stSumTitle.setExpanded(false);
        stMostPopularTitle.setExpanded(false);
        stStandardDeviation.setExpanded(false);
        stSumOfMeals.setExpanded(false);
        stMedianPrice.setExpanded(false);
    }
    public void lvSearchInTable(){
        String searchValue = lvSearchBar.getText();
        List<int[]> matches = new ArrayList<>();
        lvNumberOfResults.setVisible(true);

        //make sure search value for price matches pattern
        if(searchValue.matches("[0-9]*,[0-9]{0,2}[\u20ac]?")){
            searchValue = searchValue.replace("\u20ac", "");
            searchValue = searchValue.replace(",", "");
        }

        //filter log data for results and write into matches
        for(int i = 0; i < logDataSorted.size(); i++){
            String[] line = logDataSorted.get(i).toCsvLine().split(";");
            for(int j = 1; j < line.length; j++){
                if(line[j].toLowerCase().contains(searchValue.toLowerCase())){
                    matches.add(new int[]{i, j});
                }
            }
        }

        if(!matches.isEmpty()){
            //select first match
            lvTable.scrollTo(matches.get(0)[0]);
            lvTable.scrollToColumn(lvTable.getColumns().get(matches.get(0)[1]*2-1));
            lvTable.getSelectionModel().select(matches.get(0)[0]);

            lvNumberOfResults.setText("Es wurden " + matches.size() + " passende Eintr\u00e4ge gefunden");

            //set up Spinner for result selection
            lvResultSelector.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0,matches.size()-1, 0));
            lvResultSelector.getValueFactory().wrapAroundProperty().set(true);
            lvResultSelector.setVisible(true);
            lvResultSelector.valueProperty().addListener((observable, oldValue, newValue) -> {
                if(newValue == -1) return;
                int [] result = matches.get(newValue);
                lvTable.scrollTo(result[0]);
                lvTable.scrollToColumn(lvTable.getColumns().get(result[1]*2-1));
                lvTable.getSelectionModel().select(result[0]);
            });
        } else {
            lvNumberOfResults.setText("Leider wurden keine passenden Eintr\u00e4ge gefunden");
            lvResultSelector.setVisible(false);
            lvTable.getSelectionModel().clearSelection();
        }
    }
}



