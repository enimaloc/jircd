package com.github.enimaloc.irc.jircd;

import com.github.enimaloc.irc.jircd.api.JIRCD;
import com.github.enimaloc.irc.jircd.api.ServerSettings;
import java.io.File;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        File settings = new File("settings.toml");
        if (!settings.exists()) {
            new ServerSettings().saveAs(settings);
        }
        try {
            JIRCD server = new JIRCD.Builder()
                    .withFileSettings(settings)
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

