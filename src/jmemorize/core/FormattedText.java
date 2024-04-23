/*
 * jMemorize - Learning made easy (and fun) - A Leitner flashcards tool
 * Copyright(C) 2004-2008 Riad Djemili and contributors
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

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * This class handles encoding/decoding and displaying formatted/
 * unformatted text. The text is immutable.
 * 
 * Styled document 
 * Unformatted String <--> FormattedText class 
 * Encoding
 * 
 * @author djemili
 */
//Renamed the m_formatted & m_unformatted to mFormatted & mUnformatted
//Removed the Cloneable implementation and made the copy constructor
public class FormattedText {
    //Copy constructor & constructor
    public FormattedText(FormattedText other) {
        this.mFormattedText = other.mFormattedText;
        this.mUnformattedText = other.mUnformattedText;
    }
    public FormattedText() {}
    //Made Static
    public static class ParseException extends Exception {
        public ParseException(String message)
        {
            super(message);
        }
    }
    
    // TODO add trimming at end
    // TODO check if reg exp that breaks at new lines is suffice
    // TODO replace direct StyledDocument reference by eclipse-style IAdapter pattern
    // TODO optimze the reg expr
    
    /**
     * An empty formatted text (immutable).
     */
    public static final FormattedText EMPTY = FormattedText.unformatted("");
    
    private static final String  TAGS = "<(/?(b|i|u|sub|sup)?)>";
    private static final Pattern TEXT_PATTERN = Pattern.compile(
        "(.*?)<(/?(b|i|u|sub|sup)?)>", Pattern.DOTALL);
    
    private static final String CONTENT_ELEMENT_NAME = "content";
    
    private String mFormattedText;
    private String mUnformattedText;
    private static final Map<String, Object> stylesMap = new HashMap<>();

    static {
        setupStylesMap();
    }
    public static FormattedText formatted(String formatted)
    {
        FormattedText text = new FormattedText();
        text.mFormattedText = formatted;
        text.mUnformattedText = unescape(formatted.replaceAll(TAGS, "").replaceAll("<img .*?/>", ""));
        
        return text;
    }
    
    public static FormattedText formatted(StyledDocument document)
    {
        Element root = document.getDefaultRootElement();
        String fText = removeRedundantTags(getFormattedText(
            root, 0, document.getLength()));

        return FormattedText.formatted(fText);
    }
    
    public static FormattedText formatted(StyledDocument document, 
        int start, int end)
    {
        Element root = document.getDefaultRootElement();
        String fText = removeRedundantTags(getFormattedText(
            root, start, end));

        return FormattedText.formatted(fText);
    }    
    
    public static FormattedText unformatted(String unformatted)
    {
        FormattedText text = new FormattedText();
        text.mFormattedText = unformatted;
        text.mUnformattedText = unformatted;
        
        return text;
    }
    
    public static void insertImage(Document doc, ImageIcon icon, int offset) 
        throws BadLocationException
    {
        int iconWidth = icon.getIconWidth();
        int iconHeight = icon.getIconHeight();
        Dimension dim = new Dimension(iconWidth, iconHeight);
        
        SimpleAttributeSet sa = new SimpleAttributeSet();
        
        JLabel label = new JLabel(icon);
        label.setMinimumSize(dim);
        label.setPreferredSize(dim);
        label.setMaximumSize(dim);
        label.setSize(dim);
        
        StyleConstants.setComponent(sa, label);
        doc.insertString(offset, " ", sa);
    }
    
    public String getFormatted()
    {
        return mFormattedText;
    }

    public String getUnformatted()
    {
        return mUnformattedText;
    }

    public StyledDocument toStyledDocument()
    {
        DefaultStyledDocument doc = new DefaultStyledDocument();
        doc.setCharacterAttributes(0, doc.getLength() + 1, // HACK 
            SimpleAttributeSet.EMPTY, true);
        
        try
        {
            decode(doc, mFormattedText, 0);
        } 
        catch (Exception e)
        {
            Main.logThrowable("Error formatting card", e);
        }
        
        return doc;
    }
    
