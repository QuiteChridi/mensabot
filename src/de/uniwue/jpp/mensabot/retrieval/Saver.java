package de.uniwue.jpp.mensabot.retrieval;

import de.uniwue.jpp.errorhandling.OptionalWithMessage;
import de.uniwue.jpp.mensabot.dataclasses.Menu;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public interface Saver {

    Optional<String> log(Path path, Menu newMenu);

    static Saver createCsvSaver() {
        return new Saver() {
            @Override
            public Optional<String> log(Path path, Menu newMenu) {
                try {
                    if (!(Files.exists(path))) {
                        Files.createFile(path);
                    }
                    List<String> logData;
                    BufferedReader reader = new BufferedReader(new FileReader(path.toFile()));
                    logData = reader.lines().toList();
                    reader.close();

                    if (!logData.isEmpty()) {
                        OptionalWithMessage<Menu> firstLog = Parser.createCsvParser().parse(logData.get(0));
                        if (firstLog.isEmpty()) {
                            return Optional.of("Latest log entry is invalid");
                        }
                        if ((!firstLog.get().getDate().isBefore(newMenu.getDate()))) {
                            return Optional.of("Date of new entry is older than date of last entry - not writing to log.");
                        }
                    }

                    BufferedWriter writer = new BufferedWriter(new FileWriter(path.toFile()));
                    writer.write(newMenu.toCsvLine() + "\n");
                    writer.write(String.join("\n", logData));
                    writer.close();

                } catch (IOException e) {
                    return Optional.of(e.toString());
                }
                return Optional.empty();
            }
        };
    }
}
