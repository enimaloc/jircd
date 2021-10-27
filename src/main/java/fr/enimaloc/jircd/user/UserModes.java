package fr.enimaloc.jircd.user;

public class UserModes {

    private boolean invisible;
    private boolean oper;
    private boolean localOper;
    private boolean wallops;

    public boolean invisible() {
        return invisible;
    }

    public UserModes invisible(boolean invisible) {
        this.invisible = invisible;
        return this;
    }

    public boolean oper() {
        return oper;
    }

    public UserModes oper(boolean oper) {
        this.oper = oper;
        return this;
    }

    public boolean localOper() {
        return localOper;
    }

    public UserModes localOper(boolean localOper) {
        this.localOper = localOper;
        return this;
    }

    public boolean wallops() {
        return wallops;
    }

    public UserModes wallops(boolean wallops) {
        this.wallops = wallops;
        return this;
    }

    @Override
    public String toString() {
        return "Modes{" +
               "invisible=" + invisible +
               ", oper=" + oper +
               ", localOper=" + localOper +
               ", wallops=" + wallops +
               '}';
    }

    public String modes() {
        return (invisible() ? "i" : "") +
               (oper() ? "o" : "") +
               (localOper() ? "O" : "") +
//                   (registered() ? "r" : "")+
               (wallops() ? "w" : "");
    }

    public String prefix() {
        return (localOper() || oper() ? "@" : "");
    }
}
