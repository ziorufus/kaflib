package ixa.kaflib;


public abstract class IReferable implements Comparable<IReferable>
{
    public abstract String getId();

	@Override
	public int compareTo(IReferable o) {
		return this.getId().compareTo(o.getId());
	}
}
