package kr.pokeeditui;

/** The saved current HP of a pokemon being loaded, kept until its persistent data (with the base stats) is in (see LoadHealthMixin). */
public interface PendingHealth {
    int pokeeditui$pendingHealth();

    void pokeeditui$setPendingHealth(int health);
}
