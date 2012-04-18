package org.wdssii.storage;

import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Robert Toomey
 *
 * This list is highly specialized linked list for synchronization. Basically it
 * allows adding to the end of it within one thread, while reading up-to a
 * previous size in another. The java classes do so much checking/etc. and are
 * very generalizes, so that this helps with speed.
 *
 * It doesn't allow changing items. It grows only. It can only be iterated
 * forward...
 *
 * It is used so that a builder thread can append it it, while an openGL thread
 * can draw from it at the same time.
 *
 * Note that the objects stored inside the list are not synchronized
 *
 */
public class GrowList<E> {

        private static Logger log = LoggerFactory.getLogger(GrowList.class);

	/** Create a node...single linked list.  We could save a bit of
	 * memory by 'blocking' items into chunks...might do that later
	 * @param <E2> 
	 */
	private static class node<E2> {

		node(E2 item) {
			item0 = item;
		}
		E2 item0;
		node next;
	}

	/**
	 * Note the iterator itself is not thread-safe. You should create one
	 * everytime you want to read from the list.  In other words, don't
	 * use the same iterator object in two different threads.
	 */
	public class Itr implements Iterator<E> {

		private int sizeAtCreation;
		private int cursor = -1;
		private node<E> at;

		public Itr() {

// Note the only time we need to synchronize is at
// creation.  The root might change or the last node might
// add a next pointer, but we don't care.  We keep track of
// how many we have gone through....

			// get size first, since root is made non-null
			// BEFORE size increase...
			sizeAtCreation = getSize();
			// 2nd call not synched with above, but that's ok
			at = root;
		}

		@Override
		public boolean hasNext() {
			return ((cursor + 1) < sizeAtCreation);
		}

		@Override
		public E next() {
			boolean has = hasNext();
			E item = null;
			if (at != null) {
				at = at.next;
			}else{
				if (has){
					log.debug("BLEH NULL...supposed to have");
				}
			}
			if (at != null) {
			    item = at.item0;
				if ((item == null) && has){
					log.debug("BLEH ITEM NULL...supposed to have");
				}
			}
			cursor++;
			if (item == null){
				log.debug("ITEM WAS NULL???"+cursor+", "+sizeAtCreation);
			}
			return item;
		}

		public E peek(int i) {
			E item = null;
			node<E> pat = at;
			for (int p = 0; p < i; i++) {
				if (pat != null) {
					pat = pat.next;
				}
			}
			if (pat != null) {
				item = pat.item0;
			}
			return item;
		}

		@Override
		public void remove() {
			// not allowed
		}
	}
	private node<E> root;
	private node<E> last;
	private int size = 0;

	public Iterator<E> iterator() {
		return new Itr();
	}

	/**
	 * Add to the list
	 */
	public boolean add(E e) {
		node n = new node<E>(e);

		// Change root before size, since iterator might
		// be in different thread....worst case you have the
		// new root but a smaller size...
		if (root == null) { // first node....
			root = n;
			last = n;
		} else {
			// Add to end of list....note done before size
			last.next = n;
		}
		setSize(size + 1);
		return true;
	}

	// You don't want the size, you want to use the iterator
	// and next...the size is changing in another thread..
	private synchronized int getSize() {
		return size;
	}

	private synchronized void setSize(int s) {
		size = s;
	}
}
