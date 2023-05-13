package it.unipd.dei.dards.analysis;

import org.apache.lucene.analysis.FilteringTokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import java.io.IOException;

/**
 *  Filter that removes all the tokens that represent a number
 */
public class NumberFilter extends FilteringTokenFilter {
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final TypeAttribute typeAttribute = addAttribute(TypeAttribute.class);

    /**
     * Creates a NumberFilter instance
     * @param ts the token stream.
     */
    public NumberFilter(TokenStream ts){
        super(ts);
    }
    @Override
    protected boolean accept() throws IOException {

        if(typeAttribute.type().equals(StandardTokenizer.TOKEN_TYPES[StandardTokenizer.NUM])){
            return false;
        }
        return true;
        /*try{
            Integer.parseInt(new String(termAtt.buffer()));
            return false;
        }catch (Exception e){
            return true;
        }*/
    }

}