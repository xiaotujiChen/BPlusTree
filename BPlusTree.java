import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map.Entry;


/**
 * BPlusTree Class Assumptions: 1. No duplicate keys inserted 2. Order D:
 * D<=number of keys in a node <=2*D 3. All keys are non-negative
 * TODO: Rename to BPlusTree
 */
public class BPlusTree<K extends Comparable<K>, T> {

    public Node<K, T> root;
    public static final int D = 2;

    /**
     * TODO Search the value for a specific key
     *
     * @param key
     * @return value
     */
    public T search(K key) {
        if (root == null) return null;
        LeafNode<K, T> leafNode = (LeafNode<K, T>) tree_search(root, key);
        int i = 0;
        int size = leafNode.keys.size();
        while (!leafNode.keys.get(i).equals(key)) {
            if (i == size - 1) return null;
            i++;
        }
        return leafNode.values.get(i);
    }


    /**
     * find the LeafNode that this key is in
     *
     * @param node
     * @param key
     * @return the LeafNode that contains this key
     */
    public Node<K, T> tree_search(Node<K, T> node, K key) {
        //if node is a LeafNode,
        if (node.isLeafNode) return node;
        IndexNode<K, T> indexNode = (IndexNode<K, T>) node;
        Entry<Integer, Node<K, T>> childNode = getChildNode(indexNode, key);
        return tree_search(childNode.getValue(), key);
    }

    /**
     * TODO Insert a key/value pair into the BPlusTree
     *
     * @param key
     * @param value
     */
    public void insert(K key, T value) {
        if (key == null) return;
        if (root == null) {
            //the tree is empty
            root = new LeafNode<K, T>(key, value);

        } else { // the tree has a root
            Entry<K, Node<K, T>> e = tree_insert(root, key, value);
            if (e != null) {
                root = new IndexNode(e.getKey(), root, e.getValue());
            }
        }
    }


    /**
     * insert key and value into node
     *
     * @param node
     * @param key
     * @param value
     * @return null if no overflow happens, else return entry of the new key and new node
     */
    public Entry<K, Node<K, T>> tree_insert(Node<K, T> node, K key, T value) {
        if (!node.isLeafNode) { // the node is IndexNode
            //find i such that Ki <= entry's key < Ki+1
            IndexNode<K, T> indexNode = (IndexNode<K, T>) node;
            Entry<Integer, Node<K, T>> childNode = getChildNode(indexNode, key);
            Entry<K, Node<K, T>> e = tree_insert(childNode.getValue(), key, value);
            if (e == null) {
                // no insertion from children's split
                return null;
            } else {
                // children split into two node
                indexNode.insertSorted(e, childNode.getKey());// insert new key and new node into this IndexNode
                if (!indexNode.isOverflowed()) {
                    // there is space in this IndexNode, don't need to split
                    return null;
                } else {
                    // there is no space in this IndexNode, need to split
                    Entry<K, Node<K, T>> entry = splitIndexNode(indexNode);
                    return entry;
                }
            }
        } else { // the node is LeafNode
            LeafNode<K, T> leafNode = (LeafNode<K, T>) node;
            leafNode.insertSorted(key, value);
            if (!leafNode.isOverflowed()) {
                // leafNode don't need to split
                return null;
            } else {
                // leafNode is overflowed and need to split
                Entry<K, Node<K, T>> entry = splitLeafNode(leafNode);
                return entry;
            }
        }
    }


    /**
     * TODO Split a leaf node and return the new right node and the splitting
     * key as an Entry<slitingKey, RightNode>
     *
     * @param leaf, any other relevant data
     * @return the key/node pair as an Entry
     */
    public Entry<K, Node<K, T>> splitLeafNode(LeafNode<K, T> leaf) {
        ArrayList<K> splitKeys = new ArrayList<>();
        ArrayList<T> splitValues = new ArrayList<>();
        while (leaf.keys.size() > D) { // keep only D keys in the leaf node
            splitKeys.add(leaf.keys.remove(D));
            splitValues.add(leaf.values.remove(D));
        }
        LeafNode<K, T> splitLeafNode = new LeafNode<K, T>(splitKeys, splitValues);
        // set nextLeaf and previousLeaf fields
        if (leaf.nextLeaf == null) {
            leaf.nextLeaf = splitLeafNode;
            splitLeafNode.previousLeaf = leaf;
        } else {
            LeafNode<K, T> next = leaf.nextLeaf;
            leaf.nextLeaf = splitLeafNode;
            splitLeafNode.nextLeaf = next;
            next.previousLeaf = splitLeafNode;
            splitLeafNode.previousLeaf = leaf;
        }
        return new AbstractMap.SimpleEntry<K, Node<K, T>>(splitLeafNode.keys.get(0), splitLeafNode);
    }

