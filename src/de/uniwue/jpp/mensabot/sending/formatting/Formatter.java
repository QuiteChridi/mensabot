package de.uniwue.jpp.mensabot.sending.formatting;

import de.uniwue.jpp.mensabot.dataclasses.Meal;
import de.uniwue.jpp.mensabot.dataclasses.Menu;
import de.uniwue.jpp.errorhandling.OptionalWithMessage;
import de.uniwue.jpp.mensabot.sending.formatting.analyze.Analyzer;

import java.text.Format;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
/*-------------------------------------------------------------------------------------------------------------------------------------
Each formatter takes a single menu and a supplier to a menu list and formats either the singele menu or the whole list in a certain way
depending on the formatter

The following formatters format the latest menu:
- SimpleFormatter:      formats the date as headline and one meal per line with name and price, optionally a String can be passed to be put aside the date
- SimpleMealFormatter:  one meal per line, with name and price
- HiddenFormatter:      one meal per line, just the price without the name
- ShortFormatter:       one meal per line, shortens the name to show just the first letters of the words, shows price
- FirstWordFormatter:   one meal per line, first word of the name and price
- PricelessFormatter:   one meal per line, just the name

The following formatters format all menus:
- FormatterFromAnalyzer:    analyzes all meals with a given analyzer and formats the result with a given headline, a custom name can be passed
- ComplexFormatter:         analyzes all meals with a set of analyzers and formats the result with a set of headliness
- FormatterFromFormat:      analyzes all meals with a set of analyzers and wraps the results in a given format, $ are used to signal where the
                            result of the analyzers shall be put
- SimpleTotalFormatter:     formats all meals of all menus according to the SimpleMealFormatter
-------------------------------------------------------------------------------------------------------------------------------------*/

public interface Formatter {
    OptionalWithMessage<String> format(Menu latestMenu, Supplier<OptionalWithMessage<List<Menu>>> allMenus);
    static Formatter createSimpleFormatter()  {
        return new Formatter() {
            @Override
            public OptionalWithMessage<String> format(Menu latestMenu, Supplier<OptionalWithMessage<List<Menu>>> allMenus) {
                if(latestMenu == null || allMenus == null) throw  new NullPointerException("At least one of the arguments was null");

                String output = "Essen am " + latestMenu.getDate().toString() + ":" + System.lineSeparator()
                        + latestMenu.getMeals().stream().map(Meal::toString).collect(Collectors.joining(System.lineSeparator()));
                return OptionalWithMessage.of(output);
            }

            @Override
            public String toString() {
                return "SimpleFormatter";
            }
        };
    }

    static public String centToEuro(int centValue){
        if(centValue%100 < 10){
            return centValue/100 + ",0" + centValue%100  + "\u20ac";
        }
        return  centValue/100 + "," + centValue%100  + "\u20ac";
    }

    static public String centToEuro(double centValue){
        return centToEuro( (int) Math.round(centValue));
    }

    static Formatter createSimpleFormatter(String info) {
        return new Formatter() {
            @Override
            public OptionalWithMessage<String> format(Menu latestMenu, Supplier<OptionalWithMessage<List<Menu>>> allMenus) {
                if(latestMenu == null || allMenus == null) throw  new NullPointerException("At least one of the arguments was null");

                String output = "Essen am " + latestMenu.getDate().toString() + " (" + info + ")" + ":" + System.lineSeparator()
                        + latestMenu.getMeals().stream().map(Meal::toString).collect(Collectors.joining(System.lineSeparator()));
                return OptionalWithMessage.of(output);
            }

            @Override
            public String toString() {
                return "SimpleFormatter";
            }
        };
    }

    //formats all meals inside of a Menu
    static Formatter createSimpleMealFormatter() {
        return new Formatter() {
            @Override
            public OptionalWithMessage<String> format(Menu latestMenu, Supplier<OptionalWithMessage<List<Menu>>> allMenus) {
                if(latestMenu == null || allMenus == null) throw  new NullPointerException("At least one of the arguments was null");

                return OptionalWithMessage.of(latestMenu.getMeals().stream().map(Meal::toString).collect(Collectors.joining(System.lineSeparator())));
            }

            @Override
            public String toString() {
                return "SimpleMealFormatter";
            }
        };
    }

