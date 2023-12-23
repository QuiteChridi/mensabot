package de.uniwue.jpp.mensabot.retrieval;

import de.uniwue.jpp.mensabot.dataclasses.Meal;
import de.uniwue.jpp.mensabot.dataclasses.Menu;
import de.uniwue.jpp.errorhandling.OptionalWithMessage;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Set;

public interface Parser {

    OptionalWithMessage<Menu> parse(String fetched);

    //creates a new parser for meals in csv format, note that the String musst be formated as following
    //YYYY-MM-DD;<MealName1>_<Price>;<MealName2>_<Price>
    static Parser createCsvParser() {
        return new Parser() {
            @Override
            public OptionalWithMessage<Menu> parse(String fetched) {
                Set<Meal> mealSet = new HashSet<>();
                LocalDate date;

                if(!(fetched.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}(;[^_;]*_[0-9]+)*"))){
                    return OptionalWithMessage.ofMsg("Input does not match! Input was: '" + fetched +"'");
                }

                String[] input = fetched.split(";");
                String[] singleMeal;

                //checks if date is valid and parses
                try{
                    date = LocalDate.parse(input[0]);
                } catch (DateTimeParseException dtpe){
                    return OptionalWithMessage.ofMsg("Invalid date");
                }

                for(int i = 1; i < input.length; i++){
                    singleMeal = input[i].split("_");
                    mealSet.add(Meal.createMeal(singleMeal[0], Integer.parseInt(singleMeal[1])));
                }

                return OptionalWithMessage.of(Menu.createMenu(date, mealSet));
            }
        };
    }
}
