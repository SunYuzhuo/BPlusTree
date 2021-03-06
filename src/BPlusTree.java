/**
 * Author: Yuzhuo Sun, Bangrui Chen, Jingyao Ren
 * Date: Oct 7, 2015
 */

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.List;


/**
 * BPlusTree Class Assumptions: 1. No duplicate keys inserted 2. Order D:
 * D<=number of keys in a node <=2*D 3. All keys are non-negative
 * TODO: Rename to BPlusTree
 */
public class BPlusTree<K extends Comparable<K>, T> {
    
    public Node<K,T> root;
    public static final int D = 2;
    
    /**
     * Search the value for a specific key
     *
     * @param key
     * @return value
     */
    public T search(K key) {
        return searchNode(root, key);
    }
    
    /**
     * Overwritten search for a specific key
     *
     * @param current temp root
     * @param key
     * @return value
     */
    private T searchNode(Node<K,T> tempRoot, K key) {
        if (tempRoot.isLeafNode) { // Search in leaf nodes
            int i = 0;
            LeafNode<K, T> ln = (LeafNode<K, T>) tempRoot;
            for (i = 0; i < ln.keys.size(); i++ ) {
                if (key.compareTo(ln.keys.get(i)) == 0) {
                    return ln.values.get(i);
                }
            }
            return null;
        } else { // Search in index nodes
            int i = 0;
            IndexNode<K, T> in = (IndexNode<K, T>)tempRoot;
            for (i = 0; i < in.keys.size(); i++) {
                if (key.compareTo(in.keys.get(i)) < 0) {
                    break;
                }
            }
            return searchNode(in.children.get(i),key);
        }
    }
    
    
    /**
     *  Insert a key/value pair into the BPlusTree
     *
     * @param key
     * @param value
     */
    public void insert(K key, T value) {
        if (root == null) {
            LeafNode<K,T> ln = new LeafNode<K,T>(key, value);
            root = ln;
            return;
        }
        Node<K,T> tempNode = root;
        ArrayList<IndexNode<K,T>> parentsNode = new ArrayList<IndexNode<K,T>>();
        while(!tempNode.isLeafNode) { // Traverse until encounter a leaf node
            parentsNode.add((IndexNode<K,T>)tempNode);
            IndexNode<K,T> in = (IndexNode<K,T>) tempNode;
            int i;
            for (i = 0; i < in.keys.size(); i++) {
                if (key.compareTo(in.keys.get(i)) < 0) {
                    break;
                }
            }
            tempNode = in.children.get(i);
        }
        assert (tempNode.isLeafNode);
        if(tempNode.isLeafNode) { // Traverse in the targeted leaf node to insert new node
            LeafNode<K, T> ln = (LeafNode<K,T>) tempNode;
            int i = 0;
            for (i = 0; i < ln.keys.size(); i++) {
                if (key.compareTo(ln.keys.get(i)) < 0) {
                    break;
                }
            }
            ln.keys.add(i, key);
            ln.values.add(i, value);
        }
        if (tempNode.isOverflowed()) { // Check overflow and handle overflow condition
            int j = parentsNode.size() - 1;
            Entry<K, Node<K,T>> tempEntry = splitLeafNode((LeafNode<K,T>)tempNode);
            if (j > -1) {
                int i;
                for (i = 0; i < parentsNode.get(j).keys.size(); i++) {
                    if (tempEntry.getKey().compareTo(parentsNode.get(j).keys.get(i)) < 0) {
                        break;
                    }
                }
                parentsNode.get(j).insertSorted(tempEntry, i);
                while (parentsNode.get(j).isOverflowed()) {
                    tempEntry = splitIndexNode((parentsNode.get(j)));
                    j = j - 1;
                    if (j == -1) {
                        break;
                    }
                    for (i = 0; i < parentsNode.get(j).keys.size(); i++) {
                        if (tempEntry.getKey().compareTo(parentsNode.get(j).keys.get(i)) < 0) {
                            break;
                        }
                    }
                    parentsNode.get(j).insertSorted(tempEntry, i);
                }
                if (j == -1) {
                    root = new IndexNode<K,T>(tempEntry.getKey(), parentsNode.get(0), tempEntry.getValue());
                }
            } else {
                if (j == -1) {
                    root = new IndexNode<K,T>(tempEntry.getKey(), ((LeafNode<K,T>)tempEntry.getValue()).previousLeaf, tempEntry.getValue());
                }
            }
        }
        
    }
    
