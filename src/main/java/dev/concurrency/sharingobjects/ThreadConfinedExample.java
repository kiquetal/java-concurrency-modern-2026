package dev.concurrency.sharingobjects;

import java.util.ArrayList;
import java.util.List;

public class ThreadConfinedExample {

    public void processItems() {
        // This list is strictly thread-confined. It is created within the method,
        // modified locally, and its reference never escapes the method's scope.
        // Therefore, it does not require any synchronization.
        List<String> confinedList = new ArrayList<>();
        
        confinedList.add("Item 1");
        confinedList.add("Item 2");

        for (String item : confinedList) {
            System.out.println(Thread.currentThread().getName() + " processing " + item);
        }
    }

    public static void main(String[] args) {
        ThreadConfinedExample example = new ThreadConfinedExample();

        // Multiple threads executing the same method safely,
        // because the state is confined to each thread's stack.
        Runnable task = example::processItems;
        new Thread(task, "Thread-1").start();
        new Thread(task, "Thread-2").start();
    }
}
