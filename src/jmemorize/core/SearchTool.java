/*
 * jMemorize - Learning made easy (and fun) - A Leitner flashcards tool
 * Copyright(C) 2004-2006 Riad Djemili
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 1, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package jmemorize.core;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author djemili
 */
public class SearchTool
{
    private SearchTool(){

    }
    public static final int FRONT_SIDE = 0;
    public static final int FLIP_SIDE  = 1;
    public static final int BOTH_SIDES = 2;
    
    public static List<Card> search(String text, int side, boolean matchCase, List<Card> cards)
    {
        List<Card> foundCards = new LinkedList<>();
        for (Card card : cards)
        {
            String frontSide = card.getFrontSide().getText().getUnformatted();

            
            if (!matchCase)
            {
                text      = text.toLowerCase();
                frontSide = frontSide.toLowerCase();

            }
            
            if (side == FRONT_SIDE || side == BOTH_SIDES && frontSide.contains(text))
            {

                    foundCards.add(card);


            }
            


        }
        
        return foundCards;
    }
    
    public static List<Integer> search(String text, String searchtext,
         boolean ignoreCase)
    {
        if (ignoreCase)
        {
            text       = text.toLowerCase();
            searchtext = searchtext.toLowerCase();
        }
        
        List<Integer> positions = new ArrayList<>();
        int pos = 0;
        while ((pos = text.indexOf(searchtext, pos)) >= 0) 
        {
            positions.add(pos);
            pos += searchtext.length();
        }
        
        return positions;
    }

}
