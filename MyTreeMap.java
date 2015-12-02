package maps;

import java.util.*;

public class MyTreeMap<K, V> implements MyMap<K, V> {

    private static final boolean BLACK = true;
    private static final boolean RED = false;

    private static class SimpleEntry<K, V> implements Entry<K, V> {

        private boolean color;
        private K key;
        private SimpleEntry<K, V> left;
        private SimpleEntry<K, V> parent;
        private SimpleEntry<K, V> right;
        private V value;

        public SimpleEntry(K key, V value) {
            this.key = key;
            this.value = value;
            this.color = RED;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Entry
                    && this.key.equals(((Entry) o).getKey())
                    && this.value.equals(((Entry) o).getValue());
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }

        @Override
        public V setValue(V value) {
            V toReturn = this.value;
            this.value = value;
            return toReturn;
        }

        @Override
        public String toString() {
            return this.key.toString() + (this.color ? "(b)" : "(r)");
        }
    }

    private class EntryIterator implements Iterator<MyMap.Entry<K, V>> {

        private int expectedModCount = modCount;
        private SimpleEntry<K, V> nextEntry;

        private EntryIterator() {
            this.nextEntry = minimal(root);
        }

        @Override
        public boolean hasNext() {
            return nextEntry != null;
        }

        @Override
        public Entry<K, V> next() {
            if (this.nextEntry == null) {
                throw new NoSuchElementException();
            }
            if (expectedModCount != modCount) {
                throw new ConcurrentModificationException();
            }
            SimpleEntry<K, V> toReturn = nextEntry;
            nextEntry = successor(nextEntry);
            return toReturn;
        }
    }

    private Comparator<K> comparator;
    private int modCount;
    private SimpleEntry<K, V> root;
    private int size;

    public MyTreeMap() {
        this(null);
    }

    public MyTreeMap(Comparator<K> comparator) {
        this.comparator = comparator;
    }

    private static <K, V> boolean isRightChild(SimpleEntry<K, V> toCheck) {
        return toCheck.parent.right == toCheck;
    }

    private static <K, V> SimpleEntry<K, V> minimal(SimpleEntry<K, V> root) {
        SimpleEntry<K, V> curEntry = root;
        if (curEntry == null) {
            return null;
        }
        while (curEntry.left != null) {
            curEntry = curEntry.left;
        }
        return curEntry;
    }

    private static <K, V> SimpleEntry<K, V> sibling(SimpleEntry<K, V> entry) {
        if (entry == null || entry.parent == null) {
            return null;
        }
        if (isRightChild(entry)) {
            return entry.parent.left;
        }
        return entry.parent.right;
    }

    private static <K, V> SimpleEntry<K, V> successor(SimpleEntry<K, V> entry) {
        if (entry == null) {
            return null;
        }
        SimpleEntry<K, V> curEntry = entry.right;
        if (curEntry == null) {
            curEntry = entry.parent;
            while (curEntry != null && curEntry.right == entry) {
                entry = curEntry;
                curEntry = curEntry.parent;
            }
            return curEntry;
        }
        while (curEntry.left != null) {
            curEntry = curEntry.left;
        }
        return curEntry;
    }

    @Override
    public void clear() {
        root = null;
        size = 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return findEntry(key) != null;
    }

