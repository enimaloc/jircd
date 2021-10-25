package com.github.enimaloc.irc.jircd;

import com.electronwill.nightconfig.core.conversion.ObjectConverter;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.github.enimaloc.irc.jircd.server.JIRCD;
import com.github.enimaloc.irc.jircd.server.ServerSettings;
import java.io.File;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        File file = new File("settings.toml");
        if (!file.exists()) {
            new ServerSettings().saveAs(file);
        }
        FileConfig settings = FileConfig.of(file);
        settings.load();
        try {
            new JIRCD(new ObjectConverter().toObject(settings, ServerSettings::new));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

