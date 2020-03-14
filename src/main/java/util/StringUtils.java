package util;

import java.io.BufferedReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Pattern;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.RamUsageEstimator;

public class StringUtils extends org.apache.commons.lang3.StringUtils {

    private static final Pattern PATTERN_ACRONYM = Pattern.compile("^(\\p{Alpha}\\.){2,}$");

    public static String asNotEmpty(String s, String defaultValue) {
        return isNotEmpty(s) ? s : defaultValue;
    }

    public static StringBuilder append(StringBuilder buffer, char[] ch, int start, int length) {
        if (buffer == null) {
            buffer = new StringBuilder();
        }
        buffer.append(ch, start, length);
        return buffer;
    }

    public static String trimToNull(CharSequence text) {
        if (text == null) {
            return null;
        }
        String text_ = text.toString().trim();
        return text_.isEmpty() ? null : text_;
    }

    public static boolean hasLetter(String text) {
        if (isNotEmpty(text)) {
            final int size = text.length();
            for (int i = 0; i < size; i++) {
                if (Character.isLetter(text.charAt(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String replaceAccents(String s) {
        char[] output;
        {
            char[] chars = s.toCharArray();
            s = null;
            int length = chars.length;
            // Worst-case length required:
            int maxSizeNeeded = 4 * length;
            output = new char[ArrayUtil.oversize(maxSizeNeeded, RamUsageEstimator.NUM_BYTES_CHAR)];

            ASCIIFoldingFilter.foldToASCII(chars, 0, output, 0, length);
        }
        return new String(output);
    }

    public static int[] splitInts(String s, String separatorChars){
        String[] pieces = split(s, separatorChars);
        int[] ints = new int[pieces.length];
        for (int i = 0; i < pieces.length; i++)
            ints[i] = Integer.parseInt(pieces[i]);
        return ints;
    }

    public static long[] splitLongs(String s, String separatorChars){
        String[] pieces = split(s, separatorChars);
        long[] values = new long[pieces.length];
        for (int i = 0; i < pieces.length; i++)
            values[i] = Long.parseLong(pieces[i]);
        return values;
    }

    public static double[] splitDoubles(String s, String separatorChars){
        String[] pieces = split(s, separatorChars);
        double[] ints = new double[pieces.length];
        for (int i = 0; i < pieces.length; i++)
            ints[i] = Double.parseDouble(pieces[i]);
        return ints;
    }

    public static String retainOnlyDigitsAndSimpleLetters(String s) {
        return s.replaceAll("[^A-Za-z0-9]", "");
    }

    public static String retainDigits(String str) {
        return str.replaceAll("\\D+", "");
    }

    /**
     * Returns the text after removal of all types of valid number ocurrences,
     * decimal or integers, considering english separators for grouping (,) and decimal (.).
     */
    public static String removeNumbers(String text) {
        return text.replaceAll("\\d+(,\\d+)*(\\.\\d+)*", " ");
    }

    public static boolean isAcronym(String term) {
        return term != null && PATTERN_ACRONYM.matcher(term).matches();
    }

    public static boolean isSequenceOfSingleChar(String s){
    	for (int i = 1; i < s.length(); i++) {
			if(s.charAt(i-1) != s.charAt(i)){
				return false;
			}
		}
    	return true;
    }

    public static String stripDirtyFromEdges(String text) {
        int start = 0;
        int end = text.length() - 1;
        while (end >= start && !Character.isLetterOrDigit(text.charAt(end))) {
            end--;
        }
        while (start < end && !Character.isLetterOrDigit(text.charAt(start))) {
            start++;
        }
        return text.substring(start, end + 1);
    }

    public static String removeNonPrintableCharacters(String s) {
        int length = s.length();

        char[] chars = new char[length + 1];
        s.getChars(0, length, chars, 0);
        chars[length] = '\0';// avoiding explicit bound check in while

        // find first non-printable. If there are none it ends on the null char I appended
        int newLength = -1;
        while (chars[++newLength] >= ' ') {}

        // no control characters found? early bail out with existing string
        if (newLength == length) {
            return s;
        }

        for (int j = newLength; j < length; j++) {
            char ch = chars[j];
            if (ch >= ' ') {
                chars[newLength++] = ch; // the while avoids repeated overwriting here when newLen==j
            }
        }
        return new String(chars, 0, newLength);
    }

    /**
     * Here we consider latin characters as the tables: Basic Latin
     */
    public static String removeNonPrintableAndNonBasicLatinCharacters(String s) {
        return removeCharactersOutOfRange(s, ' ', '\u007F');
    }

    /**
     * Here we consider latin characters as the tables: Basic Latin, Latin-1 Supplement, Latin Extended-A and Latin Extended-B.
     */
    public static String removeNonPrintableAndNonLatinCharacters(String s) {
        return removeCharactersOutOfRange(s, ' ', '\u0233');
    }

    public static String removeCharactersOutOfRange(String s, char firstAccepted, char lastAccepted) {
        int length = s.length();

        char[] chars = new char[length + 1];
        s.getChars(0, length, chars, 0);
        chars[length] = '\0';// avoiding explicit bound check in while

        // find first non-printable. If there are none it ends on the null char I appended
        int newLength = 0;
        while (chars[newLength] >= firstAccepted && chars[newLength] <= lastAccepted) {
            newLength++;
        }

        // no prohibited characters found? early bail out with existing string
        if (newLength == length) {
            return s;
        }

        for (int j = newLength; j < length; j++) {
            char ch = chars[j];
            if (ch >= firstAccepted && ch <= lastAccepted) {
                chars[newLength++] = ch; // the while avoids repeated overwriting here when newLen==j
            }
        }
        return new String(chars, 0, newLength);
    }

    public static void removeAtEnd(StringBuilder s, int count) {
        s.setLength(s.length() - count);
    }

    public static String decodeURL(String text, String encoding) {
        if (!isEmpty(text)) {
            try {
                text = URLDecoder.decode(text, encoding);
            } catch (UnsupportedEncodingException e) {}
        }
        return text;
    }

    public static String[] decodeAndSplit(String s, char separatorChar) {
        if (s.isEmpty())
            return null;
        return split(decodeURL(s, "UTF-8"), separatorChar);
    }

    public static BufferedReader toBufferedReader(String s){
		return new BufferedReader(new StringReader(s));
	}

    public static void main(String[] args) {
        //resultado esperado:   elem ents  tre   abcd   abcde   xyw   qwe   ewq
//        String text = "12 elem ents12.2 tre 12.200 abcd 29,100 abcde 30,100.32 xyw 301,030,100.32 qwe 301,030.100.32 ewq";
//        text = text.replaceAll("\\d+(,\\d+)*(\\.\\d+)*", " ");
//        System.out.println(text);

        //testes do identificador de sigla:
        //true
//        System.out.println(isAcronym("A.B."));
//        //false
//        System.out.println(isAcronym("a.b.c."));
//        System.out.println(isAcronym("A.b."));
//        System.out.println(isAcronym(null));
//        System.out.println(isAcronym(""));
//        System.out.println(isAcronym("123"));
//        System.out.println(isAcronym("A.2.3."));
//        System.out.println(isAcronym("a"));
//        System.out.println(isAcronym("a.b"));
//        System.out.println(isAcronym("a.b.c"));

        System.out.println(retainOnlyDigitsAndSimpleLetters("t�rnblomçá 1"));
        System.out.println(removeNonPrintableCharacters("t�rnblomçá 1"));
        System.out.println(removeNonPrintableAndNonLatinCharacters("t�rnblomçá 1 ǯcornell"));
        System.out.println(removeNonPrintableAndNonBasicLatinCharacters("t�rnblomçá 1 ǯcornell"));

        System.out.println(replaceAccents("t�rnblomçá 1 Casa casará brandão zürich ághata açúcar jónsson guðbjörn resumé göteborg"));

        System.out.println(stripDirtyFromEdges("--"));
        System.out.println(stripDirtyFromEdges("---"));
        System.out.println(stripDirtyFromEdges(""));
        System.out.println(stripDirtyFromEdges("abc aasdasdasjd lsa jsaj d"));
        System.out.println(stripDirtyFromEdges(" \n abc aasdasdasjd lsa jsaj d \t"));
        System.out.println(stripDirtyFromEdges(" \n 32abc aasdasdasjd lsa jsaj 3213$%#@"));
        System.out.println(stripDirtyFromEdges("321"));
        System.out.println(stripDirtyFromEdges("3a#"));
        System.out.println(stripDirtyFromEdges("#a1"));
        System.out.println(stripDirtyFromEdges("#a#"));
        System.out.println(stripDirtyFromEdges("#a"));
        System.out.println(stripDirtyFromEdges("a#"));
        System.out.println(stripDirtyFromEdges("a"));
        System.out.println(stripDirtyFromEdges("#"));
        System.out.println(stripDirtyFromEdges("token#"));
        System.out.println(stripDirtyFromEdges("token#!"));
        System.out.println(stripDirtyFromEdges("!$token@#!"));
        System.out.println(stripDirtyFromEdges("!token"));
        System.out.println(stripDirtyFromEdges("-token-"));

        System.out.println();
        System.out.println(isSequenceOfSingleChar(""));
        System.out.println(isSequenceOfSingleChar("a"));
        System.out.println(isSequenceOfSingleChar("bb"));
        System.out.println(isSequenceOfSingleChar("bbb"));
        System.out.println(isSequenceOfSingleChar("cccc"));
        System.out.println(isSequenceOfSingleChar("ddddd"));
        System.out.println(isSequenceOfSingleChar("ab"));
        System.out.println(isSequenceOfSingleChar("abc"));
        System.out.println(isSequenceOfSingleChar("abcd"));
        System.out.println(isSequenceOfSingleChar("aab"));
        System.out.println(isSequenceOfSingleChar("aaab"));
    }
}
