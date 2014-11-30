package org.deeplearning4j.scaleout.perform.models.word2vec.iterator;

import org.deeplearning4j.bagofwords.vectorizer.TextVectorizer;
import org.deeplearning4j.models.embeddings.WeightLookupTable;
import org.deeplearning4j.models.embeddings.inmemory.InMemoryLookupTable;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.models.word2vec.wordstore.VocabCache;
import org.deeplearning4j.scaleout.api.statetracker.NewUpdateListener;
import org.deeplearning4j.scaleout.api.statetracker.StateTracker;
import org.deeplearning4j.scaleout.job.Job;
import org.deeplearning4j.scaleout.job.JobIterator;
import org.deeplearning4j.scaleout.perform.models.word2vec.Word2VecResult;
import org.deeplearning4j.scaleout.perform.models.word2vec.Word2VecWork;
import org.deeplearning4j.text.invertedindex.InvertedIndex;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Word2vec job iterator
 *
 *
 * @author Adam Gibson
 */
public class Word2VecJobIterator implements JobIterator {

    private Iterator<List<VocabWord>> sentenceIterator;
    private WeightLookupTable table;
    private VocabCache cache;


    public Word2VecJobIterator(Iterator<List<VocabWord>> sentenceIterator,WeightLookupTable table,VocabCache cache,StateTracker stateTracker) {
        this.sentenceIterator = sentenceIterator;
        this.table = table;
        this.cache = cache;
        addListener(stateTracker);

    }


    public Word2VecJobIterator(TextVectorizer textVectorizer,WeightLookupTable table,VocabCache cache,StateTracker stateTracker) {
        this.sentenceIterator = textVectorizer.index().docs();
        this.cache = cache;
        this.table = table;
        addListener(stateTracker);

    }
    public Word2VecJobIterator(InvertedIndex invertedIndex, WeightLookupTable table,VocabCache cache,StateTracker stateTracker) {
        this.sentenceIterator = invertedIndex.docs();
        this.cache = cache;
        this.table = table;
        addListener(stateTracker);

    }


    private void addListener(StateTracker stateTracker) {
        stateTracker.addUpdateListener(new NewUpdateListener() {
            @Override
            public void onUpdate(Serializable update) {
                Job j = (Job) update;
                Collection<Word2VecResult> work = (Collection<Word2VecResult>) j.getResult();
                if(work == null || work.isEmpty())
                    return;

                InMemoryLookupTable l = (InMemoryLookupTable) table;

                for(Word2VecResult work1 : work) {
                    for(String s : work1.getSyn0Change().keySet()) {
                        l.getSyn0().getRow(cache.indexOf(s)).addi(work1.getSyn0Change().get(s));
                        l.getSyn1().getRow(cache.indexOf(s)).addi(work1.getSyn1Change().get(s));
                        if(l.getSyn1Neg() != null)
                            l.getSyn1Neg().getRow(cache.indexOf(s)).addi(work1.getNegativeChange().get(s));


                    }
                }

            }
        });
    }


    private Word2VecWork create(List<VocabWord> sentence) {
        Word2VecWork work = new Word2VecWork((InMemoryLookupTable) table,sentence);
        return work;
    }

    @Override
    public Job next(String workerId) {

        List<VocabWord> next = sentenceIterator.next();
        return new Job(create(next),workerId);
    }

    @Override
    public Job next() {
        List<VocabWord> next = sentenceIterator.next();
        return new Job(create(next),"");
    }

    @Override
    public boolean hasNext() {
        return sentenceIterator.hasNext();
    }

    @Override
    public void reset() {

    }
}