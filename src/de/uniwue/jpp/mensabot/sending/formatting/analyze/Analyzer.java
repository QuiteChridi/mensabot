package de.uniwue.jpp.mensabot.sending.formatting.analyze;

import de.uniwue.jpp.mensabot.dataclasses.Meal;
import de.uniwue.jpp.mensabot.dataclasses.Menu;
import de.uniwue.jpp.errorhandling.OptionalWithMessage;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/*-------------------------------------------------------------------------------------------------------------------------------------
Each analyzer takes a list of menus as injput value and performs a certain type of analysis on the given data

Includes the following analyzers:
- AveragePriceAnalyzer:         returns the arithmetic mean price of all meals
- MedianPriceAnalyzer:          returns the median price of all meals
- MinPriceMealAnalyzer:         returns the cheapest meal
- MaxPriceMealAnalyzer:         returns the most expensive meal
- TotalPriceAnalyzer:           returns the sum of all meal prices
- AveragePricePerDayAnalyzer:   returns the avarage price for each day
- TotalPricePerDayAnalyzer:     returns the sum of all prices for each day
- PopularityAnalyzer:           returns a list of all meals sorted by popularity descending
- AmountOfDishesPerDayAnalyzer: returns the amount of different meals for each day
- AmountOfDishesAnalyzer:       returns the total amount of dishes
- PriceRangeAnalyzer:           returns the meals categorized in a specified amount price categories of specified size
- StandardDeviationAnalyzer:    returns the standard deviation of the prices
-------------------------------------------------------------------------------------------------------------------------------------*/


public interface Analyzer<T> {
    OptionalWithMessage<T> analyze_unsafe(List<Menu> data);

    default OptionalWithMessage<T> analyze(List<Menu> data) {
        if(data == null || data.isEmpty()){
            return OptionalWithMessage.ofMsg("Invalid data argument!");
        }
        return analyze_unsafe(data);
    }

    default OptionalWithMessage<String> analyze(List<Menu> data, Function<T, String> convert) {
        if(convert == null){
            return OptionalWithMessage.ofMsg("No convert-function given!");
        }
        return analyze(data).map(convert);
    }

    static private List<Meal> getMealListSortedByPrice(List<Menu> data){
        return data
                .stream()
                .flatMap(s -> s.getMeals().stream())
                .sorted(Comparator.comparingInt(Meal::getPriceInCent))
                .toList();
    }

    static Analyzer<Integer> createAveragePriceAnalyzer() {
        return new Analyzer<>(){
            @Override
            public OptionalWithMessage<Integer> analyze_unsafe(List<Menu> data) {
                List<Meal> mealListSorted = Analyzer.getMealListSortedByPrice(data);
                int average = mealListSorted
                        .stream()
                        .mapToInt(Meal::getPriceInCent)
                        .sum();
                return OptionalWithMessage.of(average/mealListSorted.size());
            }

            @Override
            public String toString(){
                return "AveragePriceAnalyzer";
            }
        };
    }


    static Analyzer<Integer> createMedianPriceAnalyzer() {
        return new Analyzer<>(){
            @Override
            public OptionalWithMessage<Integer> analyze_unsafe(List<Menu> data) {
                List<Meal> mealListSorted = Analyzer.getMealListSortedByPrice(data);
                return OptionalWithMessage.of(mealListSorted.get((mealListSorted.size()-1)/2).getPriceInCent());
            }

            @Override
            public String toString(){
                return "MedianPriceAnalyzer";
            }
        };
    }

    static Analyzer<Meal> createMinPriceMealAnalyzer() {
        return new Analyzer<>(){
            @Override
            public OptionalWithMessage<Meal> analyze_unsafe(List<Menu> data) {
                return OptionalWithMessage.of(Analyzer.getMealListSortedByPrice(data).get(0));
            }

            @Override
            public String toString(){
                return "MinPriceMealAnalyzer";
            }
        };
    }

    static Analyzer<Meal> createMaxPriceMealAnalyzer() {
        return new Analyzer<>(){
            @Override
            public OptionalWithMessage<Meal> analyze_unsafe(List<Menu> data) {
                List<Meal> mealListSorted = Analyzer.getMealListSortedByPrice(data);
                return OptionalWithMessage.of(mealListSorted.get(mealListSorted.size()-1));
            }

            @Override
            public String toString(){
                return "MaxPriceMealAnalyzer";
            }
        };
    }

    static Analyzer<Integer> createTotalPriceAnalyzer() {
        return new Analyzer<>(){
            @Override
            public OptionalWithMessage<Integer> analyze_unsafe(List<Menu> data) {
                return OptionalWithMessage.of(
                        Analyzer.getMealListSortedByPrice(data)
                                .stream()
                                .mapToInt(Meal::getPriceInCent)
                                .sum()
                );
            }

            @Override
            public String toString(){
                return "TotalPriceAnalyzer";
            }
        };
    }