    /**
     * Split a leaf node and return the new right node and the splitting
     * key as an Entry<slitingKey, RightNode>
     *
     * @param leaf, any other relevant data
     * @return the key/node pair as an Entry
     */
    public Entry<K, Node<K,T>> splitLeafNode(LeafNode<K,T> leaf) {
        assert(leaf.keys.size() == 2 * D + 1);
        List<T> tempValues = new ArrayList<T>(leaf.values);
        List<K> tempKeys = new ArrayList<K>(leaf.keys);
        for (int i = 0; i < BPlusTree.D; i++) {
            tempValues.remove(0);
            tempKeys.remove(0);
        }
        K splitkey = tempKeys.get(0);
        for (int i = 0; i <= BPlusTree.D; i++) {
            leaf.values.remove(BPlusTree.D);
            leaf.keys.remove(BPlusTree.D);
        }
        LeafNode<K,T> newLn = new LeafNode<K,T>((List<K>)tempKeys, (List<T>)tempValues);
        newLn.isLeafNode = true;
        newLn.nextLeaf = leaf.nextLeaf;
        leaf.nextLeaf = newLn;
        newLn.previousLeaf = leaf;
        Entry<K, Node<K,T>> newEntry = new AbstractMap.SimpleEntry<K, Node<K,T>>((K)splitkey, newLn);
        return newEntry;
    }
    
    
    /**
     * TODO split an indexNode and return the new right node and the splitting
     * key as an Entry<slitingKey, RightNode>
     *
     * @param index, any other relevant data
     * @return new key/node pair as an Entry
     */
    public Entry<K, Node<K,T>> splitIndexNode(IndexNode<K,T> index) {
        assert (index.keys.size() == 2 * D + 1);
        List<K> tempKeys = new ArrayList<K>(index.keys);
        List<Node<K,T>> tempChildren = new ArrayList<Node<K,T>>(index.children);
        for (int i = 0; i < BPlusTree.D; i++) {
            tempKeys.remove(0);
            tempChildren.remove(0);
        }
        K splitkey = (K)tempKeys.get(0);
        tempKeys.remove(0);
        tempChildren.remove(0);
        int size = index.keys.size();
        for (int i = D; i < size; i++) {
            index.keys.remove(D);
            index.children.remove(D + 1);
        }
        assert (tempKeys.size() + 1 == tempChildren.size());
        assert (index.keys.size() + 1 == index.children.size());
        IndexNode<K, T> in = new IndexNode<K, T> (tempKeys, tempChildren);
        in.isLeafNode = false;
        Entry<K, Node<K,T>> tempEntry = new AbstractMap.SimpleEntry<K, Node<K, T>>(splitkey, in);
        return tempEntry;
    }

    /**
     * Delete a key/value pair from this B+Tree
     *
     * @param key
     */
    public void delete(K key) {
        delete(null, root, key, -1);
    }
    
    /**
     * Overwritten method delete a key/value pair from this B+Tree
     *
     * @param parent node
     * @param current traversing node 
     * @param key
     * @param current node's index in parent
     */
    private void delete(IndexNode <K, T> parent, Node<K, T> current, K key, int indexInParent){
        // LeafNode case
        if(!current.isLeafNode){
            IndexNode<K, T> indexNode = (IndexNode<K, T>) current;
            // Choose subtree
            for(int i = 0; i < indexNode.keys.size(); i++){
                if(indexNode.keys.get(i).compareTo(key) > 0){
                    delete(indexNode, indexNode.children.get(i), key, i);
                    break;
                }else if(i == indexNode.keys.size() - 1 && indexNode.keys.get(indexNode.keys.size() - 1).compareTo(key) <= 0){
                    delete(indexNode, indexNode.children.get(indexNode.children.size() - 1), key, i + 1);
                    break;
                }
            }
            // Handle leafNode underflow case
            if(indexNode.isUnderflowed() && parent != null){
                //int splitPos;
                if(indexInParent > 0){
                    handleIndexNodeUnderflow((IndexNode<K, T>)parent.children.get(indexInParent - 1), indexNode, parent);
                }else{
                    handleIndexNodeUnderflow(indexNode, (IndexNode<K, T>)parent.children.get(indexInParent + 1), parent);
                }
            }else if(indexNode.isUnderflowed() && parent == null && indexNode.children.size() == 1){
                root = indexNode.children.get(0);
            }
        }else{  // LeafNode case
            LeafNode<K, T> leafNode = (LeafNode<K, T>) current;
            // Locate position to delete leafNode
            for(int i = 0; i < leafNode.keys.size(); i++){
                if(leafNode.keys.get(i).compareTo(key) == 0){
                    leafNode.keys.remove(i);
                    leafNode.values.remove(i);
                    break;
                }
            }
            // Handle leafNode underflow case;
            if(leafNode.isUnderflowed() && parent != null){
                //int splitPos = -1;
                if(indexInParent > 0){
                    handleLeafNodeUnderflow(leafNode.previousLeaf, leafNode, parent);
                }else{
                    handleLeafNodeUnderflow(leafNode,leafNode.nextLeaf, parent);
                }
            }
        }
    }
    
