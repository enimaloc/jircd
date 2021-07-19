package com.github.enimaloc.irc.jircd;

import com.github.enimaloc.irc.jircd.api.JIRCD;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        try {
            JIRCD server = new JIRCD.Builder()
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