    public void insertIntoDocument(StyledDocument doc, int offset)
    {
        try
        {
            decode(doc, mFormattedText, offset);
        } 
        catch (Exception e)
        {
            Main.logThrowable("Error formatting card", e);
        }
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return mUnformattedText;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */


    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (obj instanceof FormattedText other) {
            return mFormattedText.equals(other.mFormattedText);
        }
        return false;
    }
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        return mFormattedText.hashCode();
    }
    
    private static String removeRedundantTags(String formattedText)
    {
        /*
         * StyledDocument merges styles in certain situations. To avoid that
         * this results in getting a different encoding after decoding to a
         * StyledDocument and back to an encoding again, we remove redundant
         * tags by ourselves.
         */
        for (String key : stylesMap.keySet()) {
            formattedText = formattedText.replaceAll("</" + key + "><" + key + ">", "");
        }
        return formattedText;
    }

    private static String getFormattedText(Element e,
                                           int startSelection,
                                           int endSelection) {
        StringBuilder sb = new StringBuilder();
        if (e.getName().equals(CONTENT_ELEMENT_NAME)) {
            Document doc = e.getDocument();

            int start = e.getStartOffset();
            int end = Math.min(e.getEndOffset(), doc.getLength());

            if (start > endSelection || end < startSelection)
                return sb.toString();

            try {
                start = Math.max(start, startSelection);
                end = Math.min(end, endSelection);

                String text = doc.getText(start, end - start);
                sb.append(escape(text));
            } catch (BadLocationException e1) {
                e1.printStackTrace();
                Main.logThrowable("Error formatting text", e1);
            }
        } else {
            for (int i = 0; i < e.getElementCount(); i++)
            {
                sb.append(getFormattedText(e.getElement(i), 
                    startSelection, endSelection));
            }
        }

        for (Map.Entry<String, Object> entry : stylesMap.entrySet()) {
            String name = entry.getKey();
            Object styleId = entry.getValue();

            if (hasStyle(e.getAttributes(), styleId)) {
                sb.insert(0, "<" + name + ">");
                sb.append("</" + name + ">");
            }
        }

        
        return sb.toString();
    }
    
    private static String escape(String text)
    {
        return text.replace("<", "&lt;").replace(">", "&gt;");
    }
    
    private static String unescape(String text)
    {
        return text.replace("&lt;", "<").replace("&gt;", ">");
    }

    private static void setupStylesMap()
    {
        stylesMap.put("b", StyleConstants.Bold);
        stylesMap.put("i", StyleConstants.Italic);
        stylesMap.put("u", StyleConstants.Underline);
        stylesMap.put("sub", StyleConstants.Subscript);
        stylesMap.put("sup", StyleConstants.Superscript);
    }
    
    private void decode(StyledDocument doc, String text, int offset) 
        throws BadLocationException {
        StringBuilder sb = new StringBuilder(text);
        
        Matcher m = TEXT_PATTERN.matcher(sb);
        int end = 0;
        
        SimpleAttributeSet attr = new SimpleAttributeSet();
        while (m.find()) {
            String pretext = m.group(1);
            String tag = m.group(2);
            
            String unescapedPretext = unescape(pretext);
            doc.insertString(offset, unescapedPretext, attr);
            offset += unescapedPretext.length();
            
            boolean style = true;
            if (tag.startsWith("/")) {
                tag = tag.substring(1);
                style = false;
            }
            
            Object styleId = stylesMap.get(tag); 
            attr.addAttribute(styleId, Boolean.valueOf(style));
            
            end = m.end();
        }
        
        String restText = unescape(sb.substring(end));
        doc.insertString(offset, restText, new SimpleAttributeSet());
    }
    private static boolean hasStyle(AttributeSet attr, Object styleId)
    {
        Boolean style = (Boolean)attr.getAttribute(styleId);
        return style != null && style.booleanValue();
    }
}
