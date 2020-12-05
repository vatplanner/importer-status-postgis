package org.vatplanner.importer.postgis.status;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.vatplanner.importer.postgis.status.entities.DirtyMark;

public class DirtyEntityTracker {

    private final Map<Class<? extends DirtyMark>, Set<DirtyMark>> dirtyEntitiesByClass = new HashMap<>();

    private <T extends DirtyMark> Set<T> access(Class<T> entityClass) {
        return (Set<T>) dirtyEntitiesByClass.computeIfAbsent(entityClass, c -> new HashSet<>());
    }

    public <T extends DirtyMark> void recordAsDirty(Class<T> entityClass, T entity) {
        access(entityClass).add(entity);
    }

    public <T extends DirtyMark> void recordAsClean(Class<T> entityClass, T entity) {
        access(entityClass).remove(entity);
    }

    public <T extends DirtyMark> Set<T> getDirtyEntities(Class<T> entityClass) {
        return Collections.unmodifiableSet(new HashSet<>(access(entityClass)));
    }

    public <T extends DirtyMark> boolean isDirty(Class<T> entityClass, T entity) {
        return access(entityClass).contains(entity);
    }

    public int countDirtyEntities() {
        return dirtyEntitiesByClass
            .values()
            .stream()
            .mapToInt(Set::size)
            .sum();
    }
}
