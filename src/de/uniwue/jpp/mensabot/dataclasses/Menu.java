package de.uniwue.jpp.mensabot.dataclasses;

import java.time.LocalDate;
import java.util.*;

public interface Menu {

    LocalDate getDate();
    Set<Meal> getMeals();
    String toCsvLine();

    static Menu createMenu(LocalDate date, Set<Meal> meals) {
        if(date == null || meals == null) throw new IllegalArgumentException("Es muss ein Datum und ein Set an Gerichten angegeben werden");
        if(meals.isEmpty()) throw new IllegalArgumentException("Dass Menü muss mindestens ein Gericht enthalten");
        if(meals.stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException("Im Menü darf kein undefiniertes Gericht enthalten sein");

        return new Menu(){
            private final LocalDate menuDate = date;
            private final Set<Meal> menuMeals = meals;

            @Override
            public LocalDate getDate() {
                return menuDate;
            }

            @Override
            public Set<Meal> getMeals() {
                return Collections.unmodifiableSet(menuMeals);
            }

            @Override
            public String toCsvLine() {
                StringBuilder menuCSV = new StringBuilder();
                menuCSV.append(menuDate.toString());
                for(Meal m : menuMeals){
                    menuCSV.append(";").append(m.getName()).append("_").append(m.getPriceInCent());
                }
                return menuCSV.toString();
            }

            @Override
            public String toString(){
                return toCsvLine();
            }

            @Override
            public boolean equals(Object o){
                if(!(o instanceof Menu)){
                    return false;
                }
                return this.hashCode() == o.hashCode();
            }

            @Override
            public int hashCode(){
                return getDate().hashCode();
            }
        };
    }
}
