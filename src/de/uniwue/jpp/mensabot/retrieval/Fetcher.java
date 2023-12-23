package de.uniwue.jpp.mensabot.retrieval;

import de.uniwue.jpp.errorhandling.OptionalWithMessage;
import de.uniwue.jpp.mensabot.dataclasses.Menu;
import de.uniwue.jpp.mensabot.util.MensabotUtil;

import java.net.http.HttpClient;
import java.time.LocalDate;
public interface Fetcher {

    OptionalWithMessage<String> fetchCurrentData();

    //returns random menus for each date starting today
    static Fetcher createDummyCsvFetcher() {
        return new Fetcher() {
            static LocalDate date = LocalDate.now();
            @Override
            public OptionalWithMessage<String> fetchCurrentData() {
                Menu sampleMenu = Menu.createMenu(date, MensabotUtil.createRandomMealSet());
                date = date.plusDays(1);
                return OptionalWithMessage.of(sampleMenu.toCsvLine());
            }
        };
    }
}

