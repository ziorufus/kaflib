package ixa.kaflib;

import java.util.List;
import java.util.ArrayList;
import java.io.Serializable;

public class Span<T> implements Serializable {
	@Override
	public String toString() {
		return "Span{" +
				"targets=" + targets +
				'}';
	}

	//private List<String> targets;
    private List<T> targets;
    private T head;

    Span() {
	this.targets = new ArrayList<T>();
	this.head = null;
    }

    Span(List<T> targets) {
	this.targets = targets;
	this.head = null;
    }

    Span(List<T> targets, T head) {
	this.targets = targets;
	this.head = head;
    }

    public boolean isEmpty() {
	return (this.targets.size() <= 0);
    }

    public String getStr() {
        StringBuilder builder = new StringBuilder();
        for (Object term : targets) {
            if (builder.length() != 0) {
                builder.append(' ');
            }
            if (term instanceof Term) {
                builder.append(((Term) term).getStr());
            } else if (term instanceof WF) {
                builder.append(((WF) term).getForm());
            } else {
                builder.append(term.toString());
            }
        }
        return builder.toString();
    }
    
    public List<T> getTargets() {
	return this.targets;
    }

    public T getFirstTarget() {
	return this.targets.get(0);
    }

    public boolean hasHead() {
	return (this.head != null);
    }

    public T getHead() {
	return this.head;
    }

    public boolean isHead(T target) {
	return (target == this.head);
    }

    public void setHead(T head) {
	this.head = head;
    }

    public void addTarget(T target) {
	this.targets.add(target);
    }

    public void addTarget(T target, boolean isHead) {
	this.targets.add(target);
	if (isHead) {
	    this.head = target;
	}
    }

    public void addTargets(List<T> targets) {
	this.targets.addAll(targets);
    }

    public boolean hasTarget(T target) {
	for (T t : targets) {
	    if (t == target) {
		return true;
	    }
	}
	return false;
    }

    public int size() {
	return this.targets.size();
    }
}
