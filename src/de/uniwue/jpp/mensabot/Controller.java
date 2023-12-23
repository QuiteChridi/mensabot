package de.uniwue.jpp.mensabot;

import de.uniwue.jpp.mensabot.retrieval.Fetcher;
import de.uniwue.jpp.mensabot.retrieval.Parser;
import de.uniwue.jpp.mensabot.retrieval.Saver;
import de.uniwue.jpp.mensabot.sending.Importer;
import de.uniwue.jpp.mensabot.sending.Sender;
import de.uniwue.jpp.mensabot.sending.formatting.Formatter;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.Optional;


public interface Controller {

    Optional<String> retrieveData();
    Optional<String> send(Formatter formatter);

    static Controller create(Fetcher f, Parser p, Saver sav, Path logfile, Importer i, Sender s) {
        if(f == null || p == null || sav == null ||logfile == null ||i == null ||s == null) throw new NullPointerException("At least one of the given parameters was null");
        return new Controller() {
            @Override
            public Optional<String> retrieveData() {
                 return  f.fetchCurrentData().flatMap(p::parse).tryToConsume(s -> sav.log(logfile, s));
            }

            @Override
            public Optional<String> send(Formatter formatter) {
                try{
                    BufferedReader firstReader = new BufferedReader(new FileReader(logfile.toFile()));
                    BufferedReader allReader = new BufferedReader(new FileReader(logfile.toFile()));

                    return i.getLatest(firstReader).flatMap(s -> formatter.format(s, () -> i.getAll(allReader))).tryToConsume(s::send);
                } catch (FileNotFoundException fnfe){
                    return Optional.of("File not found!");
                }
            }
        };
    }

    static void executeDummyPipeline() {
       Controller dummyController = Controller.create(Fetcher.createDummyCsvFetcher(), Parser.createCsvParser(), Saver.createCsvSaver(), Path.of("dummylog.csv"), Importer.createCsvImporter(), Sender.createDummySender());
       dummyController.retrieveData();
       dummyController.send(Formatter.createSimpleFormatter());
    }
}
