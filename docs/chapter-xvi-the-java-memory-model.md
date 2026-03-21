### Chapter XVI: The Java Memory Model

##### Platform Memory Model

In a shared-memory multiprocessor architecture,
each processor has its own cache that is 
periodically synchronized with the main memory.


The JMM (Java Memory Model) defines the rules for how threads interact through memory,
Defines a partial ordering called "happens-before" that guarantees visibility and ordering of memory operations across threads.
Example: If Thread A writes to a variable and then Thread B reads that variable, the JMM ensures that if Thread A's write happens-before Thread B's read, then Thread B will see the updated value.
If there is no happens-before relationship, then the behavior is undefined, and Thread B may see a stale value or even a completely different value.

![happens-before visibility](happens-before-visibility.png)


