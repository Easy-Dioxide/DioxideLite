package com.dioxidelite.event;

import com.dioxidelite.DioxideLite;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * A small reflection-based event bus.
 * <p>
 * Objects register handler methods annotated with {@link Listen}; events are
 * delivered to every handler registered for the event's exact runtime type,
 * ordered by descending {@link Priority}. Handler lists are copy-on-write so a
 * handler may (un)subscribe another object while an event is being dispatched.
 */
public final class EventBus {

    public static final EventBus INSTANCE = new EventBus();

    /** Handlers keyed by the event type they listen for. */
    private final Map<Class<?>, CopyOnWriteArrayList<Handler>> handlers = new ConcurrentHashMap<>();

    /** Cache of scanned {@code @Listen} methods per subscriber class. */
    private final Map<Class<?>, List<Method>> methodCache = new ConcurrentHashMap<>();

    private EventBus() {
    }

    /**
     * Registers every {@link Listen} method declared on {@code subscriber} (and
     * its superclasses). Calling this twice for the same instance duplicates its
     * handlers, so pair each call with {@link #unsubscribe(Object)}.
     */
    public void subscribe(Object subscriber) {
        for (Method method : scan(subscriber.getClass())) {
            Class<?> eventType = method.getParameterTypes()[0];
            Handler handler = new Handler(subscriber, method, method.getAnnotation(Listen.class).priority());
            CopyOnWriteArrayList<Handler> list = handlers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>());
            insertByPriority(list, handler);
        }
    }

    /** Removes all handlers belonging to {@code subscriber}. */
    public void unsubscribe(Object subscriber) {
        for (CopyOnWriteArrayList<Handler> list : handlers.values()) {
            list.removeIf(handler -> handler.target == subscriber);
        }
    }

    /**
     * Delivers {@code event} to all registered handlers and returns it, so calls
     * can be chained (e.g. {@code if (post(new FooEvent()).isCancelled())}).
     * A {@link CancellableEvent} stops propagating once cancelled.
     */
    public <T extends Event> T post(T event) {
        return postTo(event, ignored -> true);
    }

    /** Delivers an event only to subscribers accepted by {@code targetFilter}. */
    public <T extends Event> T postTo(T event, Predicate<Object> targetFilter) {
        CopyOnWriteArrayList<Handler> list = handlers.get(event.getClass());
        if (list == null) {
            return event;
        }

        boolean cancellable = event instanceof CancellableEvent;
        for (Handler handler : list) {
            if (!targetFilter.test(handler.target)) continue;
            handler.invoke(event);
            if (cancellable && ((CancellableEvent) event).isCancelled()) {
                break;
            }
        }
        return event;
    }

    private void insertByPriority(CopyOnWriteArrayList<Handler> list, Handler handler) {
        int index = 0;
        while (index < list.size() && list.get(index).priority >= handler.priority) {
            index++;
        }
        list.add(index, handler);
    }

    private List<Method> scan(Class<?> type) {
        return methodCache.computeIfAbsent(type, EventBus::collectListenMethods);
    }

    private static List<Method> collectListenMethods(Class<?> type) {
        List<Method> found = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(Listen.class)) {
                    continue;
                }
                if (Modifier.isStatic(method.getModifiers())) {
                    throw new IllegalArgumentException("@Listen method must not be static: " + method);
                }
                if (method.getParameterCount() != 1 || !Event.class.isAssignableFrom(method.getParameterTypes()[0])) {
                    throw new IllegalArgumentException("@Listen method must take a single Event parameter: " + method);
                }
                method.setAccessible(true);
                found.add(method);
            }
        }
        return List.copyOf(found);
    }

    /** A single bound listener method. */
    private static final class Handler {

        private final Object target;
        private final Method method;
        private final int priority;

        private Handler(Object target, Method method, int priority) {
            this.target = target;
            this.method = method;
            this.priority = priority;
        }

        private void invoke(Event event) {
            try {
                method.invoke(target, event);
            } catch (Throwable t) {
                // 这里的 catch 绝不能再抛出去 —— 一个模块的 handler 出错不该把整个游戏带崩。
                // 原实现用 event.getClass().getSimpleName()：对嵌套事件类（如 PlayerTickEvent$Pre）
                // 求简单名会去加载外层类 PlayerTickEvent，一旦类加载失败就会从 catch 里抛出
                // NoClassDefFoundError，把"某个 handler 报错"直接升级成 ReportedException 崩溃。
                // getName() 不触发类加载；日志本身也兜一层。
                try {
                    DioxideLite.LOGGER.error("Event handler {} threw while handling {}",
                            method, event.getClass().getName(), t);
                } catch (Throwable ignored) {
                    // 连日志都失败时也必须让事件分发继续下去
                }
            }
        }
    }
}
