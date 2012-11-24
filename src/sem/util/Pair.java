package sem.util;

/**
 * Item pair
 * 
 * @param <T> Type of the items
 */
public class Pair<T>{
	private T item1;
	private T item2;
	
	public Pair(T item1, T item2){
		this.item1 = item1;
		this.item2 = item2;
	}
	
	public T getItem1(){
		return this.item1;
	}
	
	public T getItem2(){
		return this.item2;
	}
}
