package com.github.enimaloc.irc.jircd.server.attributes;

public class UserAttribute extends Attribute {
    int awayLen;
    int nickLen;
    int userLen;
    int hostLen;

    public UserAttribute(int awayLen, int nickLen, int userLen, int hostLen) {
        this.awayLen = awayLen;
        this.nickLen = nickLen;
        this.userLen = userLen;
        this.hostLen = hostLen;
    }

    public int awayLen() {
        return awayLen;
    }

    public void awayLen(int awayLen) {
        this.awayLen = awayLen;
    }

    public void nickLen(int nickLen) {
        this.nickLen = nickLen;
    }

    public int nickLen() {
        return nickLen;
    }

    public void userLen(int userLen) {
        this.userLen = userLen;
    }

    public int userLen() {
        return userLen;
    }

    public int hostLen() {
        return hostLen;
    }

    public void hostLen(int hostLen) {
        this.hostLen = hostLen;
    }

    @Override
    public String toString() {
        return "UserAttribute{" +
               "awayLen=" + awayLen +
               ", nickLen=" + nickLen +
               ", userLen=" + userLen +
               ", hostLen=" + hostLen +
               '}';
    }
}