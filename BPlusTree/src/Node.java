import java.util.ArrayList;

public class Node<K extends Comparable<K>, T> {
	protected boolean isLeafNode;
	//某一个node的keys,这个node指向的children的node
	protected ArrayList<K> keys;


	public boolean isOverflowed() {
		return keys.size() > 2 * BPlusTree.D;
	}

	public boolean isUnderflowed() {
		return keys.size() < BPlusTree.D;
	}

}
