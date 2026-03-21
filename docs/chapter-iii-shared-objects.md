### Chapter 3- Shared Objects

#### Notes

- Locking is not just about mutual exclusionl it is also about memory visibility.
To ensure that all threads see the most up-to-date values of shared mutable varibles, the reading and
writing threads must synchronize on a common lock.


