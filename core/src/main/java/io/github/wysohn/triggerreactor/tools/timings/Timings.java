package io.github.wysohn.triggerreactor.tools.timings;

import io.github.wysohn.triggerreactor.tools.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * A Benchmark system to measure runtime of various tasks throughout the Triggers deployed by users.
 *
 * This class is inspired by how java.util.logging.Logger works, where Logger.getLogger(String) either create new
 * Logger or get the existing Logger, and the '.'(dot) in the name indicating the hierarchy of the Loggers.
 * For example, Timings.getTiming("my.timing.name") would yield three level of hierarchy: 'my', 'timing', and 'name',
 * and 'timing' would be the child of 'my' and 'name' would be child of 'timing.'
 */
public class Timings {
    private static final Timing root = new Timing(null);
    static{
        root.displayName = "Root";
    }

    public static boolean on = false;

    /**
     * Reset the root Timing to initial state.
     */
    public static void reset(){
        root.children.clear();
        root.executionTime = -1;
        root.count = 0;
    }

    /**
     * Get the timing with given name or create if not exist.
     * @param name the fully qualified name of the timing. null will yield root (root of all the timings)
     * @return the Timing
     */
    public static Timing getTiming(String name){
        if(name == null)
            return root;

        String[] split = name.split("\\.");
        Stack<String> path = new Stack<>();
        for(int i = split.length - 1; i >= 0; i--){
            path.push(split[i]);
        }
        return getTiming(root, path);
    }

    private static Timing getTiming(Timing root, Stack<String> path){
        if(path.isEmpty()){
            return root;
        } else {
            return getTiming(root.getOrCreate(root, path.pop()), path);
        }
    }

    public static void print(Timing timing, OutputStream stream) throws IOException {
        if(timing == null)
            return;

        stream.write(timing.toString().getBytes());
        for(Map.Entry<String, Timing> child : timing.children.entrySet()){
            stream.write('\n');
            print(child.getValue(), stream);
        }
    }

    public static class Timing implements AutoCloseable {
        private final Timing parent;
        private final int level;
        private final Map<String, Timing> children = new ConcurrentHashMap<>();
        private final Supplier<Long> currentTimeFn;

        private String displayName;

        private long executionTime;
        private long count;

        private Timing(Timing parent) {
            this(parent, System::currentTimeMillis);
        }

        /**
         * Maybe if we need some other measurement unit?
         * @param currentTimeFn the function to get current timestamp
         */
        private Timing(Timing parent, Supplier<Long> currentTimeFn) {
            this.parent = parent;
            this.level = parent == null ? 0 : parent.level + 1;
            this.currentTimeFn = currentTimeFn;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            if(displayName == null)
                return;

            this.displayName = displayName;
        }

        public boolean isLeafNode(){
            return children.size() == 0;
        }

        /**
         * This should be the direct child name. Do not use fully qualified name.
         * @param name the name of the direct child name.
         * @return Timing instance
         */
        private Timing getOrCreate(Timing parent, String name){
            if(name.contains("."))
                throw new RuntimeException("Cannot use .(dot) for the direct child's name!");

            if(children.containsKey(name)){
                return children.get(name);
            } else {
                Timing t = new Timing(parent);
                t.displayName = name;
                children.put(name, t);
                return t;
            }
        }

        private long begin;
        private boolean mainThread;

        /**
         * Record current system timestamp and return this Timing instance.
         * As Timing is AutoCloseable, you can use the try resources which is newly introduced in Java 8.
         * The execution time will be logged automatically by the close() method.
         * <p>
         *     try(Timing t = Timings.getTiming("my.timing")){<br>
         *     Thread.sleep(10L);<br>
         *     }
         * </p>
         *
         * @param mainThread mark it to indicate that the task ran by the main thread.
         * @return this instance
         */
        public Timing begin(boolean mainThread){
            begin = currentTimeFn.get();
            this.mainThread = mainThread;
            return this;
        }

        /**
         * Record current system timestamp and return this Timing instance.
         * As Timing is AutoCloseable, you can use the try resources which is newly introduced in Java 8.
         * The execution time will be logged automatically by the close() method.
         * <p>
         *     try(Timing t = Timings.getTiming("my.timing")){<br>
         *     Thread.sleep(10L);<br>
         *     }
         * </p>
         * @return this instance
         */
        public Timing begin(){
            if(parent == null)
                throw new RuntimeException("Can't begin() with root Timing.");

            return begin(false);
        }

        public double avg(){
            if(count == 0)
                return -1;

            return (double)executionTime / count;
        }

        private void add(long executionTime){
            if(!on)
                return;

            this.executionTime += executionTime;
            this.count++;

            if(parent != null){
                parent.add(executionTime);
            }
        }

        @Override
        public void close() {
            if(!on)
                return;

            add(currentTimeFn.get() - begin);
            begin = -1;
        }

        @Override
        public String toString() {
            String str = StringUtils.spaces(level * SPACES) + displayName;
            if(!isLeafNode())
                str += "["+children.size()+"]";
            str += " -- (avg: "+avg()+"ms, count: "+count+")";
            if(mainThread)
                str += "  !!MainThread";
            return str;
        }


    }

    private static final int SPACES = 4;
}
