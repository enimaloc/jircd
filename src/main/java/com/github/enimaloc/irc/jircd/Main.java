package com.github.enimaloc.irc.jircd;

import com.electronwill.nightconfig.core.conversion.ObjectConverter;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.github.enimaloc.irc.jircd.server.JIRCD;
import com.github.enimaloc.irc.jircd.server.ServerSettings;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.LoggerFactory;

public class Main {

    public static void main(String[] args) {
        Path path = Path.of("settings.toml");
        if (Files.exists(path)) {
            new ServerSettings().saveAs(path);
        }
        FileConfig settings = FileConfig.of(path);
        settings.load();
        try {
            new JIRCD(new ObjectConverter().toObject(settings, ServerSettings::new));
        } catch (IOException e) {
            LoggerFactory.getLogger(Main.class).error(e.getLocalizedMessage(), e);
        }
    }

}