    static Analyzer<Map<LocalDate, Double>> createAveragePricePerDayAnalyzer() {
        return new Analyzer<>(){
            @Override
            public OptionalWithMessage<Map<LocalDate, Double>> analyze_unsafe(List<Menu> data) {
                Map<LocalDate, Double> result = new HashMap<>();

                for(Menu menu : data){
                    result.put(menu.getDate(), menu.getMeals().stream().mapToDouble(Meal::getPriceInCent).sum()/menu.getMeals().size());
                }
                return OptionalWithMessage.of(result);
            }

            @Override
            public String toString(){
                return "AveragePricePerDayAnalyzer";
            }
        };
    }

    static Analyzer<Map<LocalDate, Double>> createTotalPricePerDayAnalyzer() {
        return new Analyzer<>(){
            @Override
            public OptionalWithMessage<Map<LocalDate, Double>> analyze_unsafe(List<Menu> data) {
                Map<LocalDate, Double> result = new HashMap<>();

                for(Menu menu : data){
                    result.put(menu.getDate(), menu.getMeals().stream().mapToDouble(Meal::getPriceInCent).sum());
                }
                return OptionalWithMessage.of(result);
            }

            @Override
            public String toString(){
                return "TotalPricePerDayAnalyzer";
            }
        };
    }

    static Analyzer<List<Meal>> createPopularityAnalyzer() {
        return new Analyzer<>(){
            @Override
            public OptionalWithMessage<List<Meal>> analyze_unsafe(List<Menu> data) {
                HashMap<Meal, Integer> occurences = new HashMap<>();
                List<Meal> mealList = data
                        .stream()
                        .flatMap(s -> s.getMeals().stream())
                        .toList();

                for(Meal meal : mealList){
                    if(occurences.containsKey(meal)){
                        occurences.replace(meal, occurences.get(meal)+1);
                    }else{
                        occurences.put(meal, 1);
                    }
                }

                return OptionalWithMessage.of(occurences
                        .keySet()
                        .stream()
                        .sorted(Comparator
                                .comparingInt(occurences::get))
                        .toList());
            }

            @Override
            public String toString(){
                return "PopularityAnalyzer";
            }
        };
    }


    static Analyzer<Map<LocalDate, Integer>> createAmountOfDishesPerDayAnalyzer() {
        return new Analyzer<>(){
            @Override
            public OptionalWithMessage<Map<LocalDate, Integer>> analyze_unsafe(List<Menu> data) {
                return OptionalWithMessage.of(data
                        .stream()
                        .collect(Collectors.toMap(Menu::getDate, s -> s.getMeals().size())));
            }

            @Override
            public String toString(){
                return "AmountOfDishesPerDayAnalyzer";
            }
        };
    }

    static Analyzer<Integer> createAmountOfDishesAnalyzer() {
        return new Analyzer<>(){
            @Override
            public OptionalWithMessage<Integer> analyze_unsafe(List<Menu> data) {
                return OptionalWithMessage.of(data
                        .stream().mapToInt(s -> s.getMeals().size())
                        .sum());
            }

            @Override
            public String toString(){
                return "AmountOfDishesAnalyzer";
            }
        };
    }

    //returns the standard deviation of meal prices grouped into days
    static Analyzer<Double> createStandardDeviationAnalyzer() {
        return new Analyzer<>(){
            @Override
            public OptionalWithMessage<Double> analyze_unsafe(List<Menu> data) {
                OptionalWithMessage<Integer> sum = Analyzer.createAmountOfDishesAnalyzer().analyze(data);
                OptionalWithMessage<Integer> mean = Analyzer.createAveragePriceAnalyzer().analyze(data);
                 double var = data.stream()
                         .flatMap(s -> s.getMeals().stream())
                         .mapToDouble(Meal::getPriceInCent)
                         .map(s -> (s - mean.get()))
                         .map(s -> Math.pow(s,2))
                         .sum()/sum.get();
                 return OptionalWithMessage.of(Math.sqrt(var));
            }

            @Override
            public String toString(){
                return "StandardDeviationAnalyzer";
            }
        };
    }

    //returns an ArrayList with size+1 categories, last category being for prices higher than numberOfCategories * sizeCent
    static Analyzer<List<Integer>> createPriceRangeAnalyzer(int numberOfCategories, int sizeCent) {
        return new Analyzer<>(){
            @Override
            public OptionalWithMessage<List<Integer>> analyze_unsafe(List<Menu> data) {
                Integer [] result = new Integer [numberOfCategories+1];
                Arrays.fill(result, 0);
                List<Meal> mealList = data
                        .stream()
                        .flatMap(s -> s.getMeals().stream())
                        .toList();

                for(Meal meal : mealList){
                    if(meal.getPriceInCent()/sizeCent >= numberOfCategories){
                        result[numberOfCategories]++;
                    } else {
                        result[meal.getPriceInCent()/sizeCent]++;
                    }
                }
                return OptionalWithMessage.of(Arrays.asList(result));
            }

            @Override
            public String toString(){
                return "PriceRangeAnalyzer";
            }
        };
    }
}
