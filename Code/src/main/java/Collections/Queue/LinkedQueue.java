package Collections.Queue;

import Collections.Exceptions.EmptyCollectionException;
import Collections.Queue.Node;

import java.security.InvalidParameterException;

/**
 *
 * @author Gabriel
 */
public class LinkedQueue<T> implements QueueADT<T> {

    private int count;
    private Node<T> front, rear;

    public LinkedQueue(){
        this.count = 0;
        this.front =  this.rear = null;
    }

    /**
     * Adicionar elemento a fila
     *
     * @param element
     */
    @Override
    public void enqueue(T element) {

        if (element == null) {
            throw new InvalidParameterException("Elemento não pode ser nulo.");
        }

        Node<T> newNode = new Node<T>(element);

        if (isEmpty()) {
            front = newNode;
        } else {
            rear.setNext(newNode);
        }

        rear = newNode;
        count++;
    }

    /**
     * Remover elemento da fila
     *
     * @return
     * @throws EmptyCollectionException
     */
    @Override
    public T dequeue() throws EmptyCollectionException {
        if (isEmpty()) {
            throw new EmptyCollectionException("Lista está vazia");
        }

        T result = front.getElement();
        front = front.getNext();
        count--;

        if (isEmpty()) {
            rear = null;
        }

        return result;

    }

    /**
     * Obter o primeiro elemento da fila
     *
     * @return
     */
    @Override
    public T first() {
        return this.front.getElement();
    }

    /**
     * Obter o último elemento da fila
     *
     * @return
     */
    @Override
    public boolean isEmpty() {
        return (front == null);
    }

    /**
     * Devolver o tamanho da fila
     *
     * @return
     */
    @Override
    public int size() {
        return this.count;
    }

    /**
     * Representação em string da fila
     *
     * @return
     */
    @Override
    public String toString() {

        Node<T> current = this.front;
        String result = "";

        while (current != null) {
            result += "Valor: " + current.getElement() + "\n";
            current = current.getNext();
        }
        return result;
    }
}