    /**
     * TODO split an indexNode and return the new right node and the splitting
     * key as an Entry<slitingKey, RightNode>
     *
     * @param index, any other relevant data
     * @return new key/node pair as an Entry
     */
    public Entry<K, Node<K, T>> splitIndexNode(IndexNode<K, T> index) {
        ArrayList<K> splitKeys = new ArrayList<>();
        ArrayList<Node<K, T>> newChildren = new ArrayList<>();
        //store the middle key
        K upKey = index.keys.remove(D);
        // D = 2 then from third children, they should be
        newChildren.add(index.children.remove(D + 1));
        // move the next D keys and children into new node
        while (index.keys.size() > D) {
            splitKeys.add(index.keys.remove(D));
            newChildren.add(index.children.remove(D + 1));
        }
        IndexNode<K, T> splitIndexNode = new IndexNode<K, T>(splitKeys, newChildren);
        return new AbstractMap.SimpleEntry<K, Node<K, T>>(upKey, splitIndexNode);
    }

    /**
     * TODO Delete a key/value pair from this B+Tree
     *
     * @param key
     */

    public void delete(K key) {
        if (key == null) return;
        // root is LeafNode
        if (root.isLeafNode) {
            //LeafNode<K, T> leafRoot = (LeafNode<K, T) root;
            int position = getPosition(root, key);
            if (position != -1) {
                root.keys.remove(position);
                ((LeafNode<K, T>) root).values.remove(position);
            }
        } else { // root is IndexNode
            Entry<Integer, Node<K, T>> childNode = getChildNode((IndexNode<K, T>) root, key);
            int rootKeySize = root.keys.size();
            int delete = tree_delete(root, childNode.getValue(), key);
            if (delete != -1) {
                if (rootKeySize > 1) {
                    root.keys.remove(delete);
                    ((IndexNode<K, T>) root).children.remove(delete);
                } else {
                    root = ((IndexNode<K, T>) root).children.get(1);
                }
            }
        }
    }

    /**
     * @param parent
     * @param node
     * @param key
     * @return index of key in parent that need to be delete, return -1 do nothing
     */
    public int tree_delete(Node<K, T> parent, Node<K, T> node, K key) {
        // the node is IndexNode
        if (!node.isLeafNode) {
            IndexNode<K, T> indexNode = (IndexNode<K, T>) node;
            Entry<Integer, Node<K, T>> childNode = getChildNode(indexNode, key);
            int delete = tree_delete(indexNode, childNode.getValue(), key);
            if (delete == -1) { // there is no underFlow happened
                return -1;
            } else { // underFlow happened
                indexNode.keys.remove(delete);
                indexNode.children.remove(delete);
                if (!indexNode.isUnderflowed()) {
                    return -1;
                } else {
                    Node<K, T>[] siblingNodes = getSibling(parent, indexNode);
                    if (siblingNodes[0] != null) {
                        // this node has left sibling
                        return handleIndexNodeUnderflow(indexNode,
                                (IndexNode<K, T>) siblingNodes[0], (IndexNode<K, T>) parent);
                    } else {
                        // this node only has right sibling
                        return handleIndexNodeUnderflow(indexNode,
                                (IndexNode<K, T>) siblingNodes[1], (IndexNode<K, T>) parent);
                    }
                }
            }
        } else { // the node is LeafNode
            LeafNode<K, T> leafNode = (LeafNode<K, T>) node;
            int position = getPosition(leafNode, key);
            // if there is no key in this tree, do nothing, otherwise, delete key and value
            if (position == -1) return -1;
            leafNode.keys.remove(position);
            leafNode.values.remove(position);
            if (!leafNode.isUnderflowed()) {
                return -1;
            } else {
                Node<K, T>[] siblingNodes = getSibling(parent, leafNode);
                //merge or redistribution
                if (siblingNodes[0] != null) {
                    // this node has left sibling
                    return handleLeafNodeUnderflow((LeafNode<K, T>) siblingNodes[0],
                            leafNode, (IndexNode<K, T>) parent);
                } else {
                    // this node only has right sibling
                    return handleLeafNodeUnderflow(leafNode,
                            (LeafNode<K, T>) siblingNodes[1], (IndexNode<K, T>) parent);
                }
            }
        }
    }

    /**
     * TODO Handle LeafNode Underflow (merge or redistribution)
     *
     * @param left   : the smaller node
     * @param right  : the bigger node
     * @param parent : their parent index node
     * @return the splitkey position in parent if merged so that parent can
     * delete the splitkey later on. -1 otherwise
     */
    public int handleLeafNodeUnderflow(LeafNode<K, T> left, LeafNode<K, T> right,
                                       IndexNode<K, T> parent) {
        // redistribution
        if ((left.keys.size() + right.keys.size()) >= 2 * D) {
            // sibling is right node
            if (left.isUnderflowed()) {
                while (left.keys.size() < D) {
                    left.keys.add(right.keys.remove(0));
                    left.values.add(right.values.remove(0));
                }
            } else { // sibling is left node
                while (left.keys.size() > D) {
                    right.keys.add(0, left.keys.remove(left.keys.size() - 1));
                    right.values.add(0, left.values.remove(left.values.size() - 1));
                }
            }
            // change the key in parent node
            parent.keys.set(parent.children.indexOf(left), right.keys.get(0));
            return -1;
        } else { // merge
            right.keys.addAll(0, left.keys);
            right.values.addAll(0, left.values);
            LeafNode<K, T> prevNode = left.previousLeaf;
            if (prevNode != null) {
                prevNode.nextLeaf = right;
            }
            right.previousLeaf = prevNode;
            return parent.children.indexOf(left);
        }
    }