    @Override
    public boolean containsValue(V value) {
        Iterator<Entry<K, V>> iterator = new EntryIterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().equals(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator entryIterator() {
        return new EntryIterator();
    }

    @Override
    public Object get(Object key) {
        SimpleEntry entry = findEntry(key);
        return entry == null ? null : entry.value;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public V put(K key, V value) {
        SimpleEntry<K, V> curEntry = findInsertPoint(key);
        if (curEntry == null) {
            root = new SimpleEntry<>(key, value);
            root.color = BLACK;
            size++;
            modCount++;
            return null;
        }
        if (curEntry.key.equals(key)) {
            V toReturn = curEntry.value;
            curEntry.value = value;
            return toReturn;
        }
        SimpleEntry<K, V> toAdd = new SimpleEntry<>(key, value);
        toAdd.parent = curEntry;
        if (compare(curEntry.key, key) > 0) {
            curEntry.left = toAdd;
        } else {
            curEntry.right = toAdd;
        }
        size++;
        modCount++;
        fixAfterInsert(toAdd);
        return null;
    }

    @Override
    public V remove(K key) {
        if (key == null || !containsKey(key)) {
            return null;
        }
        SimpleEntry<K, V> toRemove = findDelPoint(root, key);
        V toReturn = toRemove.value;
        size--;
        modCount++;

        // toRemove has one child
        if (toRemove.left == null ^ toRemove.right == null) {
            SimpleEntry<K, V> subst = toRemove.left == null
                    ? toRemove.right : toRemove.left;
            subst.parent = toRemove.parent;
            subst.color = toRemove.color;
            if (toRemove == root) {
                root = subst;
            } else {
                if (isRightChild(toRemove)) {
                    toRemove.parent.right = subst;
                } else {
                    toRemove.parent.left = subst;
                }
            }
        } else {
            if (toRemove.color == BLACK) {
                pullDown(toRemove, key);
            }
            // toRemove has no children
            if (toRemove.left == null && toRemove.right == null) {
                removeLeaf(toRemove);
            }
            // toRemove has two children
            else {
                SimpleEntry<K, V> subst = findDelPoint(toRemove.left, key);
                removeLeaf(subst);
                toRemove.key = subst.key;
                toRemove.value = subst.value;
            }
        }
        if (root != null) {
            root.color = BLACK;
        }
        return toReturn;
    }

    @Override
    public int size() {
        return size;
    }

    public String toString() {
        if (this.root == null) return "";
        StringBuilder toReturn = new StringBuilder();
        Queue<SimpleEntry> q = new LinkedList<>();
        q.add(root);
        int maxLevel = getMaxLevel(root, 0);
        int maxKeySize = getLongestToString(root, 0) + 1;
        for (int i = 0; i <= maxLevel; i++) {
            int edgeOffset = (int) ((Math.pow(2, maxLevel - i - 1) - 1) * (maxKeySize + 1) + Math.ceil(maxKeySize / 2.0));
            toReturn.append(getSpace(' ', edgeOffset));
            int levelLength = (int) Math.pow(2, i);
            for (int j = 0; j < levelLength; j++) {
                SimpleEntry curEntry = q.poll();
                if (i != 0 && j % 2 == 0) toReturn.append('[');
                toReturn.append(curEntry == null ? getString("--", maxKeySize - 1) : getString(curEntry.toString(), maxKeySize - 1));
                if (i != 0 && j % 2 != 0) toReturn.append(']');
                if (j != levelLength - 1) {
                    int nodeOffset = (int) (Math.pow(2, maxLevel - i) * (maxKeySize + 1) - maxKeySize);
                    toReturn.append(j % 2 != 0 ? getSpace(' ', nodeOffset) : getSpace('_', nodeOffset));
                }
                q.add(curEntry == null || curEntry.left == null ? null : curEntry.left);
                q.add(curEntry == null || curEntry.right == null ? null : curEntry.right);
            }
            toReturn.append("\n");
        }
        toReturn.deleteCharAt(toReturn.length() - 1);
        return toReturn.toString();
    }

    private void changeColor(SimpleEntry entry) {
        entry.color = !entry.color;
    }

    private int compare(K o1, K o2) {
        return this.comparator == null
                ? ((Comparable) o1).compareTo(o2) : this.comparator.compare(o1, o2);
    }

    private SimpleEntry<K, V> findDelPoint(SimpleEntry<K, V> curEntry, K key) {
        while (!curEntry.key.equals(key)) {
//            System.out.println(curEntry.value);
            if (curEntry.color == BLACK) {
                pullDown(curEntry, key);
            }
            if (compare(curEntry.key, key) > 0) {
                if (curEntry.left == null) {
                    break;
                } else {
                    curEntry = curEntry.left;
                }
            } else {
                if (curEntry.right == null) {
                    break;
                } else {
                    curEntry = curEntry.right;
                }
            }
        }
        return curEntry;
    }

    private SimpleEntry findEntry(Object key) {
        if (key == null) {
            return null;
        }
        SimpleEntry curElement = root;
        while (curElement != null && !curElement.key.equals(key)) {
            if (((Comparable) curElement.key).compareTo(key) > 0) {
                curElement = curElement.left;
            } else {
                curElement = curElement.right;
            }
        }
        return curElement;
    }

    private SimpleEntry<K, V> findInsertPoint(K key) {
        SimpleEntry<K, V> curEntry = root;
        while (curEntry != null) {
            fixDuringInsert(curEntry);
            if (curEntry.key.equals(key)) {
                return curEntry;
            }
            if (compare(curEntry.key, key) > 0) {
                if (curEntry.left == null) {
                    return curEntry;
                } else {
                    curEntry = curEntry.left;
                }
            } else {
                if (curEntry.right == null) {
                    return curEntry;
                } else {
                    curEntry = curEntry.right;
                }
            }
        }
        return curEntry;
    }

    private void fixAfterInsert(SimpleEntry<K, V> toCheck) {
        if (isDoubleRed(toCheck)) {
            pullUp(toCheck);
        }
    }

    private void fixDuringInsert(SimpleEntry<K, V> toCheck) {
        if (hasRedChildren(toCheck)) {
            flipColor(toCheck);
        }
        if (toCheck != root && isDoubleRed(toCheck)) {
            pullUp(toCheck);
        }
    }

    private void flipColor(SimpleEntry<K, V> top) {
        if (top != root) {
            changeColor(top);
        }
        changeColor(top.left);
        changeColor(top.right);
    }

    private int getLongestToString(SimpleEntry<K, V> curEntry, int maxSize) {
        if (curEntry == null) return maxSize;
        maxSize = Math.max(maxSize, curEntry.toString().length());
        int maxSizeLeft, maxSizeRight;
        maxSizeLeft = getLongestToString(curEntry.left, maxSize);
        maxSizeRight = getLongestToString(curEntry.right, maxSize);
        return Math.max(maxSize, Math.max(maxSizeLeft, maxSizeRight));
    }

    private int getMaxLevel(SimpleEntry<K, V> curEntry, int level) {
        if (curEntry == null) return level - 1;
        int leftLevel, rightLevel;
        leftLevel = getMaxLevel(curEntry.left, level + 1);
        rightLevel = getMaxLevel(curEntry.right, level + 1);
        return Math.max(level, Math.max(leftLevel, rightLevel));
    }

    private String getSpace(char ch, int quant) {
        StringBuilder toReturn = new StringBuilder();
        while (quant-- > 0) {
            toReturn.append(ch);
        }
        return toReturn.toString();
    }

    private String getString(String data, int size) {
        StringBuilder toReturn = new StringBuilder();
        for (int i = 0; i < size - data.length(); i++) {
            toReturn.append(" ");
        }
        toReturn.append(data);
        return toReturn.toString();
    }

    private boolean hasBlackChildren(SimpleEntry<K, V> toCheck) {
        if (toCheck.left == null && toCheck.right == null) {
            return true;
        }
        if (toCheck.left == null || toCheck.right == null) {
            return false;
        }
        return toCheck.left.color == BLACK && toCheck.right.color == BLACK;
    }

    private boolean hasRedChildren(SimpleEntry<K, V> toCheck) {
        if (toCheck.left == null || toCheck.right == null) {
            return false;
        }
        return toCheck.left.color == RED && toCheck.right.color == RED;
    }

    private boolean isDoubleRed(SimpleEntry<K, V> toCheck) {
        return toCheck.color == RED && toCheck.parent.color == RED;
    }

    private void pullDown(SimpleEntry<K, V> toPull, K toDelete) {
        SimpleEntry<K, V> sibl;
        // Root has two black children
        if (toPull == root && hasBlackChildren(toPull)) {
            changeColor(toPull);
        }
        // At least one red child
        else if (!hasBlackChildren(toPull)) {
            // Next step -> to the left and left child is black
            if (compare(toPull.key, toDelete) >= 0 && toPull.left.color == BLACK) {
                changeColor(toPull);
                changeColor(toPull.right);
                roL(toPull);
                // Next step -> to the right and right child is black or null
            } else if (compare(toPull.key, toDelete) < 0 &&
                    (toPull.right == null || toPull.right.color == BLACK)) {
                changeColor(toPull);
                changeColor(toPull.left);
                roR(toPull);
            }
        }
        // Both siblings are black, their four children are black
        else if (hasBlackChildren(sibling(toPull))) {
            flipColor(toPull.parent);
        }
        // Both siblings are black, toPull's sibling has at least one red child
        // toPull's sibling is a right child
        else if (isRightChild(sibl = sibling(toPull))) {
            // toPull sibling's right child is outer red grandson
            if (sibl.right != null && sibl.right.color == RED) {
                flipColor(toPull.parent);
                // In case if parent was root
                toPull.parent.color = BLACK;
                changeColor(sibl.right);
                roL(toPull.parent);
            }
            // toPull sibling's left child is inner red grandson
            else {
                changeColor(toPull);
                changeColor(toPull.parent);
                roR(sibl);
                roL(toPull.parent);
            }
        }
        // toPull's sibling is a left child
        else {
            // toPull sibling's left child is outer red grandson
            if (sibl.left != null && sibl.left.color == RED) {
                flipColor(toPull.parent);
                // In case if parent was root
                toPull.parent.color = BLACK;
                changeColor(sibl.left);
                roR(toPull.parent);
            }
            // toPull sibling's right child is inner grandson
            else {
                changeColor(toPull);
                changeColor(toPull.parent);
                roL(sibl);
                roR(toPull.parent);
            }
        }
    }

    private void pullUp(SimpleEntry<K, V> toPull) {
        if (isRightChild(toPull) ^ isRightChild(toPull.parent)) {
            // Inner grandson
            changeColor(toPull.parent.parent);
            changeColor(toPull);
            for (int i = 0; i < 2; i++) {
                if (isRightChild(toPull)) {
                    roL(toPull.parent);
                } else {
                    roR(toPull.parent);
                }
            }
        } else {
            // Outer grandson
            changeColor(toPull.parent.parent);
            changeColor(toPull.parent);
            if (isRightChild(toPull)) {
                roL(toPull.parent.parent);
            } else {
                roR(toPull.parent.parent);
            }
        }
    }

    private void removeLeaf(SimpleEntry<K, V> toRemove) {
        if (toRemove == root) {
            root = null;
        } else if (isRightChild(toRemove)) {
            toRemove.parent.right = null;
        } else {
            toRemove.parent.left = null;
        }
    }

    private void roL(SimpleEntry<K, V> top) {
        if (top == null) {
            return;
        }
        if (top.right == null) {
            throw new IllegalArgumentException("Unable to perform rotation left");
        }
        top.right.parent = top.parent;
        if (top == root) {
            root = top.right;
        } else {
            if (isRightChild(top)) {
                top.parent.right = top.right;
            } else {
                top.parent.left = top.right;
            }
        }
        SimpleEntry<K, V> rightInnerG = top.right.left;
        top.right.left = top;
        if (rightInnerG != null) {
            rightInnerG.parent = top;
        }
        top.parent = top.right;
        top.right = rightInnerG;
    }

    private void roR(SimpleEntry<K, V> top) {
        if (top == null) {
            return;
        }
        if (top.left == null) {
            throw new IllegalArgumentException("Unable to perform rotation right");
        }
        top.left.parent = top.parent;
        if (top == root) {
            root = top.left;
        } else {
            if (isRightChild(top)) {
                top.parent.right = top.left;
            } else {
                top.parent.left = top.left;
            }
        }
        SimpleEntry<K, V> leftInnerG = top.left.right;
        top.left.right = top;
        if (leftInnerG != null) {
            leftInnerG.parent = top;
        }
        top.parent = top.left;
        top.left = leftInnerG;
    }

    private void showBlackHeights(SimpleEntry<K, V> top, int curHeight) {
        if (top == null) {
            return;
        }
        if (top.color) {
            curHeight++;
        }
        if (top.left != null) {
            showBlackHeights(top.left, curHeight);
        }
        if (top.right != null) {
            showBlackHeights(top.right, curHeight);
        }
        if (top.left == null && top.right == null) {
            System.out.println(top.key + ": " + curHeight);
        }
    }

    public static void main(String[] args) throws Exception {
        MyTreeMap<Integer, Integer> treeMap = new MyTreeMap<>();
//        MyTreeMap treeMap = new MyTreeMap((o1, o2) -> (Integer)o2 - (Integer)o1);
        for (int i = 0; i < 20; i++) {
            Integer toAdd = (int) (Math.random() * 100);
            treeMap.put(toAdd, toAdd);
        }
        System.out.println("size = " + treeMap.size());
        System.out.println(treeMap);
        System.out.println("\nBlack heights:");
        treeMap.showBlackHeights(treeMap.root, 0);
    }
}