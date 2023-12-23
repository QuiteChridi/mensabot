package de.uniwue.jpp.mensabot.sending;

import de.uniwue.jpp.errorhandling.OptionalWithMessage;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Optional;

public interface Sender {

    Optional<String> send(String msg);

    static Sender createDummySender() {
        return new Sender(){
            @Override
            public Optional<String> send(String msg) {
                return OptionalWithMessage.of(msg).consume(System.out::println);
            }
        };
    }

    /* Falls Sie Aufgabenteil 2 nicht bearbeiten, kann diese Methode ignoriert werden */
    static Sender createTelegramSender(HttpClient client, String apiToken, List<String> chatIds) {
        throw new UnsupportedOperationException();
    }
}