    /**
     * TODO Handle IndexNode Underflow (merge or redistribution)
     *
     * @param leftIndex  : the smaller node
     * @param rightIndex : the bigger node
     * @param parent     : their parent index node
     * @return the splitkey position in parent if merged so that parent can
     * delete the splitkey later on. -1 otherwise
     */
    public int handleIndexNodeUnderflow(IndexNode<K, T> leftIndex,
                                        IndexNode<K, T> rightIndex, IndexNode<K, T> parent) {
        // get the key that point to the leftIndex Node in parent Node
        int index = parent.children.indexOf(leftIndex);
        K parentKey = parent.keys.get(index);
        // redistribution
        if ((leftIndex.keys.size() + rightIndex.keys.size()) >= 2 * D) {
            if (leftIndex.isUnderflowed()) {
                leftIndex.keys.add(parentKey);
                leftIndex.children.add(rightIndex.children.remove(0));
                while (leftIndex.keys.size() < D) {
                    leftIndex.keys.add(rightIndex.keys.remove(0));
                    leftIndex.children.add(rightIndex.children.remove(0));
                }
                parent.keys.set(index, rightIndex.keys.remove(0));
            } else {
                rightIndex.keys.add(parentKey);
                rightIndex.children.add(0, leftIndex.children.remove(leftIndex.children.size() - 1));
                while (leftIndex.keys.size() > D + 1) {
                    rightIndex.keys.add(0, leftIndex.keys.remove(leftIndex.keys.size() - 1));
                    rightIndex.children.add(0, leftIndex.children.remove(leftIndex.children.size() - 1));
                }
                parent.keys.set(index, leftIndex.keys.remove(leftIndex.keys.size() - 1));
            }
            return -1;
        } else { // merge
            rightIndex.keys.add(0, parentKey);
            rightIndex.keys.addAll(0, leftIndex.keys);
            rightIndex.children.addAll(0, leftIndex.children);
            return index;
        }

    }


    /**
     * get the key node in LeafNodes
     *
     * @param node
     * @param key
     * @return the child node and the index of key position
     */
    public Entry<Integer, Node<K, T>> getChildNode(IndexNode<K, T> node, K key) {
        int size = node.keys.size();
        int index = 0;
        Node<K, T> n = null;

        if (key.compareTo(node.keys.get(0)) < 0) {
            // the key is less than this node's minimum key
            n = (Node<K, T>) node.children.get(0);
            index = 0;
        } else if (key.compareTo(node.keys.get(size - 1)) >= 0) {
            // the key is larger than this node's maximum key
            n = (Node<K, T>) node.children.get(size);
            index = size;
        } else {
            // the key is between two keys of this node, loop to find the insertion position
            boolean find = false;
            for (int i = 0; i < size - 1; i++) {
                if (find) break;
                if (key.compareTo(node.keys.get(i)) >= 0 &&
                        key.compareTo(node.keys.get(i + 1)) < 0) {
                    find = true;
                    n = (Node<K, T>) node.children.get(i + 1);
                    index = i + 1;
                }
            }
        }
        return new AbstractMap.SimpleEntry<Integer, Node<K, T>>(index, n);
    }

    /**
     * get the key's position
     *
     * @param node
     * @param key
     * @return return the key's position in this node, if not contains, return -1
     */
    public int getPosition(Node<K, T> node, K key) {
        for (int i = 0; i < node.keys.size(); i++) {
            if (key.compareTo(node.keys.get(i)) == 0) return i;
        }
        return -1;
    }

    /**
     * get the sibling of this node with same parent
     *
     * @param parent
     * @param node
     * @return the target sibling node of this node
     */
    public Node<K, T>[] getSibling(Node<K, T> parent, Node<K, T> node) {
        Node<K, T>[] siblings = new Node[2];
        int size = ((IndexNode<K, T>)parent).children.size();
        int i = ((IndexNode<K, T>) parent).children.indexOf(node);
        // this node has a left sibling
        if (i > 0) {
            Node<K, T> leftSibling = ((IndexNode<K, T>) parent).children.get(i - 1);
            siblings[0] = leftSibling;
        }
        // this node has a right sibling
        if (i < size - 1) {
            Node<K, T> rightSibling = ((IndexNode<K, T>) parent).children.get(i + 1);
            siblings[1] = rightSibling;
        }
        return siblings;
    }
}

