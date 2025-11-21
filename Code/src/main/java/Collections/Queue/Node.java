package Collections.Queue;

class Node<T> {

    private Node <T> next;
    private T element;

    public Node(){
        this.next = null;
        this.element = null;
    }

    public Node(T elem){
        this.next = null;
        this.element = elem;
    }

    /**
     * Obter o próximo elemento do nó do atual
     *
     * @return
     */
    public Node<T> getNext(){
        return this.next;
    }

    /**
     * Definir o próximo nó do atual
     *
     * @param node
     */
    public void setNext(Node<T> node){
        this.next = node;
    }

    /**
     * Obter o elemento do nó
     *
     * @return
     */
    public T getElement(){
        return element;
    }

    /**
     * Definir o elemento do nó
     *
     * @param element
     */
    public void setElement(T element){
        this.element = element;
    }
}