package ixa.kaflib;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.jdom2.Element;
import org.jdom2.JDOMException;

/**
 * Respresents a KAF document. It's the main class of the library, as it keeps all elements of the
 * document (word forms, terms, entities...) and manages all object creations. The document can be
 * created by the user calling it's methods, or loading from an existing XML file.
 */

public class KAFDocument implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 9090056364835422525L;

    public enum Layer {
        text, terms, marks, deps, chunks, entities, properties, categories, coreferences, opinions, relations, srl, constituency;
    }

    public class FileDesc implements Serializable {

        /**
         * 
         */
        private static final long serialVersionUID = 736558109018620499L;
        public String author;
        public String title;
        public String filename;
        public String filetype;
        public Integer pages;
        public String creationtime;

        private FileDesc() {
        }
    }

    public class Public implements Serializable {

        /**
         * 
         */
        private static final long serialVersionUID = -7253074783695210967L;
        public String publicId;
        public String uri;

        private Public() {
        }
    }

    public class LinguisticProcessor implements Serializable {

        /**
         * 
         */
        private static final long serialVersionUID = 4016940578834879593L;
        String name;
        String timestamp;
        String beginTimestamp;
        String endTimestamp;
        String version;

        private LinguisticProcessor(final String name) {
            this.name = name;
        }

        /* Deprecated */
        private LinguisticProcessor(final String name, final String timestamp, final String version) {
            this.name = name;
            this.timestamp = timestamp;
            this.version = version;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public boolean hasTimestamp() {
            return this.timestamp != null;
        }

        public void setTimestamp(final String timestamp) {
            this.timestamp = timestamp;
        }

        public void setTimestamp() {
            final String timestamp = createTimestamp();
            this.timestamp = timestamp;
        }

        public String getTimestamp() {
            return this.timestamp;
        }

        public boolean hasBeginTimestamp() {
            return this.beginTimestamp != null;
        }

        public void setBeginTimestamp(final String timestamp) {
            this.beginTimestamp = timestamp;
        }

        public void setBeginTimestamp() {
            final String timestamp = createTimestamp();
            this.beginTimestamp = timestamp;
        }

        public String getBeginTimestamp() {
            return this.beginTimestamp;
        }

        public boolean hasEndTimestamp() {
            return this.endTimestamp != null;
        }

        public void setEndTimestamp(final String timestamp) {
            this.endTimestamp = timestamp;
        }

        public void setEndTimestamp() {
            final String timestamp = createTimestamp();
            this.endTimestamp = timestamp;
        }

        public String getEndTimestamp() {
            return this.endTimestamp;
        }

        public boolean hasVersion() {
            return this.version != null;
        }

        public void setVersion(final String version) {
            this.version = version;
        }

        public String getVersion() {
            return this.version;
        }

    }

    /**
     * Language identifier
     */
    private String lang;

    /**
     * KAF version
     */
    private String version;

    /**
     * Linguistic processors
     */
    private final Map<String, List<LinguisticProcessor>> lps;

    private FileDesc fileDesc;

    private Public _public;

    /**
     * Identifier manager
     */
    private final IdManager idManager;

    /**
     * Keeps all the annotations of the document
     */
    private final AnnotationContainer annotationContainer;

    /**
     * Creates an empty KAFDocument element
     */
    public KAFDocument(final String lang, final String version) {
        this.lang = lang;
        this.version = version;
        this.lps = new LinkedHashMap<String, List<LinguisticProcessor>>();
        this.idManager = new IdManager();
        this.annotationContainer = new AnnotationContainer();
    }

    /**
     * Creates a new KAFDocument and loads the contents of the file passed as argument
     * 
     * @param file
     *            an existing KAF file to be loaded into the library.
     */
    public static KAFDocument createFromFile(final File file) throws IOException {
        KAFDocument kaf = null;
        try {
            kaf = ReadWriteManager.load(file);
        } catch (final JDOMException e) {
            e.printStackTrace();
        }
        return kaf;
    }

    /**
     * Creates a new KAFDocument loading the content read from the reader given on argument.
     * 
     * @param stream
     *            Reader to read KAF content.
     */
    public static KAFDocument createFromStream(final Reader stream) throws IOException {
        KAFDocument kaf = null;
        try {
            kaf = ReadWriteManager.load(stream);
        } catch (final JDOMException e) {
            e.printStackTrace();
        }
        return kaf;
    }

    /**
     * Sets the language of the processed document
     */
    public void setLang(final String lang) {
        this.lang = lang;
    }

    /**
     * Returns the language of the processed document
     */
    public String getLang() {
        return this.lang;
    }

    /**
     * Sets the KAF version
     */
    public void setVersion(final String version) {
        this.version = version;
    }

    /**
     * Returns the KAF version
     */
    public String getVersion() {
        return this.version;
    }

    /**
     * Adds a linguistic processor to the document header. The timestamp is added implicitly.
     */
    public LinguisticProcessor addLinguisticProcessor(final String layer, final String name) {
        final String timestamp = createTimestamp();
        final LinguisticProcessor lp = new LinguisticProcessor(name);
        // lp.setBeginTimestamp(timestamp); // no default timestamp
        List<LinguisticProcessor> layerLps = this.lps.get(layer);
        if (layerLps == null) {
            layerLps = new ArrayList<LinguisticProcessor>();
            this.lps.put(layer, layerLps);
        }
        layerLps.add(lp);
        return lp;
    }

    public void addLinguisticProcessors(final Map<String, List<LinguisticProcessor>> lps) {
        for (final Map.Entry<String, List<LinguisticProcessor>> entry : lps.entrySet()) {
            final List<LinguisticProcessor> layerLps = entry.getValue();
            for (final LinguisticProcessor lp : layerLps) {
                final LinguisticProcessor newLp = this.addLinguisticProcessor(entry.getKey(),
                        lp.name);
                if (lp.hasTimestamp()) {
                    newLp.setTimestamp(lp.getTimestamp());
                }
                if (lp.hasBeginTimestamp()) {
                    newLp.setBeginTimestamp(lp.getBeginTimestamp());
                }
                if (lp.hasEndTimestamp()) {
                    newLp.setEndTimestamp(lp.getEndTimestamp());
                }
                if (lp.hasVersion()) {
                    newLp.setVersion(lp.getVersion());
                }
            }
        }
    }

    /**
     * Returns a hash of linguistic processors from the document. Hash: layer => LP
     */
    public Map<String, List<LinguisticProcessor>> getLinguisticProcessors() {
        return this.lps;
    }

    /**
     * Returns wether the given linguistic processor is already defined or not. Both name and
     * version must be exactly the same.
     */
    public boolean linguisticProcessorExists(final String layer, final String name,
            final String version) {
        final List<LinguisticProcessor> layerLPs = this.lps.get(layer);
        if (layerLPs == null) {
            return false;
        }
        for (final LinguisticProcessor lp : layerLPs) {
            if (lp.version == null) {
                return false;
            } else if (lp.name.equals(name) && lp.version.equals(version)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns wether the given linguistic processor is already defined or not. Both name and
     * version must be exactly the same.
     */
    public boolean linguisticProcessorExists(final String layer, final String name) {
        final List<LinguisticProcessor> layerLPs = this.lps.get(layer);
        if (layerLPs == null) {
            return false;
        }
        for (final LinguisticProcessor lp : layerLPs) {
            if (lp.version != null) {
                return false;
            } else if (lp.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    public FileDesc createFileDesc() {
        this.fileDesc = new FileDesc();
        return this.fileDesc;
    }

    public FileDesc getFileDesc() {
        return this.fileDesc;
    }

    public Public createPublic() {
        this._public = new Public();
        return this._public;
    }

    public Public getPublic() {
        return this._public;
    }

    /**
     * Returns the annotation container used by this object
     */
    AnnotationContainer getAnnotationContainer() {
        return this.annotationContainer;
    }

    /**
     * Set raw text *
     */
    public void setRawText(final String rawText) {
        this.annotationContainer.setRawText(rawText);
    }

    /**
     * Creates a WF object to load an existing word form. It receives the ID as an argument. The
     * WF is added to the document object.
     * 
     * @param id
     *            word form's ID.
     * @param form
     *            text of the word form itself.
     * @return a new word form.
     */
    public WF newWF(final String id, final String form, final int sent) {
        this.idManager.wfs.update(id);
        final WF newWF = new WF(this.annotationContainer, id, form, sent);
        this.annotationContainer.add(newWF);
        return newWF;
    }

    /**
     * Creates a new WF object. It assigns an appropriate ID to it and it also assigns offset and
     * length attributes. The WF is added to the document object.
     * 
     * @param form
     *            text of the word form itself.
     * @return a new word form.
     */
    public WF newWF(final String form, final int offset) {
        final String newId = this.idManager.wfs.getNext();
        final int offsetVal = offset;
        final WF newWF = new WF(this.annotationContainer, newId, form, 0);
        newWF.setOffset(offsetVal);
        newWF.setLength(form.length());
        this.annotationContainer.add(newWF);
        return newWF;
    }

    /**
     * Creates a new WF object. It assigns an appropriate ID to it. The WF is added to the
     * document object.
     * 
     * @param form
     *            text of the word form itself.
     * @return a new word form.
     */
    public WF newWF(final String form, final int offset, final int sent) {
        final String newId = this.idManager.wfs.getNext();
        final WF newWF = new WF(this.annotationContainer, newId, form, sent);
        newWF.setOffset(offset);
        newWF.setLength(form.length());
        this.annotationContainer.add(newWF);
        return newWF;
    }

    /**
     * Creates a Term object to load an existing term. It receives the ID as an argument. The Term
     * is added to the document object.
     * 
     * @param id
     *            term's ID.
     * @param type
     *            type of term. There are two types of term: open and close.
     * @param lemma
     *            the lemma of the term.
     * @param pos
     *            part of speech of the term.
     * @param wfs
     *            the list of word forms this term is formed by.
     * @return a new term.
     */
    public Term newTerm(final String id, final Span<WF> span) {
        this.idManager.terms.update(id);
        final Term newTerm = new Term(id, span, false);
        this.annotationContainer.add(newTerm);
        return newTerm;
    }

    public Term newTerm(final String id, final Span<WF> span, final boolean isComponent) {
        this.idManager.terms.update(id);
        final Term newTerm = new Term(id, span, isComponent);
        if (!isComponent) {
            this.annotationContainer.add(newTerm);
        }
        return newTerm;
    }

    public Term newTerm(final String id, final Span<WF> span, final Integer position) {
        this.idManager.terms.update(id);
        final Term newTerm = new Term(id, span, false);
        this.annotationContainer.add(newTerm, position);
        return newTerm;
    }

    /**
     * Creates a new Term. It assigns an appropriate ID to it. The Term is added to the document
     * object.
     * 
     * @param type
     *            the type of the term. There are two types of term: open and close.
     * @param lemma
     *            the lemma of the term.
     * @param pos
     *            part of speech of the term.
     * @param wfs
     *            the list of word forms this term is formed by.
     * @return a new term.
     */
    public Term newTerm(final Span<WF> span) {
        final String newId = this.idManager.terms.getNext();
        final Term newTerm = new Term(newId, span, false);
        this.annotationContainer.add(newTerm);
        return newTerm;
    }

    /**
     * Creates a new Term. It assigns an appropriate ID to it. The Term is added to the document
     * object.
     * 
     * @param type
     *            the type of the term. There are two types of term: open and close.
     * @param lemma
     *            the lemma of the term.
     * @param pos
     *            part of speech of the term.
     * @param wfs
     *            the list of word forms this term is formed by.
     * @return a new term.
     */
    public Term newTermOptions(final String morphofeat, final Span<WF> span) {
        final String newId = this.idManager.terms.getNext();
        final Term newTerm = new Term(newId, span, false);
        newTerm.setMorphofeat(morphofeat);
        this.annotationContainer.add(newTerm);
        return newTerm;
    }

    public Term newCompound(final List<Term> terms, final String lemma) {
        final Span<WF> span = new Span<WF>();
        for (final Term term : terms) {
            span.addTargets(term.getSpan().getTargets());
        }
        final String newId = this.idManager.mws.getNext();
        final Term compound = newTerm(newId, span,
                this.annotationContainer.termPosition(terms.get(0)));
        compound.setLemma(lemma);
        for (final Term term : terms) {
            compound.addComponent(term);
            term.setCompound(compound);
            this.annotationContainer.remove(term);
        }
        return compound;
    }

    /**
     * Creates a Sentiment object.
     * 
     * @return a new sentiment.
     */
    public Term.Sentiment newSentiment() {
        final Term.Sentiment newSentiment = new Term.Sentiment();
        return newSentiment;
    }

    public Mark newMark(final String id, final String source, final Span<Term> span) {
        this.idManager.marks.update(id);
        final Mark newMark = new Mark(id, span);
        this.annotationContainer.add(newMark, source);
        return newMark;
    }

    public Mark newMark(final String source, final Span<Term> span) {
        final String newId = this.idManager.marks.getNext();
        final Mark newMark = new Mark(newId, span);
        this.annotationContainer.add(newMark, source);
        return newMark;
    }

    /**
     * Creates a new dependency. The Dep is added to the document object.
     * 
     * @param from
     *            the origin term of the dependency.
     * @param to
     *            the target term of the dependency.
     * @param rfunc
     *            relational function of the dependency.
     * @return a new dependency.
     */
    public Dep newDep(final Term from, final Term to, final String rfunc) {
        final Dep newDep = new Dep(from, to, rfunc);
        this.annotationContainer.add(newDep);
        return newDep;
    }

    /**
     * Creates a chunk object to load an existing chunk. It receives it's ID as an argument. The
     * Chunk is added to the document object.
     * 
     * @param id
     *            chunk's ID.
     * @param head
     *            the chunk head.
     * @param phrase
     *            type of the phrase.
     * @param terms
     *            the list of the terms in the chunk.
     * @return a new chunk.
     */
    public Chunk newChunk(final String id, final String phrase, final Span<Term> span) {
        this.idManager.chunks.update(id);
        final Chunk newChunk = new Chunk(id, span);
        newChunk.setPhrase(phrase);
        this.annotationContainer.add(newChunk);
        return newChunk;
    }

    /**
     * Creates a new chunk. It assigns an appropriate ID to it. The Chunk is added to the document
     * object.
     * 
     * @param head
     *            the chunk head.
     * @param phrase
     *            type of the phrase.
     * @param terms
     *            the list of the terms in the chunk.
     * @return a new chunk.
     */
    public Chunk newChunk(final String phrase, final Span<Term> span) {
        final String newId = this.idManager.chunks.getNext();
        final Chunk newChunk = new Chunk(newId, span);
        newChunk.setPhrase(phrase);
        this.annotationContainer.add(newChunk);
        return newChunk;
    }

    /**
     * Creates an Entity object to load an existing entity. It receives the ID as an argument. The
     * entity is added to the document object.
     * 
     * @param id
     *            the ID of the named entity.
     * @param type
     *            entity type. 8 values are posible: Person, Organization, Location, Date, Time,
     *            Money, Percent, Misc.
     * @param references
     *            it contains one or more span elements. A span can be used to reference the
     *            different occurrences of the same named entity in the document. If the entity is
     *            composed by multiple words, multiple target elements are used.
     * @return a new named entity.
     */
    public Entity newEntity(final String id, final List<Span<Term>> references) {
        this.idManager.entities.update(id);
        final Entity newEntity = new Entity(id, references);
        this.annotationContainer.add(newEntity);
        return newEntity;
    }

    /**
     * Creates a new Entity. It assigns an appropriate ID to it. The entity is added to the
     * document object.
     * 
     * @param type
     *            entity type. 8 values are posible: Person, Organization, Location, Date, Time,
     *            Money, Percent, Misc.
     * @param references
     *            it contains one or more span elements. A span can be used to reference the
     *            different occurrences of the same named entity in the document. If the entity is
     *            composed by multiple words, multiple target elements are used.
     * @return a new named entity.
     */
    public Entity newEntity(final List<Span<Term>> references) {
        final String newId = this.idManager.entities.getNext();
        final Entity newEntity = new Entity(newId, references);
        this.annotationContainer.add(newEntity);
        return newEntity;
    }

    /**
     * Creates a coreference object to load an existing Coref. It receives it's ID as an argument.
     * The Coref is added to the document.
     * 
     * @param id
     *            the ID of the coreference.
     * @param references
     *            different mentions (list of targets) to the same entity.
     * @return a new coreference.
     */
    public Coref newCoref(final String id, final List<Span<Term>> mentions) {
        this.idManager.corefs.update(id);
        final Coref newCoref = new Coref(id, mentions);
        this.annotationContainer.add(newCoref);
        return newCoref;
    }

    /**
     * Creates a new coreference. It assigns an appropriate ID to it. The Coref is added to the
     * document.
     * 
     * @param references
     *            different mentions (list of targets) to the same entity.
     * @return a new coreference.
     */
    public Coref newCoref(final List<Span<Term>> mentions) {
        final String newId = this.idManager.corefs.getNext();
        final Coref newCoref = new Coref(newId, mentions);
        this.annotationContainer.add(newCoref);
        return newCoref;
    }

    /**
     * Creates a timeExpressions object to load an existing Timex3. It receives it's ID as an
     * argument. The Timex3 is added to the document.
     * 
     * @param id
     *            the ID of the coreference.
     * @param references
     *            different mentions (list of targets) to the same entity.
     * @return a new timex3.
     */
    public Timex3 newTimex3(final String id, final List<Span<WF>> mentions) {
        this.idManager.timex3s.update(id);
        final Timex3 newTimex3 = new Timex3(id, mentions);
        this.annotationContainer.add(newTimex3);
        return newTimex3;
    }

    /**
     * Creates a new timeExpressions. It assigns an appropriate ID to it. The Coref is added to
     * the document.
     * 
     * @param references
     *            different mentions (list of targets) to the same entity.
     * @return a new timex3.
     */
    public Timex3 newTimex3(final List<Span<WF>> mentions) {
        final String newId = this.idManager.timex3s.getNext();
        final Timex3 newTimex3 = new Timex3(newId, mentions);
        this.annotationContainer.add(newTimex3);
        return newTimex3;
    }

    /**
     * Creates a new timeExpressions. It assigns an appropriate ID to it. The Coref is added to
     * the document.
     * 
     * @param references
     *            different mentions (list of targets) to the same entity.
     * @return a new timex3.
     */
    public Timex3 newTimex3(final String id) {
        this.idManager.timex3s.update(id);
        final Timex3 newTimex3 = new Timex3(id);
        this.annotationContainer.add(newTimex3);
        return newTimex3;
    }

    /**
     * Creates a factualitylayer object and add it to the document
     * 
     * @param term
     *            the Term of the coreference.
     * @return a new factuality.
     */
    public Factuality newFactuality(final Term term) {
        final Factuality factuality = new Factuality(term);
        this.annotationContainer.add(factuality);
        return factuality;
    }

    /**
     * Creates a LinkedEntity object and add it to the document, using the supplied ID.
     * 
     * @param id
     *            the entity ID
     * @param term
     *            the Term of the coreference
     * @return a new factuality
     */
    public LinkedEntity newLinkedEntity(final String id, final Span<WF> span) {
        final LinkedEntity linkedEntity = new LinkedEntity(id, span);
        this.annotationContainer.add(linkedEntity);
        return linkedEntity;
    }

    /**
     * Creates a LinkedEntity object and add it to the document
     * 
     * @param term
     *            the Term of the coreference.
     * @return a new factuality.
     */
    public LinkedEntity newLinkedEntity(final Span<WF> span) {
        final String newId = this.idManager.linkedentities.getNext();
        final LinkedEntity linkedEntity = new LinkedEntity(newId, span);
        this.annotationContainer.add(linkedEntity);
        return linkedEntity;
    }

    /**
     * Creates a SSTspan object and add it to the document
     * 
     * @param term
     *            the Term of the coreference.
     * @return a new factuality.
     */
    public SSTspan newSST(final Span<Term> span) {
        final String newId = this.idManager.ssts.getNext();
        final SSTspan sst = new SSTspan(newId, span);
        this.annotationContainer.add(sst);
        return sst;
    }

    public SSTspan newSST(final Span<Term> span, final String type, final String label) {
        final String newId = this.idManager.ssts.getNext();
        final SSTspan sst = new SSTspan(newId, span);
        sst.setLabel(label);
        sst.setType(type);
        this.annotationContainer.add(sst);
        return sst;
    }

    /**
     * Creates a Topic object and add it to the document
     * 
     * @param term
     *            the Term of the coreference.
     * @return a new factuality.
     */
    public Topic newTopic() {
        final String newId = this.idManager.topics.getNext();
        final Topic t = new Topic(newId);
        this.annotationContainer.add(t);
        return t;
    }

    public Topic newTopic(final String label, final float probability) {
        final String newId = this.idManager.topics.getNext();
        final Topic t = new Topic(newId);
        t.setLabel(label);
        t.setProbability(probability);
        this.annotationContainer.add(t);
        return t;
    }

    /**
     * Creates a new property. It receives it's ID as an argument. The property is added to the
     * document.
     * 
     * @param id
     *            the ID of the property.
     * @param lemma
     *            the lemma of the property.
     * @param references
     *            different mentions (list of targets) to the same property.
     * @return a new coreference.
     */
    public Feature newProperty(final String id, final String lemma,
            final List<Span<Term>> references) {
        this.idManager.properties.update(id);
        final Feature newProperty = new Feature(id, lemma, references);
        this.annotationContainer.add(newProperty);
        return newProperty;
    }

    /**
     * Creates a new property. It assigns an appropriate ID to it. The property is added to the
     * document.
     * 
     * @param lemma
     *            the lemma of the property.
     * @param references
     *            different mentions (list of targets) to the same property.
     * @return a new coreference.
     */
    public Feature newProperty(final String lemma, final List<Span<Term>> references) {
        final String newId = this.idManager.properties.getNext();
        final Feature newProperty = new Feature(newId, lemma, references);
        this.annotationContainer.add(newProperty);
        return newProperty;
    }

    /**
     * Creates a new category. It receives it's ID as an argument. The category is added to the
     * document.
     * 
     * @param id
     *            the ID of the category.
     * @param lemma
     *            the lemma of the category.
     * @param references
     *            different mentions (list of targets) to the same category.
     * @return a new coreference.
     */
    public Feature newCategory(final String id, final String lemma,
            final List<Span<Term>> references) {
        this.idManager.categories.update(id);
        final Feature newCategory = new Feature(id, lemma, references);
        this.annotationContainer.add(newCategory);
        return newCategory;
    }

    /**
     * Creates a new category. It assigns an appropriate ID to it. The category is added to the
     * document.
     * 
     * @param lemma
     *            the lemma of the category.
     * @param references
     *            different mentions (list of targets) to the same category.
     * @return a new coreference.
     */
    public Feature newCategory(final String lemma, final List<Span<Term>> references) {
        final String newId = this.idManager.categories.getNext();
        final Feature newCategory = new Feature(newId, lemma, references);
        this.annotationContainer.add(newCategory);
        return newCategory;
    }

    /**
     * Creates a new opinion object. It assigns an appropriate ID to it. The opinion is added to
     * the document.
     * 
     * @return a new opinion.
     */
    public Opinion newOpinion() {
        final String newId = this.idManager.opinions.getNext();
        final Opinion newOpinion = new Opinion(newId);
        this.annotationContainer.add(newOpinion);
        return newOpinion;
    }

    /**
     * Creates a new opinion object. It receives its ID as an argument. The opinion is added to
     * the document.
     * 
     * @return a new opinion.
     */
    public Opinion newOpinion(final String id) {
        this.idManager.opinions.update(id);
        final Opinion newOpinion = new Opinion(id);
        this.annotationContainer.add(newOpinion);
        return newOpinion;
    }

    /**
     * Creates a new relation between entities and/or sentiment features. It assigns an
     * appropriate ID to it. The relation is added to the document.
     * 
     * @param from
     *            source of the relation
     * @param to
     *            target of the relation
     * @return a new relation
     */
    public Relation newRelation(final Relational from, final Relational to) {
        final String newId = this.idManager.relations.getNext();
        final Relation newRelation = new Relation(newId, from, to);
        this.annotationContainer.add(newRelation);
        return newRelation;
    }

    /**
     * Creates a new relation between entities and/or sentiment features. It receives its ID as an
     * argument. The relation is added to the document.
     * 
     * @param id
     *            the ID of the relation
     * @param from
     *            source of the relation
     * @param to
     *            target of the relation
     * @return a new relation
     */
    public Relation newRelation(final String id, final Relational from, final Relational to) {
        this.idManager.relations.update(id);
        final Relation newRelation = new Relation(id, from, to);
        this.annotationContainer.add(newRelation);
        return newRelation;
    }

    /**
     * Creates a new srl predicate. It receives its ID as an argument. The predicate is added to
     * the document.
     * 
     * @param id
     *            the ID of the predicate
     * @param span
     *            span containing the targets of the predicate
     * @return a new predicate
     */
    public Predicate newPredicate(final String id, final Span<Term> span) {
        this.idManager.predicates.update(id);
        final Predicate newPredicate = new Predicate(id, span);
        this.annotationContainer.add(newPredicate);
        return newPredicate;
    }

    /**
     * Creates a new srl predicate. It assigns an appropriate ID to it. The predicate is added to
     * the document.
     * 
     * @param span
     *            span containing all the targets of the predicate
     * @return a new predicate
     */
    public Predicate newPredicate(final Span<Term> span) {
        final String newId = this.idManager.predicates.getNext();
        final Predicate newPredicate = new Predicate(newId, span);
        this.annotationContainer.add(newPredicate);
        return newPredicate;
    }

    /**
     * Creates a Role object to load an existing role. It receives the ID as an argument. It
     * doesn't add the role to the predicate.
     * 
     * @param id
     *            role's ID.
     * @param predicate
     *            the predicate which this role is part of
     * @param semRole
     *            semantic role
     * @param span
     *            span containing all the targets of the role
     * @return a new role.
     */
    public Predicate.Role newRole(final String id, final Predicate predicate,
            final String semRole, final Span<Term> span) {
        this.idManager.roles.update(id);
        final Predicate.Role newRole = new Predicate.Role(id, semRole, span);
        return newRole;
    }

    /**
     * Creates a new Role object. It assigns an appropriate ID to it. It uses the ID of the
     * predicate to create a new ID for the role. It doesn't add the role to the predicate.
     * 
     * @param predicate
     *            the predicate which this role is part of
     * @param semRole
     *            semantic role
     * @param span
     *            span containing all the targets of the role
     * @return a new role.
     */
    public Predicate.Role newRole(final Predicate predicate, final String semRole,
            final Span<Term> span) {
        final String newId = this.idManager.roles.getNext();
        final Predicate.Role newRole = new Predicate.Role(newId, semRole, span);
        return newRole;
    }

    /**
     * Creates a new external reference.
     * 
     * @param resource
     *            indicates the identifier of the resource referred to.
     * @param reference
     *            code of the referred element.
     * @return a new external reference object.
     */
    public ExternalRef newExternalRef(final String resource, final String reference) {
        return new ExternalRef(resource, reference);
    }

    public Tree newConstituent(final TreeNode root) {
        final Tree tree = new Tree(root);
        this.annotationContainer.add(tree);
        return tree;
    }

    public void addConstituencyFromParentheses(final String parseOut) throws Exception {
        Tree.parenthesesToKaf(parseOut, this);
    }

    public NonTerminal newNonTerminal(final String id, final String label) {
        final NonTerminal tn = new NonTerminal(id, label);
        final String newEdgeId = this.idManager.edges.getNext();
        tn.setEdgeId(newEdgeId);
        return tn;
    }

    public NonTerminal newNonTerminal(final String label) {
        final String newId = this.idManager.nonterminals.getNext();
        final String newEdgeId = this.idManager.edges.getNext();
        final NonTerminal newNonterminal = new NonTerminal(newId, label);
        newNonterminal.setEdgeId(newEdgeId);
        return newNonterminal;
    }

    public Terminal newTerminal(final String id, final Span<Term> span) {
        final Terminal tn = new Terminal(id, span);
        final String newEdgeId = this.idManager.edges.getNext();
        tn.setEdgeId(newEdgeId);
        return tn;
    }

    public Terminal newTerminal(final Span<Term> span) {
        final String newId = this.idManager.terminals.getNext();
        final String newEdgeId = this.idManager.edges.getNext();
        final Terminal tn = new Terminal(newId, span);
        tn.setEdgeId(newEdgeId);
        return tn;
    }

    public static Span<WF> newWFSpan() {
        return new Span<WF>();
    }

    public static Span<WF> newWFSpan(final List<WF> targets) {
        return new Span<WF>(targets);
    }

    public static Span<WF> newWFSpan(final List<WF> targets, final WF head) {
        return new Span<WF>(targets, head);
    }

    public static Span<Term> newTermSpan() {
        return new Span<Term>();
    }

    public static Span<Term> newTermSpan(final List<Term> targets) {
        return new Span<Term>(targets);
    }

    public static Span<Term> newTermSpan(final List<Term> targets, final Term head) {
        return new Span<Term>(targets, head);
    }

    void addUnknownLayer(final Element layer) {
        this.annotationContainer.add(layer);
    }

    /**
     * Returns the raw text *
     */
    public String getRawText() {
        return this.annotationContainer.getRawText();
    }

    /**
     * Returns a list containing all WFs in the document
     */
    public List<WF> getWFs() {
        return this.annotationContainer.getText();
    }

    /**
     * Returns a list with all sentences. Each sentence is a list of WFs.
     */
    public List<List<WF>> getSentences() {
        return this.annotationContainer.getSentences();
    }

    public Integer getFirstSentence() {
        return this.annotationContainer.getText().get(0).getSent();
    }

    public Integer getNumSentences() {
        final List<WF> wfs = this.annotationContainer.getText();
        final Integer firstSentence = wfs.get(0).getSent();
        final Integer lastSentence = wfs.get(wfs.size() - 1).getSent();
        return lastSentence - firstSentence + 1;
    }

    public List<Integer> getSentsByParagraph(final Integer para) {
        if (this.annotationContainer.sentsIndexedByParagraphs.get(para) == null) {
            System.out.println(para + ": 0");
        }
        return new ArrayList<Integer>(this.annotationContainer.sentsIndexedByParagraphs.get(para));
    }

    public Integer getFirstParagraph() {
        return this.annotationContainer.getText().get(0).getPara();
    }

    public Integer getNumParagraphs() {
        return this.annotationContainer.sentsIndexedByParagraphs.keySet().size();
    }

    /**
     * Returns a list with all terms in the document.
     */
    public List<Term> getTerms() {
        return this.annotationContainer.getTerms();
    }

    /**
     * Returns a list of terms containing the word forms given on argument.
     * 
     * @param wfs
     *            a list of word forms whose terms will be found.
     * @return a list of terms containing the given word forms.
     */
    public List<Term> getTermsByWFs(final List<WF> wfs) {
        return this.annotationContainer.getTermsByWFs(wfs);
    }

    public List<Term> getSentenceTerms(final int sent) {
        return this.annotationContainer.getSentenceTerms(sent);
    }

    public List<String> getMarkSources() {
        return this.annotationContainer.getMarkSources();
    }

    public List<Mark> getMarks(final String source) {
        return this.annotationContainer.getMarks(source);
    }

    public List<Dep> getDeps() {
        return this.annotationContainer.getDeps();
    }

    public List<Chunk> getChunks() {
        return this.annotationContainer.getChunks();
    }

    /**
     * Returns a list with all entities in the document
     */
    public List<Entity> getEntities() {
        return this.annotationContainer.getEntities();
    }

    public List<Coref> getCorefs() {
        return this.annotationContainer.getCorefs();
    }

    public List<Timex3> getTimeExs() {
        return this.annotationContainer.getTimeExs();
    }

    /**
     * Returns a list with all relations in the document
     */
    public List<Feature> getProperties() {
        return this.annotationContainer.getProperties();
    }

    /**
     * Returns a list with all relations in the document
     */
    public List<Feature> getCategories() {
        return this.annotationContainer.getCategories();
    }

    public List<Opinion> getOpinions() {
        return this.annotationContainer.getOpinions();
    }

    /**
     * Returns a list with all relations in the document
     */
    public List<Relation> getRelations() {
        return this.annotationContainer.getRelations();
    }

    public List<Tree> getConstituents() {
        return this.annotationContainer.getConstituents();
    }

    public List<Element> getUnknownLayers() {
        return this.annotationContainer.getUnknownLayers();
    }

    public List<WF> getWFsBySent(final Integer sent) {
        final List<WF> wfs = this.annotationContainer.textIndexedBySent.get(sent);
        return wfs == null ? new ArrayList<WF>() : wfs;
    }

    public List<WF> getWFsByPara(final Integer para) {
        return this.annotationContainer.getLayerByPara(para,
                this.annotationContainer.textIndexedBySent);
    }

    public List<Term> getTermsBySent(final Integer sent) {
        final List<Term> terms = this.annotationContainer.termsIndexedBySent.get(sent);
        return terms == null ? new ArrayList<Term>() : terms;
    }

    public List<Term> getTermsByPara(final Integer para) {
        return this.annotationContainer.getLayerByPara(para,
                this.annotationContainer.termsIndexedBySent);
    }

    public List<Entity> getEntitiesBySent(final Integer sent) {
        final List<Entity> entities = this.annotationContainer.entitiesIndexedBySent.get(sent);
        return entities == null ? new ArrayList<Entity>() : entities;
    }

    public List<Entity> getEntitiesByPara(final Integer para) {
        return this.annotationContainer.getLayerByPara(para,
                this.annotationContainer.entitiesIndexedBySent);
    }

    public List<Dep> getDepsBySent(final Integer sent) {
        return this.annotationContainer.depsIndexedBySent.get(sent);
    }

    public List<Dep> getDepsByPara(final Integer para) {
        return this.annotationContainer.getLayerByPara(para,
                this.annotationContainer.depsIndexedBySent);
    }

    public List<Chunk> getChunksBySent(final Integer sent) {
        return this.annotationContainer.chunksIndexedBySent.get(sent);
    }

    public List<Chunk> getChunksByPara(final Integer para) {
        return this.annotationContainer.getLayerByPara(para,
                this.annotationContainer.chunksIndexedBySent);
    }

    public List<Predicate> getPredicatesBySent(final Integer sent) {
        final List<Predicate> result = this.annotationContainer.predicatesIndexedBySent.get(sent);
        return result != null ? result : Collections.<Predicate>emptyList();
    }

    public List<Predicate> getPredicatesByPara(final Integer para) {
        return this.annotationContainer.getLayerByPara(para,
                this.annotationContainer.predicatesIndexedBySent);
    }

    /**
     * Returns current timestamp.
     */
    public String createTimestamp() {
        final Date date = new Date();
        // SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd H:mm:ss");
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        final String formattedDate = sdf.format(date);
        return formattedDate;
    }

    /**
     * Copies the annotations to another KAF document
     */
    private void copyAnnotationsToKAF(final KAFDocument kaf, final List<WF> wfs,
            final List<Term> terms, final List<Dep> deps, final List<Chunk> chunks,
            final List<Entity> entities, final List<Coref> corefs, final List<Timex3> timeExs,
            final List<Feature> properties, final List<Feature> categories,
            final List<Opinion> opinions, final List<Relation> relations,
            final List<Predicate> predicates) {
        final HashMap<String, WF> copiedWFs = new HashMap<String, WF>();
        final HashMap<String, Term> copiedTerms = new HashMap<String, Term>();
        final HashMap<String, Relational> copiedRelationals = new HashMap<String, Relational>();

        // WFs
        for (final WF wf : wfs) {
            final WF wfCopy = new WF(wf, kaf.getAnnotationContainer());
            kaf.insertWF(wfCopy);
            copiedWFs.put(wf.getId(), wfCopy);
        }
        // Terms
        for (final Term term : terms) {
            final Term termCopy = new Term(term, copiedWFs);
            kaf.insertTerm(termCopy);
            copiedTerms.put(term.getId(), termCopy);
        }
        // Deps
        for (final Dep dep : deps) {
            final Dep depCopy = new Dep(dep, copiedTerms);
            kaf.insertDep(depCopy);
        }
        // Chunks
        for (final Chunk chunk : chunks) {
            final Chunk chunkCopy = new Chunk(chunk, copiedTerms);
            kaf.insertChunk(chunkCopy);
        }
        // Entities
        for (final Entity entity : entities) {
            final Entity entityCopy = new Entity(entity, copiedTerms);
            kaf.insertEntity(entityCopy);
            copiedRelationals.put(entity.getId(), entityCopy);
        }
        // Coreferences
        for (final Coref coref : corefs) {
            final Coref corefCopy = new Coref(coref, copiedTerms);
            kaf.insertCoref(corefCopy);
        }
        // TimeExpressions
        for (final Timex3 timex3 : timeExs) {
            final Timex3 timex3Copy = new Timex3(timex3, copiedWFs);
            kaf.insertTimex3(timex3Copy);
        }
        // Properties
        for (final Feature property : properties) {
            final Feature propertyCopy = new Feature(property, copiedTerms);
            kaf.insertProperty(propertyCopy);
            copiedRelationals.put(property.getId(), propertyCopy);
        }
        // Categories
        for (final Feature category : categories) {
            final Feature categoryCopy = new Feature(category, copiedTerms);
            kaf.insertCategory(categoryCopy);
            copiedRelationals.put(category.getId(), categoryCopy);
        }
        // Opinions
        for (final Opinion opinion : opinions) {
            final Opinion opinionCopy = new Opinion(opinion, copiedTerms);
            kaf.insertOpinion(opinionCopy);
        }
        // Relations
        for (final Relation relation : relations) {
            final Relation relationCopy = new Relation(relation, copiedRelationals);
            kaf.insertRelation(relationCopy);
        }
        // Predicates
        /*
         * for (Predicate predicate : predicates) { Predicate predicateCopy = new
         * Predicate(predicate, copiedTerms); kaf.insertPredicate(predicateCopy); }
         */
    }

    /**
     * Returns a new document containing all annotations related to the given WFs
     */
    /*
     * Couldn't index opinion by terms. Terms are added after the Opinion object is created, and
     * there's no way to access the annotationContainer from the Opinion.
     */
    public KAFDocument split(final List<WF> wfs) {
        final List<Term> terms = this.annotationContainer.getTermsByWFs(wfs);
        final List<Dep> deps = this.annotationContainer.getDepsByTerms(terms);
        final List<Chunk> chunks = this.annotationContainer.getChunksByTerms(terms);
        final List<Entity> entities = this.annotationContainer.getEntitiesByTerms(terms);
        final List<Coref> corefs = this.annotationContainer.getCorefsByTerms(terms);
        final List<Timex3> timeExs = this.annotationContainer.getTimeExsByWFs(wfs);
        final List<Feature> properties = this.annotationContainer.getPropertiesByTerms(terms);
        final List<Feature> categories = this.annotationContainer.getCategoriesByTerms(terms);
        // List<Opinion> opinions = this.annotationContainer.getOpinionsByTerms(terms);
        final List<Predicate> predicates = this.annotationContainer.getPredicatesByTerms(terms);
        final List<Relational> relationals = new ArrayList<Relational>();
        relationals.addAll(properties);
        relationals.addAll(categories);
        relationals.addAll(entities);
        final List<Relation> relations = this.annotationContainer
                .getRelationsByRelationals(relationals);

        final KAFDocument newKaf = new KAFDocument(getLang(), getVersion());
        newKaf.addLinguisticProcessors(getLinguisticProcessors());
        copyAnnotationsToKAF(newKaf, wfs, terms, deps, chunks, entities, corefs, timeExs,
                properties, categories, new ArrayList<Opinion>(), relations, predicates);

        return newKaf;
    }

    /**
     * Joins the document with another one. *
     */
    public void join(final KAFDocument doc) {
        final HashMap<String, WF> copiedWFs = new HashMap<String, WF>(); // hash[old_id =>
                                                                         // new_WF_obj]
        final HashMap<String, Term> copiedTerms = new HashMap<String, Term>(); // hash[old_id =>
                                                                               // new_Term_obj]
        final HashMap<String, Relational> copiedRelationals = new HashMap<String, Relational>();
        // Linguistic processors
        final Map<String, List<LinguisticProcessor>> lps = doc.getLinguisticProcessors();
        for (final Map.Entry<String, List<LinguisticProcessor>> entry : lps.entrySet()) {
            final String layer = entry.getKey();
            final List<LinguisticProcessor> lpList = entry.getValue();
            for (final LinguisticProcessor lp : lpList) {
                if (!this.linguisticProcessorExists(layer, lp.name, lp.version)) {
                    // Here it uses a deprecated method
                    this.addLinguisticProcessor(layer, lp.name, lp.timestamp, lp.version);
                }
            }
        }
        // WFs
        for (final WF wf : doc.getWFs()) {
            final WF wfCopy = new WF(wf, this.annotationContainer);
            insertWF(wfCopy);
            copiedWFs.put(wf.getId(), wfCopy);
        }
        // Terms
        for (final Term term : doc.getTerms()) {
            final Term termCopy = new Term(term, copiedWFs);
            insertTerm(termCopy);
            copiedTerms.put(term.getId(), termCopy);
        }
        // Deps
        for (final Dep dep : doc.getDeps()) {
            final Dep depCopy = new Dep(dep, copiedTerms);
            insertDep(depCopy);
        }
        // Chunks
        for (final Chunk chunk : doc.getChunks()) {
            final Chunk chunkCopy = new Chunk(chunk, copiedTerms);
            insertChunk(chunkCopy);
        }
        // Entities
        for (final Entity entity : doc.getEntities()) {
            final Entity entityCopy = new Entity(entity, copiedTerms);
            insertEntity(entityCopy);
            copiedRelationals.put(entity.getId(), entityCopy);
        }
        // Coreferences
        for (final Coref coref : doc.getCorefs()) {
            final Coref corefCopy = new Coref(coref, copiedTerms);
            insertCoref(corefCopy);
        }
        // TimeExpressions
        for (final Timex3 timex3 : doc.getTimeExs()) {
            final Timex3 timex3Copy = new Timex3(timex3, copiedWFs);
            insertTimex3(timex3Copy);
        }
        // Properties
        for (final Feature property : doc.getProperties()) {
            final Feature propertyCopy = new Feature(property, copiedTerms);
            insertProperty(propertyCopy);
            copiedRelationals.put(property.getId(), propertyCopy);
        }
        // Categories
        for (final Feature category : doc.getCategories()) {
            final Feature categoryCopy = new Feature(category, copiedTerms);
            insertCategory(categoryCopy);
            copiedRelationals.put(category.getId(), categoryCopy);
        }
        // Opinions
        for (final Opinion opinion : doc.getOpinions()) {
            final Opinion opinionCopy = new Opinion(opinion, copiedTerms);
            insertOpinion(opinionCopy);
        }
        // Relations
        for (final Relation relation : doc.getRelations()) {
            final Relation relationCopy = new Relation(relation, copiedRelationals);
            insertRelation(relationCopy);
        }
    }

    public String insertWF(final WF wf) {
        final String newId = this.idManager.wfs.getNext();
        wf.setId(newId);
        this.annotationContainer.add(wf);
        return newId;
    }

    public String insertTerm(final Term term) {
        final String newId = this.idManager.terms.getNext();
        term.setId(newId);
        this.annotationContainer.add(term);
        return newId;
    }

    public void insertDep(final Dep dep) {
        this.annotationContainer.add(dep);
    }

    public String insertChunk(final Chunk chunk) {
        final String newId = this.idManager.chunks.getNext();
        chunk.setId(newId);
        this.annotationContainer.add(chunk);
        return newId;
    }

    public String insertEntity(final Entity entity) {
        final String newId = this.idManager.entities.getNext();
        entity.setId(newId);
        this.annotationContainer.add(entity);
        return newId;
    }

    public String insertCoref(final Coref coref) {
        final String newId = this.idManager.corefs.getNext();
        coref.setId(newId);
        this.annotationContainer.add(coref);
        return newId;
    }

    public String insertTimex3(final Timex3 timex3) {
        final String newId = this.idManager.timex3s.getNext();
        timex3.setId(newId);
        this.annotationContainer.add(timex3);
        return newId;
    }

    public String insertProperty(final Feature property) {
        final String newId = this.idManager.properties.getNext();
        property.setId(newId);
        this.annotationContainer.add(property);
        return newId;
    }

    public String insertCategory(final Feature category) {
        final String newId = this.idManager.categories.getNext();
        category.setId(newId);
        this.annotationContainer.add(category);
        return newId;
    }

    public String insertOpinion(final Opinion opinion) {
        final String newId = this.idManager.opinions.getNext();
        opinion.setId(newId);
        this.annotationContainer.add(opinion);
        return newId;
    }

    public String insertRelation(final Relation relation) {
        final String newId = this.idManager.relations.getNext();
        relation.setId(newId);
        this.annotationContainer.add(relation);
        return newId;
    }

    /**
     * Saves the KAF document to an XML file.
     * 
     * @param filename
     *            name of the file in which the document will be saved.
     */
    public void save(final String filename) {
        ReadWriteManager.save(this, filename);
    }

    @Override
    public String toString() {
        return ReadWriteManager.kafToStr(this);
    }

    /**
     * Prints the document on standard output.
     */
    public void print() {
        ReadWriteManager.print(this);
    }

    /**************************/
    /*** DEPRECATED METHODS ***/
    /**************************/

    /**
     * Deprecated
     */
    public LinguisticProcessor addLinguisticProcessor(final String layer, final String name,
            final String version) {
        final LinguisticProcessor lp = this.addLinguisticProcessor(layer, name);
        lp.setVersion(version);
        return lp;
    }

    /**
     * Deprecated
     */
    public LinguisticProcessor addLinguisticProcessor(final String layer, final String name,
            final String timestamp, final String version) {
        final LinguisticProcessor lp = this.addLinguisticProcessor(layer, name);
        lp.setTimestamp(timestamp);
        lp.setVersion(version);
        return lp;
    }

    /**
     * Deprecated
     */
    public WF newWF(final String id, final String form) {
        return this.newWF(id, form, 0);
    }

    /**
     * Deprecated
     */
    public WF newWF(final String form) {
        return this.newWF(form, 0);
    }

    /**
     * Deprecated
     */
    public WF createWF(final String id, final String form) {
        return this.newWF(id, form, 0);
    }

    /**
     * Deprecated
     */
    public WF createWF(final String form) {
        return this.newWF(form, 0);
    }

    /**
     * Deprecated
     */
    public WF createWF(final String form, final int offset) {
        return this.newWF(form, offset);
    }

    /**
     * Deprecated
     */
    public Term newTerm(final String id, final String type, final String lemma, final String pos,
            final Span<WF> span) {
        final Term term = newTerm(id, span);
        term.setType(type);
        term.setLemma(lemma);
        term.setPos(pos);
        return term;
    }

    /**
     * Deprecated
     */
    public Term newTerm(final String type, final String lemma, final String pos,
            final Span<WF> span) {
        final Term term = newTerm(span);
        term.setType(type);
        term.setLemma(lemma);
        term.setPos(pos);
        return term;
    }

    /**
     * Deprecated
     */
    public Term newTermOptions(final String type, final String lemma, final String pos,
            final String morphofeat, final Span<WF> span) {
        final Term newTerm = newTermOptions(morphofeat, span);
        newTerm.setType(type);
        newTerm.setLemma(lemma);
        newTerm.setPos(pos);
        return newTerm;
    }

    /**
     * Deprecated
     */
    public Term createTerm(final String id, final String type, final String lemma,
            final String pos, final List<WF> wfs) {
        return this.newTerm(id, type, lemma, pos, KAFDocument.<WF>list2Span(wfs));
    }

    /**
     * Deprecated
     */
    public Term createTerm(final String type, final String lemma, final String pos,
            final List<WF> wfs) {
        return this.newTerm(type, lemma, pos, KAFDocument.<WF>list2Span(wfs));
    }

    /**
     * Deprecated
     */
    public Term createTermOptions(final String type, final String lemma, final String pos,
            final String morphofeat, final List<WF> wfs) {
        return this.newTermOptions(type, lemma, pos, morphofeat, KAFDocument.<WF>list2Span(wfs));
    }

    /**
     * Deprecated
     */
    public Term.Sentiment createSentiment() {
        return newSentiment();
    }

    /** Deprecated */
    /*
     * public Component newComponent(String id, Term term, String lemma, String pos) { Component
     * newComponent = this.newComponent(id, term); newComponent.setLemma(lemma);
     * newComponent.setPos(pos); return newComponent; }
     */

    /** Deprecated */

    /*
     * public Component newComponent(Term term, String lemma, String pos) { Term.Component
     * newComponent = this.newComponent(term); newComponent.setLemma(lemma);
     * newComponent.setPos(pos); return newComponent; }
     */

    /** Deprecated */
    /*
     * public Component createComponent(String id, Term term, String lemma, String pos) { return
     * this.newComponent(id, term, lemma, pos); }
     */

    /** Deprecated */
    /*
     * public Component createComponent(Term term, String lemma, String pos) { return
     * this.newComponent(term, lemma, pos); }
     */

    /**
     * Deprecated
     */
    public Dep createDep(final Term from, final Term to, final String rfunc) {
        return createDep(from, to, rfunc);
    }

    /**
     * Deprecated
     */
    public Chunk createChunk(final String id, final Term head, final String phrase,
            final List<Term> terms) {
        return this.newChunk(id, phrase, KAFDocument.<Term>list2Span(terms, head));
    }

    /**
     * Deprecated
     */
    public Chunk createChunk(final Term head, final String phrase, final List<Term> terms) {
        return this.newChunk(phrase, KAFDocument.<Term>list2Span(terms, head));
    }

    /**
     * Deprecated
     */
    public Entity createEntity(final String id, final String type,
            final List<List<Term>> references) {
        final List<Span<Term>> spanReferences = new ArrayList<Span<Term>>();
        for (final List<Term> list : references) {
            spanReferences.add(KAFDocument.list2Span(list));
        }
        final Entity entity = this.newEntity(id, spanReferences);
        entity.setType(type);
        return entity;
    }

    /**
     * Deprecated
     */
    public Entity createEntity(final String type, final List<List<Term>> references) {
        final List<Span<Term>> spanReferences = new ArrayList<Span<Term>>();
        for (final List<Term> list : references) {
            spanReferences.add(KAFDocument.list2Span(list));
        }
        final Entity entity = this.newEntity(spanReferences);
        entity.setType(type);
        return entity;
    }

    /**
     * Deprecated
     */
    public Coref createCoref(final String id, final List<List<Target>> references) {
        final List<Span<Term>> spanReferences = new ArrayList<Span<Term>>();
        for (final List<Target> list : references) {
            spanReferences.add(targetList2Span(list));
        }
        return this.newCoref(id, spanReferences);
    }

    /**
     * Deprecated
     */
    public Coref createCoref(final List<List<Target>> references) {
        final List<Span<Term>> spanReferences = new ArrayList<Span<Term>>();
        for (final List<Target> list : references) {
            spanReferences.add(targetList2Span(list));
        }
        return this.newCoref(spanReferences);
    }

    /**
     * Deprecated
     */
    public Feature createProperty(final String id, final String lemma,
            final List<List<Term>> references) {
        final List<Span<Term>> spanReferences = new ArrayList<Span<Term>>();
        for (final List<Term> list : references) {
            spanReferences.add(KAFDocument.list2Span(list));
        }
        return this.newProperty(id, lemma, spanReferences);
    }

    /**
     * Deprecated
     */
    public Feature createProperty(final String lemma, final List<List<Term>> references) {
        final List<Span<Term>> spanReferences = new ArrayList<Span<Term>>();
        for (final List<Term> list : references) {
            spanReferences.add(KAFDocument.list2Span(list));
        }
        return this.newProperty(lemma, spanReferences);
    }

    /**
     * Deprecated
     */
    public Feature createCategory(final String id, final String lemma,
            final List<List<Term>> references) {
        final List<Span<Term>> spanReferences = new ArrayList<Span<Term>>();
        for (final List<Term> list : references) {
            spanReferences.add(KAFDocument.list2Span(list));
        }
        return this.newCategory(id, lemma, spanReferences);
    }

    /**
     * Deprecated
     */
    public Feature createCategory(final String lemma, final List<List<Term>> references) {
        final List<Span<Term>> spanReferences = new ArrayList<Span<Term>>();
        for (final List<Term> list : references) {
            spanReferences.add(KAFDocument.list2Span(list));
        }
        return this.newCategory(lemma, spanReferences);
    }

    /**
     * Deprecated
     */
    public Opinion createOpinion() {
        return this.newOpinion();
    }

    /**
     * Deprecated
     */
    public Opinion createOpinion(final String id) {
        return this.newOpinion(id);
    }

    /**
     * Deprecated
     */
    public Relation createRelation(final Relational from, final Relational to) {
        return this.newRelation(from, to);
    }

    /**
     * Deprecated
     */
    public Relation createRelation(final String id, final Relational from, final Relational to) {
        return this.newRelation(id, from, to);
    }

    /**
     * Deprecated
     */
    public ExternalRef createExternalRef(final String resource, final String reference) {
        return newExternalRef(resource, reference);
    }

    /**
     * Deprecated. Creates a new target. This method is overloaded. Any target created by calling
     * this method won't be the head term.
     * 
     * @param term
     *            target term.
     * @return a new target.
     */
    public static Target createTarget(final Term term) {
        return new Target(term, false);
    }

    /**
     * Deprecated. Creates a new target. This method is overloaded. In this case, it receives a
     * boolean argument which defines whether the target term is the head or not.
     * 
     * @param term
     *            target term.
     * @param isHead
     *            a boolean argument which defines whether the target term is the head or not.
     * @return a new target.
     */
    public static Target createTarget(final Term term, final boolean isHead) {
        return new Target(term, isHead);
    }

    public void removeLayer(final Layer layer) {
        this.annotationContainer.removeLayer(layer);
    }

    public void removeAnnotation(Object annotation) {
        this.annotationContainer.removeAnnotation(annotation);
    }
    
    /**
     * Converts a List into a Span
     */
    static <T> Span<T> list2Span(final List<T> list) {
        final Span<T> span = new Span<T>();
        for (final T elem : list) {
            span.addTarget(elem);
        }
        return span;
    }

    /**
     * Converts a List into a Span
     */
    static <T> Span<T> list2Span(final List<T> list, final T head) {
        final Span<T> span = new Span<T>();
        for (final T elem : list) {
            if (head == elem) {
                span.addTarget(elem, true);
            } else {
                span.addTarget(elem);
            }
        }
        return span;
    }

    /**
     * Converts a Target list into a Span of terms
     */
    static Span<Term> targetList2Span(final List<Target> list) {
        final Span<Term> span = new Span<Term>();
        for (final Target target : list) {
            if (target.isHead()) {
                span.addTarget(target.getTerm(), true);
            } else {
                span.addTarget(target.getTerm());
            }
        }
        return span;
    }

    /**
     * Converts a Span into a Target list
     */
    static List<Target> span2TargetList(final Span<Term> span) {
        final List<Target> list = new ArrayList<Target>();
        for (final Term t : span.getTargets()) {
            list.add(KAFDocument.createTarget(t, t == span.getHead()));
        }
        return list;
    }

    /**
     * Deprecated. Returns a list of terms containing the word forms given on argument.
     * 
     * @param wfIds
     *            a list of word form IDs whose terms will be found.
     * @return a list of terms containing the given word forms.
     */
    public List<Term> getTermsFromWFs(final List<String> wfIds) {
        return this.annotationContainer.getTermsByWFIds(wfIds);
    }

    // ADDED BY FRANCESCO

    private static final Map<String, Character> DEP_PATH_CHARS = new HashMap<String, Character>();

    private static final Map<String, Pattern> DEP_PATH_REGEXS = new HashMap<String, Pattern>();

    private static char getDepPathChar(final String label) {
        final String key = label.toLowerCase();
        synchronized (DEP_PATH_CHARS) {
            Character letter = DEP_PATH_CHARS.get(key);
            if (letter == null) {
                letter = 'a';
                for (final Character ch : DEP_PATH_CHARS.values()) {
                    if (ch >= letter) {
                        letter = (char) (ch + 1);
                    }
                }
                DEP_PATH_CHARS.put(key, letter);
            }
            return letter;
        }
    }

    private static String getDepPathString(final Term from, final Iterable<Dep> path) {
        final StringBuilder builder = new StringBuilder("_");
        Term term = from; // current node in the path
        for (final Dep dep : path) {
            char prefix;
            if (dep.getFrom() == term) {
                prefix = '+';
                term = dep.getTo();
            } else {
                prefix = '-';
                term = dep.getFrom();
            }
            for (final String label : dep.getRfunc().split("-")) {
                final Character letter = getDepPathChar(label);
                builder.append(prefix).append(letter);
            }
            builder.append("_");
        }
        return builder.toString();
    }

    private static Pattern getDepPathRegex(String pattern) {
        synchronized (DEP_PATH_REGEXS) {
            Pattern regex = DEP_PATH_REGEXS.get(pattern);
            if (regex == null) {
                final StringBuilder builder = new StringBuilder();
                builder.append('_');
                int start = -1;
                pattern = pattern + " ";
                for (int i = 0; i < pattern.length(); ++i) {
                    final char ch = pattern.charAt(i);
                    if (Character.isLetter(ch) || ch == '-') {
                        if (start < 0) {
                            start = i;
                        }
                    } else {
                        if (start >= 0) {
                            final boolean inverse = pattern.charAt(start) == '-';
                            final String label = pattern.substring(inverse ? start + 1 : start, i);
                            final char letter = getDepPathChar(label);
                            builder.append("([^_]*")
                                    .append(Pattern.quote((inverse ? "-" : "+") + letter))
                                    .append("[^_]*_)");
                            start = -1;
                        }
                        if (!Character.isWhitespace(ch)) {
                            builder.append(ch);
                        }
                    }
                }
                regex = Pattern.compile(builder.toString());
                DEP_PATH_REGEXS.put(pattern, regex);
            }
            return regex;
        }
    }

    public boolean matchDepPath(final Term from, final Iterable<Dep> path, final String pattern) {
        final String pathString = getDepPathString(from, path);
        final Pattern pathRegex = getDepPathRegex(pattern);
        return pathRegex.matcher(pathString).matches();
    }

    public List<Dep> getDepPath(final Term from, final Term to) {
        if (from == to) {
            return Collections.emptyList();
        }
        final List<Dep> toPath = new ArrayList<Dep>();
        for (Dep dep = getDepToTerm(to); dep != null; dep = getDepToTerm(dep.getFrom())) {
            toPath.add(dep);
            if (dep.getFrom() == from) {
                Collections.reverse(toPath);
                return toPath;
            }
        }
        final List<Dep> fromPath = new ArrayList<Dep>();
        for (Dep dep = getDepToTerm(from); dep != null; dep = getDepToTerm(dep.getFrom())) {
            fromPath.add(dep);
            if (dep.getFrom() == to) {
                return fromPath;
            }
            for (int i = 0; i < toPath.size(); ++i) {
                if (dep.getFrom() == toPath.get(i).getFrom()) {
                    for (int j = i; j >= 0; --j) {
                        fromPath.add(toPath.get(j));
                    }
                    return fromPath;
                }
            }
        }
        return null; // unconnected nodes
    }

    public Dep getDepToTerm(final Term term) {
        for (final Dep dep : getDepsByTerm(term)) {
            if (dep.getTo() == term) {
                return dep;
            }
        }
        return null;
    }

    public List<Dep> getDepsFromTerm(final Term term) {
        final List<Dep> result = new ArrayList<Dep>();
        for (final Dep dep : getDepsByTerm(term)) {
            if (dep.getFrom() == term) {
                result.add(dep);
            }
        }
        return result;
    }

    public List<Dep> getDepsByTerm(final Term term) {
        return this.annotationContainer.getDepsByTerm(term);
    }

    public Term getTermsHead(final Iterable<Term> descendents) {
        final Set<Term> termSet = new HashSet<Term>();
        for (final Term term : descendents) {
            termSet.add(term);
        }
        Term root = null;
        for (final Term term : termSet) {
            final Dep dep = getDepToTerm(term);
            if (dep == null || !termSet.contains(dep.getFrom())) {
                if (root == null) {
                    root = term;
                } else if (root != term) {
                    return null;
                }
            }
        }
        return root;
    }

    public Set<Term> getTermsByDepAncestors(final Iterable<Term> ancestors) {
        final Set<Term> terms = new HashSet<Term>();
        final List<Term> queue = new LinkedList<Term>();
        for (final Term term : ancestors) {
            terms.add(term);
            queue.add(term);
        }
        while (!queue.isEmpty()) {
            final Term term = queue.remove(0);
            final List<Dep> deps = getDepsByTerm(term);
            for (final Dep dep : deps) {
                if (dep.getFrom() == term) {
                    if (terms.add(dep.getTo())) {
                        queue.add(dep.getTo());
                    }
                }
            }
        }
        return terms;
    }

    public Set<Term> getTermsByDepAncestors(final Iterable<Term> ancestors, final String pattern) {
        final Set<Term> result = new HashSet<Term>();
        for (final Term term : ancestors) {
            for (final Term descendent : getTermsByDepAncestors(Collections.singleton(term))) {
                final List<Dep> path = getDepPath(term, descendent);
                if (matchDepPath(term, path, pattern)) {
                    result.add(descendent);
                }
            }
        }
        return result;
    }

    public Set<Term> getTermsByDepDescendants(Iterable<Term> descendents) {
        final Set<Term> terms = new HashSet<Term>();
        final List<Term> queue = new LinkedList<Term>();
        for (final Term term : descendents) {
            terms.add(term);
            queue.add(term);
        }
        while (!queue.isEmpty()) {
            final Term term = queue.remove(0);
            final List<Dep> deps = getDepsByTerm(term);
            for (final Dep dep : deps) {
                if (dep.getTo() == term) {
                    if (terms.add(dep.getFrom())) {
                        queue.add(dep.getFrom());
                    }
                }
            }
        }
        return terms;
    }

    public Set<Term> getTermsByDepDescendants(Iterable<Term> descendents, String pattern) {
        Set<Term> result = new HashSet<Term>();
        for (final Term term : descendents) {
            for (final Term ancestor : getTermsByDepDescendants(Collections.singleton(term))) {
                final List<Dep> path = getDepPath(term, ancestor);
                if (matchDepPath(term, path, pattern)) {
                    result.add(ancestor);
                }
            }
        }
        return result;
    }

    public List<Entity> getEntitiesByTerm(final Term term) {
        return this.annotationContainer.getEntitiesByTerm(term);
    }

    public List<Predicate> getPredicates() {
        return this.annotationContainer.getPredicates();
    }

    public List<Predicate> getPredicatesByTerm(final Term term) {
        return this.annotationContainer.getPredicatesByTerm(term);
    }

    public List<Coref> getCorefsByTerm(final Term term) {
        return this.annotationContainer.getCorefsByTerm(term);
    }

    public List<Timex3> getTimeExsBySent(final Integer sent) {
        final List<Timex3> timexs = this.annotationContainer.timeExsIndexedBySent.get(sent);
        return timexs == null ? new ArrayList<Timex3>() : timexs;
    }

    public List<Timex3> getTimeExsByWF(final WF wf) {
        return this.annotationContainer.getTimeExsByWF(wf);
    }

    public List<Factuality> getFactualities() {
        return this.annotationContainer.getFactualities();
    }

}