    /**
     * Handle LeafNode Underflow (merge or redistribution)
     *
     * @param left
     *            : the smaller node
     * @param right
     *            : the bigger node
     * @param parent
     *            : their parent index node
     * @return the splitkey position in parent if merged so that parent can
     *         delete the splitkey later on. -1 otherwise
     */
    public int handleLeafNodeUnderflow(LeafNode<K,T> left, LeafNode<K,T> right,
                                       IndexNode<K,T> parent) {
        // Merge if left node has enough space to merge, redistribute otherwise
        if(left.keys.size() + right.keys.size() < 2 * D){
            left.keys.addAll(right.keys);
            left.values.addAll(right.values);
            parent.children.remove(right);
            parent.keys.remove(parent.children.indexOf(left));
            return parent.children.indexOf(left);
        }else{// redistribution
            if(left.isUnderflowed()){
                left.insertSorted(right.keys.remove(0), right.values.remove(0));
            }else{
                // Iteratively move the left node's children until there is n entries in the left and n + 1 in the right
                while(left.keys.size() > D){
                    right.insertSorted(left.keys.remove(left.keys.size() - 1), left.values.remove(left.values.size() - 1));
                }
                parent.keys.set(parent.children.indexOf(left), right.keys.get(0));
            }
        }
        return -1;
    }
    
    /**
     * TODO Handle IndexNode Underflow (merge or redistribution)
     *
     * @param left
     *            : the smaller node
     * @param right
     *            : the bigger node
     * @param parent
     *            : their parent index node
     * @return the splitkey position in parent if merged so that parent can
     *         delete the splitkey later on. -1 otherwise
     */
    public int handleIndexNodeUnderflow(IndexNode<K,T> leftIndex,
                                        IndexNode<K,T> rightIndex, IndexNode<K,T> parent) {
        
        int parentIndex = -1;
        
        if(parent != null){// TODO Change to parentindex
            parentIndex = parent.children.indexOf(leftIndex);
        }
        
        // Merge operation
        if(leftIndex.keys.size() + rightIndex.keys.size() < 2 * D){
            leftIndex.keys.add(parent.keys.get(parentIndex));
            leftIndex.keys.addAll(rightIndex.keys);
            leftIndex.children.addAll(rightIndex.children);
            parent.children.remove(rightIndex);
            return parentIndex;
        }else{
            // Redistribute
            if(leftIndex.isUnderflowed()){
                leftIndex.keys.add(parent.keys.get(parentIndex));
                parent.keys.set(parentIndex, rightIndex.keys.remove(0));
                leftIndex.children.add(rightIndex.children.remove(0));
            }else{
                // Iteratively move the left node's children until there is n entries in the left and n + 1 in the right
                while(leftIndex.keys.size() > D){
                    rightIndex.keys.add(0, parent.keys.get(parentIndex));
                    rightIndex.children.add(0, leftIndex.children.remove(leftIndex.children.size() - 1));
                    parent.keys.set(parent.keys.size() - 1, leftIndex.keys.remove(leftIndex.keys.size() - 1));	
                }
            }
        }
        
        return -1;
    }
    
}
