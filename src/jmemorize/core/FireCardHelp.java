package jmemorize.core;

public class FireCardHelp implements Events {
    public FireCardHelp() {
        //default constructor
    }

    public void fireEvent(Card c) {
        if (c.getCategory() != null) {
            c.getCategory().fireCardEvent(EDITED_EVENT, c, c.getCategory(), c.getLevel());
        }
    }
}
