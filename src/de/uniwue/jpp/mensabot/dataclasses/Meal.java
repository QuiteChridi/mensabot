package de.uniwue.jpp.mensabot.dataclasses;

public interface Meal {

    String getName();

    int getPriceInCent();

    String getPriceInEuros();


    static Meal createMeal(String name, int price) {
        if(name == null) throw new IllegalArgumentException("Es muss ein Name übergeben werden");
        if(name.contains(";") || name.contains("_")) throw new IllegalArgumentException("Der Name des Gerichtes darf die Zeichen \"_\" und \";\" nicht enthalten!");
        if (name.isBlank()) throw new IllegalArgumentException("Der Name des Gerichtes darf nicht leer sein!");
        if(price < 0) throw new IllegalArgumentException("Der Preis darf nicht negativ sein!");

        return new Meal(){
            private final String mealName = name;
            private final int mealPriceCent = price;

            @Override
            public String getName() {
                return mealName;
            }

            @Override
            public int getPriceInCent() {
                return mealPriceCent;
            }

            public String getPriceInEuros() {
                if(mealPriceCent%100 < 10){
                    return mealPriceCent/100 + ",0" + mealPriceCent%100  + "\u20ac";
                }
                return  mealPriceCent/100 + "," + mealPriceCent%100  + "\u20ac";
            }

            @Override
            public String toString(){
                return  mealName + " (" + getPriceInEuros() +")";
            }

            @Override
            public boolean equals(Object o){
                if(!(o instanceof Meal)){
                    return false;
                }
                return this.hashCode() == o.hashCode();
            }

            @Override
            public int hashCode(){
                return mealName.hashCode() + mealPriceCent;
            }
        };
    }
}
