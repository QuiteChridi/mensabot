package de.uniwue.jpp.mensabot.sending;

import de.uniwue.jpp.mensabot.dataclasses.Menu;
import de.uniwue.jpp.errorhandling.OptionalWithMessage;
import de.uniwue.jpp.mensabot.retrieval.Parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

public interface Importer {

    OptionalWithMessage<Menu> getLatest(BufferedReader fileReader);
    OptionalWithMessage<List<Menu>> getAll(BufferedReader fileReader);

    static Importer createCsvImporter() {
        return new Importer() {
            @Override
            public OptionalWithMessage<Menu> getLatest(BufferedReader fileReader) {
                try {
                    return Parser.createCsvParser().parse(fileReader.readLine());
                } catch (IOException e) {
                    return OptionalWithMessage.ofMsg("Import failure - File could not be read");
                }
            }

            @Override
            public OptionalWithMessage<List<Menu>> getAll(BufferedReader fileReader) {
                    return OptionalWithMessage.sequence(fileReader.lines().map(s -> Parser.createCsvParser().parse(s)).toList());
            }
        };
    }
}