    //Analyzes all meals of a menu with a given analyzer and formats the result with a given Headline
    static Formatter createFormatterFromAnalyzer(String headline, Analyzer<?> analyzer) {
        if(headline == null || headline.isBlank() || analyzer == null){
            throw new IllegalArgumentException("Illegal argument!");
        }
        return new Formatter() {
            @Override
            public OptionalWithMessage<String> format(Menu latestMenu, Supplier<OptionalWithMessage<List<Menu>>> allMenus) {
                if(latestMenu == null || allMenus == null) throw  new NullPointerException("At least one of the arguments was null");

                return OptionalWithMessage.of(
                        headline
                                + ":"
                                + allMenus
                                .get()
                                .flatMap(analyzer::analyze)
                                .map(Object::toString)
                                .orElse("Analyzing is not possible")
                                + System.lineSeparator());
            }

            @Override
            public String toString() {
                return "FormatterFromAnalyzer";
            }
        };
    }

    //Analyzes all meals of a menu with a given analyzer and formats the result with a given Headline
    static Formatter createFormatterFromAnalyzer(String headline, Analyzer<?> analyzer, String name) {
        if(headline == null || headline.isBlank() || analyzer == null || name == null || name.isBlank()){
            throw new IllegalArgumentException("Illegal argument!");
        }
        return new Formatter() {
            @Override
            public OptionalWithMessage<String> format(Menu latestMenu, Supplier<OptionalWithMessage<List<Menu>>> allMenus) {
                if(latestMenu == null || allMenus == null) throw  new NullPointerException("At least one of the arguments was null");

                return OptionalWithMessage.of(
                        headline
                                + ":"
                                + allMenus
                                .get()
                                .flatMap(analyzer::analyze)
                                .map(Object::toString)
                                .orElse("Analyzing is not possible")
                                + System.lineSeparator());
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }

    static Formatter createComplexFormatter(List<String> headlines, List<Analyzer<?>> analyzers) {
        if(headlines == null || headlines.isEmpty()|| analyzers == null || analyzers.isEmpty()) throw new IllegalArgumentException("Illegal argument!");
        if(headlines.size() != analyzers.size()) throw new IllegalArgumentException("There must be a headline for each analyzer!");

        return new Formatter() {
            @Override
            public OptionalWithMessage<String> format(Menu latestMenu, Supplier<OptionalWithMessage<List<Menu>>> allMenus) {
                if(latestMenu == null || allMenus == null) throw  new NullPointerException("At least one of the arguments was null");
                OptionalWithMessage<List<Menu>> allMenuList = allMenus.get();

                StringBuilder output = new StringBuilder();
                for(int i = 0; i < headlines.size(); i++){
                    output.append(headlines.get(i)).append(":").append(allMenuList
                            .flatMap(analyzers.get(i)::analyze)
                            .map(Object::toString)
                            .orElse("Analyzing is not possible"))
                            .append(System.lineSeparator());
                }
                return OptionalWithMessage.of(output.toString());
            }

            @Override
            public String toString() {
                return "ComplexFormatter";
            }
        };
    }

    //Fills the blanks of a given format with the analyzed results of the given Menus
    static Formatter createFormatterFromFormat(String format, List<Analyzer<?>> analyzers, String name) {
        if(name == null || name.isEmpty()) throw new IllegalArgumentException("Illegal name argument!");
        if(format == null || format.isEmpty()) throw new IllegalArgumentException("Illegal format argument!");
        if(!format.contains("$")) throw new IllegalArgumentException("Format must contain $ signs!");
        if(analyzers == null || analyzers.isEmpty()) throw new IllegalArgumentException("Illegal analyzer argument!");
        if(analyzers.size() != format.chars().filter(c -> c == '$').count()) throw new IllegalArgumentException("There must be a $ for each analyzer");

        return new Formatter() {
            @Override
            public OptionalWithMessage<String> format(Menu latestMenu, Supplier<OptionalWithMessage<List<Menu>>> allMenus) {
                if(latestMenu == null || allMenus == null) throw  new NullPointerException("At least one of the arguments was null");

                List<String> formatParts = Arrays.stream(format.split("[$]")).collect(Collectors.toList());
                while(formatParts.size() < analyzers.size()){
                    formatParts.add("");
                }
                StringBuilder output = new StringBuilder();
                OptionalWithMessage<List<Menu>> allMenuList = allMenus.get();

                for(int i = 0; i < analyzers.size(); i++){
                    output.append(formatParts.get(i));
                    output.append(allMenuList.flatMap(analyzers.get(i)::analyze).map(Object::toString).orElse("Analyzing is not possible"));
                }
                if(formatParts.size() > analyzers.size()){
                    output.append(formatParts.get(formatParts.size()-1));
                }

                return OptionalWithMessage.of(output.toString());
            }

            @Override
            public String toString() {
                return name;
            }
        };
    }

    static Formatter createHiddenFormatter() {
        return new Formatter() {
            @Override
            public OptionalWithMessage<String> format(Menu latestMenu, Supplier<OptionalWithMessage<List<Menu>>> allMenus) {
                if(latestMenu == null || allMenus == null) throw  new NullPointerException("At least one of the arguments was null");

                return OptionalWithMessage.of(latestMenu
                        .getMeals()
                        .stream()
                        .map(Meal::getPriceInEuros)
                        .collect(Collectors.joining(System.lineSeparator())));
            }
            @Override
            public String toString() {
                return "HiddenFormatter";
            }
        };
    }

    static Formatter createShortFormatter() {
        return new Formatter() {
            @Override
            public OptionalWithMessage<String> format(Menu latestMenu, Supplier<OptionalWithMessage<List<Menu>>> allMenus) {
                if(latestMenu == null || allMenus == null) throw  new NullPointerException("At least one of the arguments was null");

                StringBuilder result = new StringBuilder();
                String[] singleWords;

                for(Meal meal : latestMenu.getMeals()) {
                    singleWords = meal.getName().split(" ");
                    for (String word : singleWords) {
                        result.append(word.charAt(0));
                    }
                    result.append(" (").append(meal.getPriceInEuros()).append(")").append(System.lineSeparator());
                }
                result.deleteCharAt(result.length()-1);


                return OptionalWithMessage.of(result.toString());
            }
            @Override
            public String toString() {
                return "ShortFormatter";
            }
        };
    }


    static Formatter createFirstWordFormatter() {
        return new Formatter() {
            @Override
            public OptionalWithMessage<String> format(Menu latestMenu, Supplier<OptionalWithMessage<List<Menu>>> allMenus) {
                if(latestMenu == null || allMenus == null) throw  new NullPointerException("At least one of the arguments was null");

                StringBuilder result = new StringBuilder();

                for(Meal meal : latestMenu.getMeals()) {
                    result.append(meal.getName().split(" ")[0]).append(" (");
                    result.append((meal.getPriceInEuros())).append(")").append(System.lineSeparator());
                }
                result.deleteCharAt(result.length()-1);

                return OptionalWithMessage.of(result.toString());
            }
            @Override
            public String toString() {
                return "FirstWordFormatter";
            }
        };
    }

    static Formatter createPricelessFormatter() {
        return new Formatter() {
            @Override
            public OptionalWithMessage<String> format(Menu latestMenu, Supplier<OptionalWithMessage<List<Menu>>> allMenus) {
                if(latestMenu == null || allMenus == null) throw  new NullPointerException("At least one of the arguments was null");
                return OptionalWithMessage.of(latestMenu
                        .getMeals()
                        .stream()
                        .map(Meal::getName)
                        .collect(Collectors.joining(System.lineSeparator())));
            }
            @Override
            public String toString() {
                return "PricelessFormatter";
            }
        };
    }

    static Formatter createSimpleTotalFormatter() {
        return new Formatter() {
            @Override
            public OptionalWithMessage<String> format(Menu latestMenu, Supplier<OptionalWithMessage<List<Menu>>> allMenus) {
                if(latestMenu == null || allMenus == null) throw  new NullPointerException("At least one of the arguments was null");
                return  allMenus
                        .get()
                        .map(List::stream)
                        .map(stream -> stream
                                .flatMap(menu -> menu
                                        .getMeals()
                                        .stream()
                                        .map(Meal::toString))
                                .collect(Collectors.joining(System.lineSeparator())));

            }

            public String toString() {
                return "SimpleTotalFormatter";
            }
        };
    }
}
