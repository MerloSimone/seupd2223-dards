package it.unipd.dei.dards.search;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryRescorer;

public class MyQueryRescorer extends QueryRescorer {
    private final float p;

    public MyQueryRescorer(float p, Query q) {
        super(q);
        if(p<0 || p>1) throw new IllegalArgumentException();
        this.p=p;
    }


    @Override
    protected float combine(float v, boolean b, float v1) {
        if(b==false){
            return v;
        }else{
            return (((1-p)*v)+(p*v1));
        }
    }
}
