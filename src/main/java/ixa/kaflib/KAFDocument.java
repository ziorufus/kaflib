package ixa.kaflib;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.io.File;
import java.io.Reader;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;
import java.io.IOException;

import org.jdom2.JDOMException;
import org.jdom2.Element;


/**
 * Respresents a KAF document. It's the main class of the library, as it keeps all elements of the document (word forms, terms, entities...) and manages all object creations. The document can be created by the user calling it's methods, or loading from an existing XML file.
 */

public class KAFDocument implements Serializable {

	public enum Layer {
		text, terms, marks, deps, chunks, entities, properties, categories, coreferences, opinions, relations, srl, constituency;
	}

	public class FileDesc implements Serializable {
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
		public String publicId;
		public String uri;

		private Public() {
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
	private Map<String, List<LinguisticProcessor>> lps;

	private FileDesc fileDesc;

	private Public _public;

	/**
	 * Identifier manager
	 */
	private IdManager idManager;

	/**
	 * Keeps all the annotations of the document
	 */
	private AnnotationContainer annotationContainer;

	/**
	 * Creates an empty KAFDocument element
	 */
	public KAFDocument(String lang, String version) {
		this.lang = lang;
		this.version = version;
		lps = new LinkedHashMap<String, List<LinguisticProcessor>>();
		idManager = new IdManager();
		annotationContainer = new AnnotationContainer();
	}

	/**
	 * Creates a new KAFDocument and loads the contents of the file passed as argument
	 *
	 * @param file an existing KAF file to be loaded into the library.
	 */
	public static KAFDocument createFromFile(File file) throws IOException {
		KAFDocument kaf = null;
		try {
			kaf = ReadWriteManager.load(file);
		} catch (JDOMException e) {
			e.printStackTrace();
		}
		return kaf;
	}

	/**
	 * Creates a new KAFDocument loading the content read from the reader given on argument.
	 *
	 * @param stream Reader to read KAF content.
	 */
	public static KAFDocument createFromStream(Reader stream) throws IOException {
		KAFDocument kaf = null;
		try {
			kaf = ReadWriteManager.load(stream);
		} catch (JDOMException e) {
			e.printStackTrace();
		}
		return kaf;
	}

	/**
	 * Sets the language of the processed document
	 */
	public void setLang(String lang) {
		this.lang = lang;
	}

	/**
	 * Returns the language of the processed document
	 */
	public String getLang() {
		return lang;
	}

	/**
	 * Sets the KAF version
	 */
	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * Returns the KAF version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Adds a linguistic processor to the document header. The timestamp is added implicitly.
	 */
	public LinguisticProcessor addLinguisticProcessor(String layer, String name) {
		LinguisticProcessor lp = new LinguisticProcessor(layer, name);
		List<LinguisticProcessor> layerLps = lps.get(layer);
		if (layerLps == null) {
			layerLps = new ArrayList<LinguisticProcessor>();
			lps.put(layer, layerLps);
		}
		layerLps.add(lp);
		return lp;
	}

	public LinguisticProcessor addLinguisticProcessor(String layer, LinguisticProcessor linguisticProcessor) {
		List<LinguisticProcessor> layerLps = lps.get(layer);
		if (layerLps == null) {
			layerLps = new ArrayList<LinguisticProcessor>();
			lps.put(layer, layerLps);
		}
		layerLps.add(linguisticProcessor);
		return linguisticProcessor;
	}

	public void addLinguisticProcessors(Map<String, List<LinguisticProcessor>> lps) {
		for (Map.Entry<String, List<LinguisticProcessor>> entry : lps.entrySet()) {
			List<LinguisticProcessor> layerLps = entry.getValue();
			for (LinguisticProcessor lp : layerLps) {
				LinguisticProcessor newLp = this.addLinguisticProcessor(entry.getKey(), lp.name);
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
	 * Returns a hash of linguistic processors from the document.
	 * Hash: layer => LP
	 */
	public Map<String, List<LinguisticProcessor>> getLinguisticProcessors() {
		return lps;
	}

	/**
	 * Returns wether the given linguistic processor is already defined or not. Both name and version must be exactly the same.
	 */
	public boolean linguisticProcessorExists(String layer, String name, String version) {
		List<LinguisticProcessor> layerLPs = lps.get(layer);
		if (layerLPs == null) {
			return false;
		}
		for (LinguisticProcessor lp : layerLPs) {
			if (lp.version == null) {
				return false;
			}
			else if (lp.name.equals(name) && lp.version.equals(version)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns wether the given linguistic processor is already defined or not. Both name and version must be exactly the same.
	 */
	public boolean linguisticProcessorExists(String layer, String name) {
		List<LinguisticProcessor> layerLPs = lps.get(layer);
		if (layerLPs == null) {
			return false;
		}
		for (LinguisticProcessor lp : layerLPs) {
			if (lp.version != null) {
				return false;
			}
			else if (lp.name.equals(name)) {
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
		return annotationContainer;
	}

	/**
	 * Set raw text *
	 */
	public void setRawText(String rawText) {
		annotationContainer.setRawText(rawText);
	}

	/**
	 * Creates a WF object to load an existing word form. It receives the ID as an argument. The WF is added to the document object.
	 *
	 * @param id   word form's ID.
	 * @param form text of the word form itself.
	 * @return a new word form.
	 */
	public WF newWF(String id, String form, int sent) {
		idManager.wfs.update(id);
		WF newWF = new WF(this.annotationContainer, id, form, sent);
		annotationContainer.add(newWF);
		return newWF;
	}

	/**
	 * Creates a new WF object. It assigns an appropriate ID to it and it also assigns offset and length
	 * attributes. The WF is added to the document object.
	 *
	 * @param form text of the word form itself.
	 * @return a new word form.
	 */
	public WF newWF(String form, int offset) {
		String newId = idManager.wfs.getNext();
		int offsetVal = offset;
		WF newWF = new WF(this.annotationContainer, newId, form, 0);
		newWF.setOffset(offsetVal);
		newWF.setLength(form.length());
		annotationContainer.add(newWF);
		return newWF;
	}

	/**
	 * Creates a new WF object. It assigns an appropriate ID to it.  The WF is added to the document object.
	 *
	 * @param form text of the word form itself.
	 * @return a new word form.
	 */
	public WF newWF(String form, int offset, int sent) {
		String newId = idManager.wfs.getNext();
		WF newWF = new WF(this.annotationContainer, newId, form, sent);
		newWF.setOffset(offset);
		newWF.setLength(form.length());
		annotationContainer.add(newWF);
		return newWF;
	}

	/**
	 * Creates a Term object to load an existing term. It receives the ID as an argument. The Term is added to the document object.
	 *
	 * @param id    term's ID.
	 * @param type  type of term. There are two types of term: open and close.
	 * @param lemma the lemma of the term.
	 * @param pos   part of speech of the term.
	 * @param wfs   the list of word forms this term is formed by.
	 * @return a new term.
	 */
	public Term newTerm(String id, Span<WF> span) {
		idManager.terms.update(id);
		Term newTerm = new Term(id, span, false);
		annotationContainer.add(newTerm);
		return newTerm;
	}

	public Term newTerm(String id, Span<WF> span, boolean isComponent) {
		idManager.terms.update(id);
		Term newTerm = new Term(id, span, isComponent);
		if (!isComponent) {
			annotationContainer.add(newTerm);
		}
		return newTerm;
	}

	public Term newTerm(String id, Span<WF> span, Integer position) {
		idManager.terms.update(id);
		Term newTerm = new Term(id, span, false);
		annotationContainer.add(newTerm, position);
		return newTerm;
	}

	/**
	 * Creates a new Term. It assigns an appropriate ID to it. The Term is added to the document object.
	 *
	 * @param type  the type of the term. There are two types of term: open and close.
	 * @param lemma the lemma of the term.
	 * @param pos   part of speech of the term.
	 * @param wfs   the list of word forms this term is formed by.
	 * @return a new term.
	 */
	public Term newTerm(Span<WF> span) {
		String newId = idManager.terms.getNext();
		Term newTerm = new Term(newId, span, false);
		annotationContainer.add(newTerm);
		return newTerm;
	}

	/**
	 * Creates a new Term. It assigns an appropriate ID to it. The Term is added to the document object.
	 *
	 * @param type  the type of the term. There are two types of term: open and close.
	 * @param lemma the lemma of the term.
	 * @param pos   part of speech of the term.
	 * @param wfs   the list of word forms this term is formed by.
	 * @return a new term.
	 */
	public Term newTermOptions(String morphofeat, Span<WF> span) {
		String newId = idManager.terms.getNext();
		Term newTerm = new Term(newId, span, false);
		newTerm.setMorphofeat(morphofeat);
		annotationContainer.add(newTerm);
		return newTerm;
	}

	public Term newCompound(List<Term> terms, String lemma) {
		Span<WF> span = new Span<WF>();
		for (Term term : terms) {
			span.addTargets(term.getSpan().getTargets());
		}
		String newId = idManager.mws.getNext();
		Term compound = newTerm(newId, span, annotationContainer.termPosition(terms.get(0)));
		compound.setLemma(lemma);
		for (Term term : terms) {
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
		Term.Sentiment newSentiment = new Term.Sentiment();
		return newSentiment;
	}

	public Mark newMark(String id, String source, Span<Term> span) {
		idManager.marks.update(id);
		Mark newMark = new Mark(id, span);
		annotationContainer.add(newMark, source);
		return newMark;
	}

	public Mark newMark(String source, Span<Term> span) {
		String newId = idManager.marks.getNext();
		Mark newMark = new Mark(newId, span);
		annotationContainer.add(newMark, source);
		return newMark;
	}

	/**
	 * Creates a new dependency. The Dep is added to the document object.
	 *
	 * @param from  the origin term of the dependency.
	 * @param to    the target term of the dependency.
	 * @param rfunc relational function of the dependency.
	 * @return a new dependency.
	 */
	public Dep newDep(Term from, Term to, String rfunc) {
		Dep newDep = new Dep(from, to, rfunc);
		annotationContainer.add(newDep);
		return newDep;
	}

	/**
	 * Creates a chunk object to load an existing chunk. It receives it's ID as an argument. The Chunk is added to the document object.
	 *
	 * @param id     chunk's ID.
	 * @param head   the chunk head.
	 * @param phrase type of the phrase.
	 * @param terms  the list of the terms in the chunk.
	 * @return a new chunk.
	 */
	public Chunk newChunk(String id, String phrase, Span<Term> span) {
		idManager.chunks.update(id);
		Chunk newChunk = new Chunk(id, span);
		newChunk.setPhrase(phrase);
		annotationContainer.add(newChunk);
		return newChunk;
	}

	/**
	 * Creates a new chunk. It assigns an appropriate ID to it. The Chunk is added to the document object.
	 *
	 * @param head   the chunk head.
	 * @param phrase type of the phrase.
	 * @param terms  the list of the terms in the chunk.
	 * @return a new chunk.
	 */
	public Chunk newChunk(String phrase, Span<Term> span) {
		String newId = idManager.chunks.getNext();
		Chunk newChunk = new Chunk(newId, span);
		newChunk.setPhrase(phrase);
		annotationContainer.add(newChunk);
		return newChunk;
	}

	/**
	 * Creates an Entity object to load an existing entity. It receives the ID as an argument. The entity is added to the document object.
	 *
	 * @param id         the ID of the named entity.
	 * @param type       entity type. 8 values are posible: Person, Organization, Location, Date, Time, Money, Percent, Misc.
	 * @param references it contains one or more span elements. A span can be used to reference the different occurrences of the same named entity in the document. If the entity is composed by multiple words, multiple target elements are used.
	 * @return a new named entity.
	 */
	public Entity newEntity(String id, List<Span<Term>> references) {
		idManager.entities.update(id);
		Entity newEntity = new Entity(id, references);
		annotationContainer.add(newEntity);
		return newEntity;
	}

	/**
	 * Creates a new Entity. It assigns an appropriate ID to it. The entity is added to the document object.
	 *
	 * @param type       entity type. 8 values are posible: Person, Organization, Location, Date, Time, Money, Percent, Misc.
	 * @param references it contains one or more span elements. A span can be used to reference the different occurrences of the same named entity in the document. If the entity is composed by multiple words, multiple target elements are used.
	 * @return a new named entity.
	 */
	public Entity newEntity(List<Span<Term>> references) {
		String newId = idManager.entities.getNext();
		Entity newEntity = new Entity(newId, references);
		annotationContainer.add(newEntity);
		return newEntity;
	}

	/**
	 * Creates a coreference object to load an existing Coref. It receives it's ID as an argument. The Coref is added to the document.
	 *
	 * @param id         the ID of the coreference.
	 * @param references different mentions (list of targets) to the same entity.
	 * @return a new coreference.
	 */
	public Coref newCoref(String id, List<Span<Term>> mentions) {
		idManager.corefs.update(id);
		Coref newCoref = new Coref(id, mentions);
		annotationContainer.add(newCoref);
		return newCoref;
	}

	/**
	 * Creates a new coreference. It assigns an appropriate ID to it. The Coref is added to the document.
	 *
	 * @param references different mentions (list of targets) to the same entity.
	 * @return a new coreference.
	 */
	public Coref newCoref(List<Span<Term>> mentions) {
		String newId = idManager.corefs.getNext();
		Coref newCoref = new Coref(newId, mentions);
		annotationContainer.add(newCoref);
		return newCoref;
	}

	/**
	 * Creates a timeExpressions object to load an existing Timex3. It receives it's ID as an argument. The Timex3 is added to the document.
	 *
	 * @param id         the ID of the coreference.
	 * @param references different mentions (list of targets) to the same entity.
	 * @return a new timex3.
	 */
	public Timex3 newTimex3(String id, List<Span<WF>> mentions) {
		idManager.timex3s.update(id);
		Timex3 newTimex3 = new Timex3(id, mentions);
		annotationContainer.add(newTimex3);
		return newTimex3;
	}

	/**
	 * Creates a new timeExpressions. It assigns an appropriate ID to it. The Coref is added to the document.
	 *
	 * @param references different mentions (list of targets) to the same entity.
	 * @return a new timex3.
	 */
	public Timex3 newTimex3(List<Span<WF>> mentions) {
		String newId = idManager.timex3s.getNext();
		Timex3 newTimex3 = new Timex3(newId, mentions);
		annotationContainer.add(newTimex3);
		return newTimex3;
	}

	/**
	 * Creates a new timeExpressions. It assigns an appropriate ID to it. The Coref is added to the document.
	 *
	 * @param references different mentions (list of targets) to the same entity.
	 * @return a new timex3.
	 */
	public Timex3 newTimex3(String id) {
		idManager.timex3s.update(id);
		Timex3 newTimex3 = new Timex3(id);
		annotationContainer.add(newTimex3);
		return newTimex3;
	}

	/**
	 * Creates a factualitylayer object and add it to the document
	 *
	 * @param term the Term of the coreference.
	 * @return a new factuality.
	 */
	public Factuality newFactuality(Term term) {
		Factuality factuality = new Factuality(term);
		annotationContainer.add(factuality);
		return factuality;
	}
	
	/**
     * Creates a LinkedEntity object and add it to the document, using the supplied ID.
     *
     * @param id the entity ID
     * @param term the Term of the coreference
     * @return a new factuality
     */
    public LinkedEntity newLinkedEntity(String id, Span<WF> span) {
        LinkedEntity linkedEntity = new LinkedEntity(id, span);
        annotationContainer.add(linkedEntity);
        return linkedEntity;
    }

	/**
	 * Creates a LinkedEntity object and add it to the document
	 *
	 * @param term the Term of the coreference.
	 * @return a new factuality.
	 */
	public LinkedEntity newLinkedEntity(Span<WF> span) {
		String newId = idManager.linkedentities.getNext();
		LinkedEntity linkedEntity = new LinkedEntity(newId, span);
		annotationContainer.add(linkedEntity);
		return linkedEntity;
	}

	/**
	 * Creates a SSTspan object and add it to the document
	 *
	 * @param term the Term of the coreference.
	 * @return a new factuality.
	 */
	public SSTspan newSST(Span<Term> span) {
		String newId = idManager.ssts.getNext();
		SSTspan sst = new SSTspan(newId, span);
		annotationContainer.add(sst);
		return sst;
	}

	public SSTspan newSST(Span<Term> span, String type, String label) {
		String newId = idManager.ssts.getNext();
		SSTspan sst = new SSTspan(newId, span);
		sst.setLabel(label);
		sst.setType(type);
		annotationContainer.add(sst);
		return sst;
	}

	/**
	 * Creates a Topic object and add it to the document
	 *
	 * @param term the Term of the coreference.
	 * @return a new factuality.
	 */
	public Topic newTopic() {
		String newId = idManager.topics.getNext();
		Topic t = new Topic(newId);
		annotationContainer.add(t);
		return t;
	}

	public Topic newTopic(String label, float probability) {
		String newId = idManager.topics.getNext();
		Topic t = new Topic(newId);
		t.setLabel(label);
		t.setProbability(probability);
		annotationContainer.add(t);
		return t;
	}

	/**
	 * Creates a new property. It receives it's ID as an argument. The property is added to the document.
	 *
	 * @param id         the ID of the property.
	 * @param lemma      the lemma of the property.
	 * @param references different mentions (list of targets) to the same property.
	 * @return a new coreference.
	 */
	public Feature newProperty(String id, String lemma, List<Span<Term>> references) {
		idManager.properties.update(id);
		Feature newProperty = new Feature(id, lemma, references);
		annotationContainer.add(newProperty);
		return newProperty;
	}

	/**
	 * Creates a new property. It assigns an appropriate ID to it. The property is added to the document.
	 *
	 * @param lemma      the lemma of the property.
	 * @param references different mentions (list of targets) to the same property.
	 * @return a new coreference.
	 */
	public Feature newProperty(String lemma, List<Span<Term>> references) {
		String newId = idManager.properties.getNext();
		Feature newProperty = new Feature(newId, lemma, references);
		annotationContainer.add(newProperty);
		return newProperty;
	}

	/**
	 * Creates a new category. It receives it's ID as an argument. The category is added to the document.
	 *
	 * @param id         the ID of the category.
	 * @param lemma      the lemma of the category.
	 * @param references different mentions (list of targets) to the same category.
	 * @return a new coreference.
	 */
	public Feature newCategory(String id, String lemma, List<Span<Term>> references) {
		idManager.categories.update(id);
		Feature newCategory = new Feature(id, lemma, references);
		annotationContainer.add(newCategory);
		return newCategory;
	}

	/**
	 * Creates a new category. It assigns an appropriate ID to it. The category is added to the document.
	 *
	 * @param lemma      the lemma of the category.
	 * @param references different mentions (list of targets) to the same category.
	 * @return a new coreference.
	 */
	public Feature newCategory(String lemma, List<Span<Term>> references) {
		String newId = idManager.categories.getNext();
		Feature newCategory = new Feature(newId, lemma, references);
		annotationContainer.add(newCategory);
		return newCategory;
	}

	/**
	 * Creates a new opinion object. It assigns an appropriate ID to it. The opinion is added to the document.
	 *
	 * @return a new opinion.
	 */
	public Opinion newOpinion() {
		String newId = idManager.opinions.getNext();
		Opinion newOpinion = new Opinion(newId);
		annotationContainer.add(newOpinion);
		return newOpinion;
	}

	/**
	 * Creates a new opinion object. It receives its ID as an argument. The opinion is added to the document.
	 *
	 * @return a new opinion.
	 */
	public Opinion newOpinion(String id) {
		idManager.opinions.update(id);
		Opinion newOpinion = new Opinion(id);
		annotationContainer.add(newOpinion);
		return newOpinion;
	}

	/**
	 * Creates a new relation between entities and/or sentiment features. It assigns an appropriate ID to it. The relation is added to the document.
	 *
	 * @param from source of the relation
	 * @param to   target of the relation
	 * @return a new relation
	 */
	public Relation newRelation(Relational from, Relational to) {
		String newId = idManager.relations.getNext();
		Relation newRelation = new Relation(newId, from, to);
		annotationContainer.add(newRelation);
		return newRelation;
	}

	/**
	 * Creates a new relation between entities and/or sentiment features. It receives its ID as an argument. The relation is added to the document.
	 *
	 * @param id   the ID of the relation
	 * @param from source of the relation
	 * @param to   target of the relation
	 * @return a new relation
	 */
	public Relation newRelation(String id, Relational from, Relational to) {
		idManager.relations.update(id);
		Relation newRelation = new Relation(id, from, to);
		annotationContainer.add(newRelation);
		return newRelation;
	}

	/**
	 * Creates a new srl predicate. It receives its ID as an argument. The predicate is added to the document.
	 *
	 * @param id   the ID of the predicate
	 * @param span span containing the targets of the predicate
	 * @return a new predicate
	 */
	public Predicate newPredicate(String id, Span<Term> span) {
		idManager.predicates.update(id);
		Predicate newPredicate = new Predicate(id, span);
		annotationContainer.add(newPredicate);
		return newPredicate;
	}

	/**
	 * Creates a new srl predicate. It assigns an appropriate ID to it. The predicate is added to the document.
	 *
	 * @param span span containing all the targets of the predicate
	 * @return a new predicate
	 */
	public Predicate newPredicate(Span<Term> span) {
		String newId = idManager.predicates.getNext();
		Predicate newPredicate = new Predicate(newId, span);
		annotationContainer.add(newPredicate);
		return newPredicate;
	}

	/**
	 * Creates a Role object to load an existing role. It receives the ID as an argument. It doesn't add the role to the predicate.
	 *
	 * @param id        role's ID.
	 * @param predicate the predicate which this role is part of
	 * @param semRole   semantic role
	 * @param span      span containing all the targets of the role
	 * @return a new role.
	 */
	public Predicate.Role newRole(String id, Predicate predicate, String semRole, Span<Term> span) {
		idManager.roles.update(id);
		Predicate.Role newRole = new Predicate.Role(id, semRole, span);
		return newRole;
	}

	/**
	 * Creates a new Role object. It assigns an appropriate ID to it. It uses the ID of the predicate to create a new ID for the role. It doesn't add the role to the predicate.
	 *
	 * @param predicate the predicate which this role is part of
	 * @param semRole   semantic role
	 * @param span      span containing all the targets of the role
	 * @return a new role.
	 */
	public Predicate.Role newRole(Predicate predicate, String semRole, Span<Term> span) {
		String newId = idManager.roles.getNext();
		Predicate.Role newRole = new Predicate.Role(newId, semRole, span);
		return newRole;
	}

	/**
	 * Creates a new external reference.
	 *
	 * @param resource  indicates the identifier of the resource referred to.
	 * @param reference code of the referred element.
	 * @return a new external reference object.
	 */
	public ExternalRef newExternalRef(String resource, String reference) {
		return new ExternalRef(resource, reference);
	}

	public Tree newConstituent(TreeNode root) {
		Tree tree = new Tree(root);
		annotationContainer.add(tree);
		return tree;
	}

	public void addConstituencyFromParentheses(String parseOut) throws Exception {
		Tree.parenthesesToKaf(parseOut, this);
	}

	public NonTerminal newNonTerminal(String id, String label) {
		NonTerminal tn = new NonTerminal(id, label);
		String newEdgeId = idManager.edges.getNext();
		tn.setEdgeId(newEdgeId);
		return tn;
	}

	public NonTerminal newNonTerminal(String label) {
		String newId = idManager.nonterminals.getNext();
		String newEdgeId = idManager.edges.getNext();
		NonTerminal newNonterminal = new NonTerminal(newId, label);
		newNonterminal.setEdgeId(newEdgeId);
		return newNonterminal;
	}

	public Terminal newTerminal(String id, Span<Term> span) {
		Terminal tn = new Terminal(id, span);
		String newEdgeId = idManager.edges.getNext();
		tn.setEdgeId(newEdgeId);
		return tn;
	}

	public Terminal newTerminal(Span<Term> span) {
		String newId = idManager.terminals.getNext();
		String newEdgeId = idManager.edges.getNext();
		Terminal tn = new Terminal(newId, span);
		tn.setEdgeId(newEdgeId);
		return tn;
	}

	public static Span<WF> newWFSpan() {
		return new Span<WF>();
	}

	public static Span<WF> newWFSpan(List<WF> targets) {
		return new Span<WF>(targets);
	}

	public static Span<WF> newWFSpan(List<WF> targets, WF head) {
		return new Span<WF>(targets, head);
	}

	public static Span<Term> newTermSpan() {
		return new Span<Term>();
	}

	public static Span<Term> newTermSpan(List<Term> targets) {
		return new Span<Term>(targets);
	}

	public static Span<Term> newTermSpan(List<Term> targets, Term head) {
		return new Span<Term>(targets, head);
	}

	void addUnknownLayer(Element layer) {
		annotationContainer.add(layer);
	}

	/**
	 * Returns the raw text *
	 */
	public String getRawText() {
		return annotationContainer.getRawText();
	}

	/**
	 * Returns a list containing all WFs in the document
	 */
	public List<WF> getWFs() {
		return annotationContainer.getText();
	}

	/**
	 * Returns a list with all sentences. Each sentence is a list of WFs.
	 */
	public List<List<WF>> getSentences() {
		return annotationContainer.getSentences();
	}

	public Integer getFirstSentence() {
		return annotationContainer.getText().get(0).getSent();
	}

	public Integer getNumSentences() {
		List<WF> wfs = annotationContainer.getText();
		Integer firstSentence = wfs.get(0).getSent();
		Integer lastSentence = wfs.get(wfs.size() - 1).getSent();
		return lastSentence - firstSentence + 1;
	}

	public List<Integer> getSentsByParagraph(Integer para) {
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
		return annotationContainer.getTerms();
	}

	/**
	 * Returns a list of terms containing the word forms given on argument.
	 *
	 * @param wfs a list of word forms whose terms will be found.
	 * @return a list of terms containing the given word forms.
	 */
	public List<Term> getTermsByWFs(List<WF> wfs) {
		return annotationContainer.getTermsByWFs(wfs);
	}

	public List<Term> getSentenceTerms(int sent) {
		return annotationContainer.getSentenceTerms(sent);
	}

	public List<String> getMarkSources() {
		return annotationContainer.getMarkSources();
	}

	public List<Mark> getMarks(String source) {
		return annotationContainer.getMarks(source);
	}

	public List<Dep> getDeps() {
		return annotationContainer.getDeps();
	}

	public List<Chunk> getChunks() {
		return annotationContainer.getChunks();
	}

	/**
	 * Returns a list with all entities in the document
	 */
	public List<Entity> getEntities() {
		return annotationContainer.getEntities();
	}

	public List<Coref> getCorefs() {
		return annotationContainer.getCorefs();
	}

	public List<Timex3> getTimeExs() {
		return annotationContainer.getTimeExs();
	}

	/**
	 * Returns a list with all relations in the document
	 */
	public List<Feature> getProperties() {
		return annotationContainer.getProperties();
	}

	/**
	 * Returns a list with all relations in the document
	 */
	public List<Feature> getCategories() {
		return annotationContainer.getCategories();
	}

	public List<Opinion> getOpinions() {
		return annotationContainer.getOpinions();
	}

	/**
	 * Returns a list with all relations in the document
	 */
	public List<Relation> getRelations() {
		return annotationContainer.getRelations();
	}

	public List<Tree> getConstituents() {
		return annotationContainer.getConstituents();
	}

	public List<Element> getUnknownLayers() {
		return annotationContainer.getUnknownLayers();
	}

	public List<WF> getWFsBySent(Integer sent) {
		List<WF> wfs = this.annotationContainer.textIndexedBySent.get(sent);
		return (wfs == null) ? new ArrayList<WF>() : wfs;
	}

	public List<WF> getWFsByPara(Integer para) {
		return this.annotationContainer.getLayerByPara(para, this.annotationContainer.textIndexedBySent);
	}

	public List<Term> getTermsBySent(Integer sent) {
		List<Term> terms = this.annotationContainer.termsIndexedBySent.get(sent);
		return (terms == null) ? new ArrayList<Term>() : terms;
	}

	public List<Term> getTermsByPara(Integer para) {
		return this.annotationContainer.getLayerByPara(para, this.annotationContainer.termsIndexedBySent);
	}

	public List<Entity> getEntitiesBySent(Integer sent) {
		List<Entity> entities = this.annotationContainer.entitiesIndexedBySent.get(sent);
		return (entities == null) ? new ArrayList<Entity>() : entities;
	}

	public List<Entity> getEntitiesByPara(Integer para) {
		return this.annotationContainer.getLayerByPara(para, this.annotationContainer.entitiesIndexedBySent);
	}

	public List<Dep> getDepsBySent(Integer sent) {
		return this.annotationContainer.depsIndexedBySent.get(sent);
	}

	public List<Dep> getDepsByPara(Integer para) {
		return this.annotationContainer.getLayerByPara(para, this.annotationContainer.depsIndexedBySent);
	}

	public List<Chunk> getChunksBySent(Integer sent) {
		return this.annotationContainer.chunksIndexedBySent.get(sent);
	}

	public List<Chunk> getChunksByPara(Integer para) {
		return this.annotationContainer.getLayerByPara(para, this.annotationContainer.chunksIndexedBySent);
	}

	public List<Predicate> getPredicatesBySent(Integer sent) {
		List<Predicate> result = this.annotationContainer.predicatesIndexedBySent.get(sent);
		return result != null ? result : Collections.<Predicate>emptyList(); 
	}

	public List<Predicate> getPredicatesByPara(Integer para) {
		return this.annotationContainer.getLayerByPara(para, this.annotationContainer.predicatesIndexedBySent);
	}

	/**
	 * Copies the annotations to another KAF document
	 */
	private void copyAnnotationsToKAF(KAFDocument kaf,
									  List<WF> wfs,
									  List<Term> terms,
									  List<Dep> deps,
									  List<Chunk> chunks,
									  List<Entity> entities,
									  List<Coref> corefs,
									  List<Timex3> timeExs,
									  List<Feature> properties,
									  List<Feature> categories,
									  List<Opinion> opinions,
									  List<Relation> relations,
									  List<Predicate> predicates
	) {
		HashMap<String, WF> copiedWFs = new HashMap<String, WF>();
		HashMap<String, Term> copiedTerms = new HashMap<String, Term>();
		HashMap<String, Relational> copiedRelationals = new HashMap<String, Relational>();

		// WFs
		for (WF wf : wfs) {
			WF wfCopy = new WF(wf, kaf.getAnnotationContainer());
			kaf.insertWF(wfCopy);
			copiedWFs.put(wf.getId(), wfCopy);
		}
		// Terms
		for (Term term : terms) {
			Term termCopy = new Term(term, copiedWFs);
			kaf.insertTerm(termCopy);
			copiedTerms.put(term.getId(), termCopy);
		}
		// Deps
		for (Dep dep : deps) {
			Dep depCopy = new Dep(dep, copiedTerms);
			kaf.insertDep(depCopy);
		}
		// Chunks
		for (Chunk chunk : chunks) {
			Chunk chunkCopy = new Chunk(chunk, copiedTerms);
			kaf.insertChunk(chunkCopy);
		}
		// Entities
		for (Entity entity : entities) {
			Entity entityCopy = new Entity(entity, copiedTerms);
			kaf.insertEntity(entityCopy);
			copiedRelationals.put(entity.getId(), entityCopy);
		}
		// Coreferences
		for (Coref coref : corefs) {
			Coref corefCopy = new Coref(coref, copiedTerms);
			kaf.insertCoref(corefCopy);
		}
		// TimeExpressions
		for (Timex3 timex3 : timeExs) {
			Timex3 timex3Copy = new Timex3(timex3, copiedWFs);
			kaf.insertTimex3(timex3Copy);
		}
		// Properties
		for (Feature property : properties) {
			Feature propertyCopy = new Feature(property, copiedTerms);
			kaf.insertProperty(propertyCopy);
			copiedRelationals.put(property.getId(), propertyCopy);
		}
		// Categories
		for (Feature category : categories) {
			Feature categoryCopy = new Feature(category, copiedTerms);
			kaf.insertCategory(categoryCopy);
			copiedRelationals.put(category.getId(), categoryCopy);
		}
		// Opinions
		for (Opinion opinion : opinions) {
			Opinion opinionCopy = new Opinion(opinion, copiedTerms);
			kaf.insertOpinion(opinionCopy);
		}
		// Relations
		for (Relation relation : relations) {
			Relation relationCopy = new Relation(relation, copiedRelationals);
			kaf.insertRelation(relationCopy);
		}
		// Predicates
	/*
	for (Predicate predicate : predicates) {
	    Predicate predicateCopy = new Predicate(predicate, copiedTerms);
	    kaf.insertPredicate(predicateCopy);
	}
	*/
	}


	/**
	 * Returns a new document containing all annotations related to the given WFs
	 */
	/* Couldn't index opinion by terms. Terms are added after the Opinion object is created, and there's no way to access the annotationContainer from the Opinion.*/
	public KAFDocument split(List<WF> wfs) {
		List<Term> terms = this.annotationContainer.getTermsByWFs(wfs);
		List<Dep> deps = this.annotationContainer.getDepsByTerms(terms);
		List<Chunk> chunks = this.annotationContainer.getChunksByTerms(terms);
		List<Entity> entities = this.annotationContainer.getEntitiesByTerms(terms);
		List<Coref> corefs = this.annotationContainer.getCorefsByTerms(terms);
		List<Timex3> timeExs = this.annotationContainer.getTimeExsByWFs(wfs);
		List<Feature> properties = this.annotationContainer.getPropertiesByTerms(terms);
		List<Feature> categories = this.annotationContainer.getCategoriesByTerms(terms);
		// List<Opinion> opinions = this.annotationContainer.getOpinionsByTerms(terms);
		List<Predicate> predicates = this.annotationContainer.getPredicatesByTerms(terms);
		List<Relational> relationals = new ArrayList<Relational>();
		relationals.addAll(properties);
		relationals.addAll(categories);
		relationals.addAll(entities);
		List<Relation> relations = this.annotationContainer.getRelationsByRelationals(relationals);

		KAFDocument newKaf = new KAFDocument(this.getLang(), this.getVersion());
		newKaf.addLinguisticProcessors(this.getLinguisticProcessors());
		this.copyAnnotationsToKAF(newKaf, wfs, terms, deps, chunks, entities, corefs, timeExs, properties, categories, new ArrayList<Opinion>(), relations, predicates);

		return newKaf;
	}

	/**
	 * Joins the document with another one. *
	 */
	public void join(KAFDocument doc) {
		HashMap<String, WF> copiedWFs = new HashMap<String, WF>(); // hash[old_id => new_WF_obj]
		HashMap<String, Term> copiedTerms = new HashMap<String, Term>(); // hash[old_id => new_Term_obj]
		HashMap<String, Relational> copiedRelationals = new HashMap<String, Relational>();
		// Linguistic processors
		Map<String, List<LinguisticProcessor>> lps = doc.getLinguisticProcessors();
		for (Map.Entry<String, List<LinguisticProcessor>> entry : lps.entrySet()) {
			String layer = entry.getKey();
			List<LinguisticProcessor> lpList = entry.getValue();
			for (LinguisticProcessor lp : lpList) {
				if (!this.linguisticProcessorExists(layer, lp.name, lp.version)) {
					// Here it uses a deprecated method
					this.addLinguisticProcessor(layer, lp.name, lp.timestamp, lp.version);
				}
			}
		}
		// WFs
		for (WF wf : doc.getWFs()) {
			WF wfCopy = new WF(wf, this.annotationContainer);
			this.insertWF(wfCopy);
			copiedWFs.put(wf.getId(), wfCopy);
		}
		// Terms
		for (Term term : doc.getTerms()) {
			Term termCopy = new Term(term, copiedWFs);
			this.insertTerm(termCopy);
			copiedTerms.put(term.getId(), termCopy);
		}
		// Deps
		for (Dep dep : doc.getDeps()) {
			Dep depCopy = new Dep(dep, copiedTerms);
			this.insertDep(depCopy);
		}
		// Chunks
		for (Chunk chunk : doc.getChunks()) {
			Chunk chunkCopy = new Chunk(chunk, copiedTerms);
			this.insertChunk(chunkCopy);
		}
		// Entities
		for (Entity entity : doc.getEntities()) {
			Entity entityCopy = new Entity(entity, copiedTerms);
			this.insertEntity(entityCopy);
			copiedRelationals.put(entity.getId(), entityCopy);
		}
		// Coreferences
		for (Coref coref : doc.getCorefs()) {
			Coref corefCopy = new Coref(coref, copiedTerms);
			this.insertCoref(corefCopy);
		}
		// TimeExpressions
		for (Timex3 timex3 : doc.getTimeExs()) {
			Timex3 timex3Copy = new Timex3(timex3, copiedWFs);
			this.insertTimex3(timex3Copy);
		}
		// Properties
		for (Feature property : doc.getProperties()) {
			Feature propertyCopy = new Feature(property, copiedTerms);
			this.insertProperty(propertyCopy);
			copiedRelationals.put(property.getId(), propertyCopy);
		}
		// Categories
		for (Feature category : doc.getCategories()) {
			Feature categoryCopy = new Feature(category, copiedTerms);
			this.insertCategory(categoryCopy);
			copiedRelationals.put(category.getId(), categoryCopy);
		}
		// Opinions
		for (Opinion opinion : doc.getOpinions()) {
			Opinion opinionCopy = new Opinion(opinion, copiedTerms);
			this.insertOpinion(opinionCopy);
		}
		// Relations
		for (Relation relation : doc.getRelations()) {
			Relation relationCopy = new Relation(relation, copiedRelationals);
			this.insertRelation(relationCopy);
		}
	}

	public String insertWF(WF wf) {
		String newId = idManager.wfs.getNext();
		wf.setId(newId);
		annotationContainer.add(wf);
		return newId;
	}

	public String insertTerm(Term term) {
		String newId = idManager.terms.getNext();
		term.setId(newId);
		annotationContainer.add(term);
		return newId;
	}

	public void insertDep(Dep dep) {
		annotationContainer.add(dep);
	}

	public String insertChunk(Chunk chunk) {
		String newId = idManager.chunks.getNext();
		chunk.setId(newId);
		annotationContainer.add(chunk);
		return newId;
	}

	public String insertEntity(Entity entity) {
		String newId = idManager.entities.getNext();
		entity.setId(newId);
		annotationContainer.add(entity);
		return newId;
	}

	public String insertCoref(Coref coref) {
		String newId = idManager.corefs.getNext();
		coref.setId(newId);
		annotationContainer.add(coref);
		return newId;
	}

	public String insertTimex3(Timex3 timex3) {
		String newId = idManager.timex3s.getNext();
		timex3.setId(newId);
		annotationContainer.add(timex3);
		return newId;
	}

	public String insertProperty(Feature property) {
		String newId = idManager.properties.getNext();
		property.setId(newId);
		annotationContainer.add(property);
		return newId;
	}

	public String insertCategory(Feature category) {
		String newId = idManager.categories.getNext();
		category.setId(newId);
		annotationContainer.add(category);
		return newId;
	}

	public String insertOpinion(Opinion opinion) {
		String newId = idManager.opinions.getNext();
		opinion.setId(newId);
		annotationContainer.add(opinion);
		return newId;
	}

	public String insertRelation(Relation relation) {
		String newId = idManager.relations.getNext();
		relation.setId(newId);
		annotationContainer.add(relation);
		return newId;
	}

	/**
	 * Saves the KAF document to an XML file.
	 *
	 * @param filename name of the file in which the document will be saved.
	 */
	public void save(String filename) {
		ReadWriteManager.save(this, filename);
	}

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
	public LinguisticProcessor addLinguisticProcessor(String layer, String name, String version) {
		LinguisticProcessor lp = this.addLinguisticProcessor(layer, name);
		lp.setVersion(version);
		return lp;
	}

	/**
	 * Deprecated
	 */
	public LinguisticProcessor addLinguisticProcessor(String layer, String name, String timestamp, String version) {
		LinguisticProcessor lp = this.addLinguisticProcessor(layer, name);
		lp.setTimestamp(timestamp);
		lp.setVersion(version);
		return lp;
	}

	/**
	 * Deprecated
	 */
	public WF newWF(String id, String form) {
		return this.newWF(id, form, 0);
	}

	/**
	 * Deprecated
	 */
	public WF newWF(String form) {
		return this.newWF(form, 0);
	}

	/**
	 * Deprecated
	 */
	public WF createWF(String id, String form) {
		return this.newWF(id, form, 0);
	}

	/**
	 * Deprecated
	 */
	public WF createWF(String form) {
		return this.newWF(form, 0);
	}

	/**
	 * Deprecated
	 */
	public WF createWF(String form, int offset) {
		return this.newWF(form, offset);
	}

	/**
	 * Deprecated
	 */
	public Term newTerm(String id, String type, String lemma, String pos, Span<WF> span) {
		Term term = newTerm(id, span);
		term.setType(type);
		term.setLemma(lemma);
		term.setPos(pos);
		return term;
	}

	/**
	 * Deprecated
	 */
	public Term newTerm(String type, String lemma, String pos, Span<WF> span) {
		Term term = newTerm(span);
		term.setType(type);
		term.setLemma(lemma);
		term.setPos(pos);
		return term;
	}

	/**
	 * Deprecated
	 */
	public Term newTermOptions(String type, String lemma, String pos, String morphofeat, Span<WF> span) {
		Term newTerm = newTermOptions(morphofeat, span);
		newTerm.setType(type);
		newTerm.setLemma(lemma);
		newTerm.setPos(pos);
		return newTerm;
	}

	/**
	 * Deprecated
	 */
	public Term createTerm(String id, String type, String lemma, String pos, List<WF> wfs) {
		return this.newTerm(id, type, lemma, pos, this.<WF>list2Span(wfs));
	}

	/**
	 * Deprecated
	 */
	public Term createTerm(String type, String lemma, String pos, List<WF> wfs) {
		return this.newTerm(type, lemma, pos, this.<WF>list2Span(wfs));
	}

	/**
	 * Deprecated
	 */
	public Term createTermOptions(String type, String lemma, String pos, String morphofeat, List<WF> wfs) {
		return this.newTermOptions(type, lemma, pos, morphofeat, this.<WF>list2Span(wfs));
	}

	/**
	 * Deprecated
	 */
	public Term.Sentiment createSentiment() {
		return this.newSentiment();
	}

	/** Deprecated */
    /*
    public Component newComponent(String id, Term term, String lemma, String pos) {
	Component newComponent = this.newComponent(id, term);
	newComponent.setLemma(lemma);
	newComponent.setPos(pos);
	return newComponent;
    }
    */

	/** Deprecated */
    
    /*public Component newComponent(Term term, String lemma, String pos) {
	Term.Component newComponent = this.newComponent(term);
	newComponent.setLemma(lemma);
	newComponent.setPos(pos);
	return newComponent;
    }
    */

	/** Deprecated */
    /*
    public Component createComponent(String id, Term term, String lemma, String pos) {
	return this.newComponent(id, term, lemma, pos);
    }
    */

	/** Deprecated */
    /*
      public Component createComponent(Term term, String lemma, String pos) {
	return this.newComponent(term, lemma, pos);
    }
    */

	/**
	 * Deprecated
	 */
	public Dep createDep(Term from, Term to, String rfunc) {
		return this.createDep(from, to, rfunc);
	}

	/**
	 * Deprecated
	 */
	public Chunk createChunk(String id, Term head, String phrase, List<Term> terms) {
		return this.newChunk(id, phrase, this.<Term>list2Span(terms, head));
	}

	/**
	 * Deprecated
	 */
	public Chunk createChunk(Term head, String phrase, List<Term> terms) {
		return this.newChunk(phrase, this.<Term>list2Span(terms, head));
	}

	/**
	 * Deprecated
	 */
	public Entity createEntity(String id, String type, List<List<Term>> references) {
		List<Span<Term>> spanReferences = new ArrayList<Span<Term>>();
		for (List<Term> list : references) {
			spanReferences.add(this.list2Span(list));
		}
		Entity entity = this.newEntity(id, spanReferences);
		entity.setType(type);
		return entity;
	}

	/**
	 * Deprecated
	 */
	public Entity createEntity(String type, List<List<Term>> references) {
		List<Span<Term>> spanReferences = new ArrayList<Span<Term>>();
		for (List<Term> list : references) {
			spanReferences.add(this.list2Span(list));
		}
		Entity entity = this.newEntity(spanReferences);
		entity.setType(type);
		return entity;
	}

	/**
	 * Deprecated
	 */
	public Coref createCoref(String id, List<List<Target>> references) {
		List<Span<Term>> spanReferences = new ArrayList<Span<Term>>();
		for (List<Target> list : references) {
			spanReferences.add(this.targetList2Span(list));
		}
		return this.newCoref(id, spanReferences);
	}

	/**
	 * Deprecated
	 */
	public Coref createCoref(List<List<Target>> references) {
		List<Span<Term>> spanReferences = new ArrayList<Span<Term>>();
		for (List<Target> list : references) {
			spanReferences.add(this.targetList2Span(list));
		}
		return this.newCoref(spanReferences);
	}

	/**
	 * Deprecated
	 */
	public Feature createProperty(String id, String lemma, List<List<Term>> references) {
		List<Span<Term>> spanReferences = new ArrayList<Span<Term>>();
		for (List<Term> list : references) {
			spanReferences.add(this.list2Span(list));
		}
		return this.newProperty(id, lemma, spanReferences);
	}

	/**
	 * Deprecated
	 */
	public Feature createProperty(String lemma, List<List<Term>> references) {
		List<Span<Term>> spanReferences = new ArrayList<Span<Term>>();
		for (List<Term> list : references) {
			spanReferences.add(this.list2Span(list));
		}
		return this.newProperty(lemma, spanReferences);
	}

	/**
	 * Deprecated
	 */
	public Feature createCategory(String id, String lemma, List<List<Term>> references) {
		List<Span<Term>> spanReferences = new ArrayList<Span<Term>>();
		for (List<Term> list : references) {
			spanReferences.add(this.list2Span(list));
		}
		return this.newCategory(id, lemma, spanReferences);
	}

	/**
	 * Deprecated
	 */
	public Feature createCategory(String lemma, List<List<Term>> references) {
		List<Span<Term>> spanReferences = new ArrayList<Span<Term>>();
		for (List<Term> list : references) {
			spanReferences.add(this.list2Span(list));
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
	public Opinion createOpinion(String id) {
		return this.newOpinion(id);
	}

	/**
	 * Deprecated
	 */
	public Relation createRelation(Relational from, Relational to) {
		return this.newRelation(from, to);
	}

	/**
	 * Deprecated
	 */
	public Relation createRelation(String id, Relational from, Relational to) {
		return this.newRelation(id, from, to);
	}

	/**
	 * Deprecated
	 */
	public ExternalRef createExternalRef(String resource, String reference) {
		return this.newExternalRef(resource, reference);
	}

	/**
	 * Deprecated. Creates a new target. This method is overloaded. Any target created by calling this method won't be the head term.
	 *
	 * @param term target term.
	 * @return a new target.
	 */
	public static Target createTarget(Term term) {
		return new Target(term, false);
	}

	/**
	 * Deprecated. Creates a new target. This method is overloaded. In this case, it receives a boolean argument which defines whether the target term is the head or not.
	 *
	 * @param term   target term.
	 * @param isHead a boolean argument which defines whether the target term is the head or not.
	 * @return a new target.
	 */
	public static Target createTarget(Term term, boolean isHead) {
		return new Target(term, isHead);
	}

	public void removeLayer(Layer layer) {
		this.annotationContainer.removeLayer(layer);
	}

	/**
	 * Converts a List into a Span
	 */
	static <T> Span<T> list2Span(List<T> list) {
		Span<T> span = new Span<T>();
		for (T elem : list) {
			span.addTarget(elem);
		}
		return span;
	}

	/**
	 * Converts a List into a Span
	 */
	static <T> Span<T> list2Span(List<T> list, T head) {
		Span<T> span = new Span<T>();
		for (T elem : list) {
			if (head == elem) {
				span.addTarget(elem, true);
			}
			else {
				span.addTarget(elem);
			}
		}
		return span;
	}

	/**
	 * Converts a Target list into a Span of terms
	 */
	static Span<Term> targetList2Span(List<Target> list) {
		Span<Term> span = new Span<Term>();
		for (Target target : list) {
			if (target.isHead()) {
				span.addTarget(target.getTerm(), true);
			}
			else {
				span.addTarget(target.getTerm());
			}
		}
		return span;
	}

	/**
	 * Converts a Span into a Target list
	 */
	static List<Target> span2TargetList(Span<Term> span) {
		List<Target> list = new ArrayList<Target>();
		for (Term t : span.getTargets()) {
			list.add(KAFDocument.createTarget(t, (t == span.getHead())));
		}
		return list;
	}

	/**
	 * Deprecated. Returns a list of terms containing the word forms given on argument.
	 *
	 * @param wfIds a list of word form IDs whose terms will be found.
	 * @return a list of terms containing the given word forms.
	 */
	public List<Term> getTermsFromWFs(List<String> wfIds) {
		return annotationContainer.getTermsByWFIds(wfIds);
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
			}
			else {
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

	private static Pattern getDepPathRegex(final String pattern) {
		synchronized (DEP_PATH_REGEXS) {
			Pattern regex = DEP_PATH_REGEXS.get(pattern);
			if (regex == null) {
				final StringBuilder builder = new StringBuilder();
				builder.append('_');
				int start = -1;
				for (int i = 0; i < pattern.length(); ++i) {
					final char ch = pattern.charAt(i);
					if (Character.isLetter(ch) || ch == '-') {
						if (start < 0) {
							start = i;
						}
					}
					else {
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
		String pathString = getDepPathString(from, path);
		Pattern pathRegex = getDepPathRegex(pattern);
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

	public Term getTermsHead(Iterable<Term> descendents) {
		final Set<Term> termSet = new HashSet<Term>();
		for (Term term : descendents) {
			termSet.add(term);
		}
		Term root = null;
		for (final Term term : termSet) {
			final Dep dep = getDepToTerm(term);
			if (dep == null || !termSet.contains(dep.getFrom())) {
				if (root == null) {
					root = term;
				}
				else if (root != term) {
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

	public List<Entity> getEntitiesByTerm(Term term) {
		return this.annotationContainer.getEntitiesByTerm(term);
	}


	public List<Predicate> getPredicates() {
		return this.annotationContainer.getPredicates();
	}

	public List<Predicate> getPredicatesByTerm(Term term) {
		return this.annotationContainer.getPredicatesByTerm(term);
	}

    public List<Coref> getCorefsByTerm(Term term) {
        return this.annotationContainer.getCorefsByTerm(term);
    }

    public List<Timex3> getTimeExsBySent(Integer sent) {
        List<Timex3> timexs = this.annotationContainer.timeExsIndexedBySent.get(sent);
        return (timexs == null) ? new ArrayList<Timex3>() : timexs;
    }

    public List<Factuality> getFactualities() {
        return annotationContainer.getFactualities();
    }
    
}
