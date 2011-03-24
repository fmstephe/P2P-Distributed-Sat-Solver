/*******************************************************************************
* SAT4J: a SATisfiability library for Java Copyright (C) 2004-2008 Daniel Le Berre
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Alternatively, the contents of this file may be used under the terms of
* either the GNU Lesser General Public License Version 2.1 or later (the
* "LGPL"), in which case the provisions of the LGPL are applicable instead
* of those above. If you wish to allow use of your version of this file only
* under the terms of the LGPL, and not to allow others to use your version of
* this file under the terms of the EPL, indicate your decision by deleting
* the provisions above and replace them with the notice and other provisions
* required by the LGPL. If you do not delete the provisions above, a recipient
* may use your version of this file under the terms of the EPL or the LGPL.
* 
* Based on the original MiniSat specification from:
* 
* An extensible SAT solver. Niklas Een and Niklas Sorensson. Proceedings of the
* Sixth International Conference on Theory and Applications of Satisfiability
* Testing, LNCS 2919, pp 502-518, 2003.
*
* See www.minisat.se for the original solver in C++.
* 
*******************************************************************************/
package org.francis.sat.solver;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Heap implementation used to maintain the variables order in some heuristics.
 * This heap implementation was taken directly from the JSat source code.
 * 
 * The IntVec dependency has been removed and replaced with direct array accesses.
 * 
 * @author Daniel Le Berre
 * @author Francis
 */
public class PriorityIntHeap implements Serializable {

    private static final long serialVersionUID = -7364477363610461156L;

    private final static int left(int i) {
        return i << 1;
    }

    private final static int right(int i) {
        return (i << 1) + 1;
    }

    private final static int parent(int i) {
        return i >> 1;
    }

    private int size;
    private final int[] heap; // heap of ints
    private final int[] indices; // Index for each value in the heap array
    private final double[] priorities; // The priority of each int in the heap
    
    public PriorityIntHeap(int size) {
        heap = new int[size+1];
        indices = new int[size+1];
        priorities = new double[size+1];
        size = 0;
    }
    
    private double compare(int a, int b) {
        return priorities[a] - priorities[b];
    }

    private void percolateUp(final int index) {
        int hole = index;
        final int x = heap[hole];
        while (parent(hole) != 0 && compare(x,heap[parent(hole)]) > 0) {
            heap[hole] = heap[parent(hole)];
            indices[heap[hole]] = hole;
            hole = parent(hole);
        }
        heap[hole] = x;
        indices[x] = hole;
    }

    private void percolateDown(final int index) {
        int hole = index;
        final int x = heap[hole];
        int leftChild = left(hole);
        int rightChild = right(hole);
        while (leftChild <= size) {
            int child = rightChild <= size
                    && compare(heap[rightChild],heap[leftChild]) > 0 ? rightChild
                    : leftChild;
            if (compare(heap[child],x) <= 0)
                break;
            heap[hole] = heap[child];
            indices[heap[hole]] = hole;
            hole = child;
            leftChild = left(hole);
            rightChild = right(hole);
        }
        heap[hole] = x;
        indices[x] = hole;
    }

    public boolean empty() {
        return size == 0;
    }

    public void insert(int var) {
        assert consistent();
        size++;
        heap[size] = var;
        percolateUp(size);
        assert consistent();
    }

    public int pop() {
        assert consistent();
        ensureNotEmpty();
        int var = heap[1];
        indices[var] = 0;
        // NB: Since this heap array starts at 1, size points to the last element
        heap[1] = heap[size];
        size--;
        if (size > 0) percolateDown(1);
        assert consistent();
        return var;
    }
    
    public int peek() {
        assert consistent();
        ensureNotEmpty();
        return heap[1];
    }
    
    public void delete(int var) {
        assert indices[var] != 0; // This is unchecked without assertions - don't try to delete shit which ain't there, caller
        assert consistent();
        int i = indices[var];
        indices[var] = 0;
        heap[i] = heap[size];
        heap[size] = 0;
        size--;
        if (size > 0) {
            percolateUp(i);
            percolateDown(i);
        }
        assert indices[var] == 0;
        assert consistent();
    }
    
    public int size() {
        return size;
    }

    public boolean contains(int var) {
        return indices[var] != 0;
    }
    
    public double getPriority(int var) {
        return priorities[var];
    }
    
    public void incPriority(int var, double incVal) {
        priorities[var] += incVal;
    }
    
    public void setPriority(int var, double priority) {
        priorities[var] = priority;
    }
    
    private void ensureNotEmpty() {
        if (size < 1) throw new IllegalStateException("Can't access elements of an empty heap");
    }
    
    private boolean goodIndex(int index) {
        boolean leftMatch = left(index) > size ? true : compare(heap[index],heap[left(index)]) >= 0;
        boolean rightMatch = right(index) > size ? true : compare(heap[index],heap[right(index)]) >= 0;
        boolean indiceMatch = indices[heap[index]] == index;
        boolean result = leftMatch & rightMatch & indiceMatch;
        return result;
    }
    
    private boolean consistent() {
        boolean consistent = true;
        for (int i = 1; i <= size; i++) {
            consistent &= goodIndex(i);
        }
        return consistent;
    }
}
